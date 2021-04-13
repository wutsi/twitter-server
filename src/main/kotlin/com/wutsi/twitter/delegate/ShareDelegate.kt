package com.wutsi.twitter.`delegate`

import com.wutsi.site.SiteApi
import com.wutsi.site.dto.Site
import com.wutsi.story.StoryApi
import com.wutsi.story.dto.Story
import com.wutsi.stream.EventStream
import com.wutsi.twitter.SiteAttribute
import com.wutsi.twitter.dao.SecretRepository
import com.wutsi.twitter.dao.ShareRepository
import com.wutsi.twitter.entity.SecretEntity
import com.wutsi.twitter.entity.ShareEntity
import com.wutsi.twitter.event.TwitterEventType
import com.wutsi.twitter.event.TwitterSharedEventPayload
import com.wutsi.twitter.service.bitly.BitlyUrlShortenerFactory
import com.wutsi.twitter.service.twitter.TwitterProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import twitter4j.Status
import twitter4j.StatusUpdate
import twitter4j.TwitterException
import java.net.HttpURLConnection
import java.net.URL
import javax.transaction.Transactional

@Service
public class ShareDelegate(
    @Autowired private val siteApi: SiteApi,
    @Autowired private val storyApi: StoryApi,
    @Autowired private val shareDao: ShareRepository,
    @Autowired private val secretDao: SecretRepository,
    @Autowired private val bitly: BitlyUrlShortenerFactory,
    @Autowired private val twitterProvider: TwitterProvider,
    @Autowired private val eventStream: EventStream
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(ShareDelegate::class.java)
    }

    @Transactional
    fun invoke(
        storyId: Long,
        message: String? = null,
        pictureUrl: String? = null,
        includeLink: Boolean = true,
        postId: Long? = null
    ) {
        val story = storyApi.get(storyId).story
        val site = siteApi.get(story.siteId).site
        if (!enabled(site)) {
            LOGGER.info("Site#${story.siteId} doesn't have Twitter enabled. Ignoring the request")
            return
        }
        if (alreadyPublished(storyId, postId)) {
            LOGGER.info("{Story#$storyId, Post#$postId} already published. Ignoring the request")
            return
        }

        val secret = findSecret(story, site) ?: return
        val status = share(story, secret, site, message, pictureUrl, includeLink, postId)
        if (status != null) {
            eventStream.publish(
                type = TwitterEventType.SHARED.urn,
                payload = TwitterSharedEventPayload(
                    twitterStatusId = status.id,
                    postId = postId
                )
            )
        }
    }

    private fun alreadyPublished(storyId: Long, postId: Long?): Boolean =
        shareDao.findByStoryIdAndPostId(storyId, postId).isPresent

    private fun findSecret(story: Story, site: Site): SecretEntity? {
        // Find account of the author of the story
        var opt = secretDao.findByUserIdAndSiteId(story.userId, site.id)
        if (opt.isPresent)
            return opt.get()

        // Author doesn't have twitter account, return the primary user
        val userId = userId(site) ?: return null
        opt = secretDao.findByUserIdAndSiteId(userId, site.id)
        return if (opt.isPresent)
            opt.get()
        else
            null
    }

    private fun share(
        story: Story,
        secret: SecretEntity,
        site: Site,
        message: String?,
        pictureUrl: String?,
        includeLink: Boolean,
        postId: Long?
    ): Status? {
        var status: Status? = null

        try {
            status = tweet(story, secret, site, message, pictureUrl, includeLink)
            if (status != null) {
                retweet(status, secret, site)
            }
        } catch (ex: Exception) {
            LOGGER.error("Unable to share the story", ex)
            save(story, site, secret, ex, postId)
        } finally {
            if (status != null) {
                save(story, site, secret, status, postId)
            }
        }

        return status
    }

    private fun tweet(
        story: Story,
        secret: SecretEntity,
        site: Site,
        message: String?,
        pictureUrl: String?,
        includeLink: Boolean
    ): Status? {
        val twitter = twitterProvider.getTwitter(secret.accessToken, secret.accessTokenSecret, site) ?: return null

        val text = text(story, site, message, includeLink)
        if (pictureUrl.isNullOrEmpty()) {
            LOGGER.info("Tweeting to ${secret.twitterId}: $text")
            return twitter.updateStatus(text)
        } else {
            val url = URL(pictureUrl)
            val cnn = url.openConnection() as HttpURLConnection
            try {
                LOGGER.info("Tweeting to ${secret.twitterId} the picture $pictureUrl: $text")
                val update = StatusUpdate(text)
                update.media(url.file, cnn.inputStream)
                return twitter.updateStatus(update)
            } finally {
                cnn.disconnect()
            }
        }
    }

    private fun retweet(status: Status, secret: SecretEntity, site: Site) {
        try {
            val userId = userId(site)
            if (userId == null || userId == secret.userId)
                return

            val primarySecret = secretDao.findByUserIdAndSiteId(userId, site.id)
            if (!primarySecret.isPresent)
                return

            val twitter = twitterProvider.getTwitter(primarySecret.get().accessToken, primarySecret.get().accessTokenSecret, site) ?: return
            LOGGER.info("Retweeting ${status.id}")
            twitter.retweetStatus(status.id)
        } catch (ex: Exception) {
            LOGGER.warn("Unable to retweet ${status.id}", ex)
        }
    }

    private fun save(
        story: Story,
        site: Site,
        secret: SecretEntity,
        ex: Exception,
        postId: Long?
    ) {
        val errorCode = if (ex is TwitterException) ex.errorCode else null
        val errorMessage = if (ex is TwitterException) ex.errorMessage else ex.message
        try {
            shareDao.save(
                ShareEntity(
                    storyId = story.id,
                    siteId = site.id,
                    secret = secret,
                    postId = postId,
                    statusId = null,
                    success = false,
                    errorCode = errorCode,
                    errorMessage = errorMessage
                )
            )
        } catch (ex: Exception) {
            LOGGER.warn("Unable to store the share information", ex)
        }
    }

    private fun save(
        story: Story,
        site: Site,
        secret: SecretEntity,
        status: Status,
        postId: Long?
    ) {
        try {
            shareDao.save(
                ShareEntity(
                    storyId = story.id,
                    siteId = site.id,
                    secret = secret,
                    statusId = status.id,
                    postId = postId,
                    success = true
                )
            )
        } catch (ex: Exception) {
            LOGGER.warn("Unable to store the share information", ex)
        }
    }

    private fun text(
        story: Story,
        site: Site,
        message: String?,
        includeLink: Boolean
    ): String {
        val text = if (!message.isNullOrEmpty())
            message
        else if (!story.socialMediaMessage.isNullOrEmpty())
            story.socialMediaMessage
        else
            story.title

        val url = if (includeLink)
            url(story, site)
        else
            ""

        return "$text $url".trim()
    }

    private fun url(story: Story, site: Site): String =
        bitly.get(site).shorten("${site.websiteUrl}${story.slug}?utm_source=twitter")

    private fun enabled(site: Site): Boolean =
        site.attributes.find { SiteAttribute.ENABLED.urn == it.urn }?.value == "true"

    private fun userId(site: Site): Long? {
        try {
            return site.attributes.find { SiteAttribute.USER_ID.urn == it.urn }?.value?.toLong()
        } catch (ex: Exception) {
            return null
        }
    }
}
