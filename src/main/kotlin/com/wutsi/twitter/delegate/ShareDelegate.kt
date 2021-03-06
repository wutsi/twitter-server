package com.wutsi.twitter.`delegate`

import com.wutsi.platform.site.SiteProvider
import com.wutsi.site.SiteAttribute
import com.wutsi.site.dto.Site
import com.wutsi.story.StoryApi
import com.wutsi.story.dto.Story
import com.wutsi.stream.EventStream
import com.wutsi.twitter.dao.SecretRepository
import com.wutsi.twitter.dao.ShareRepository
import com.wutsi.twitter.entity.SecretEntity
import com.wutsi.twitter.entity.ShareEntity
import com.wutsi.twitter.event.TwitterEventType
import com.wutsi.twitter.event.TwitterSharedEventPayload
import com.wutsi.twitter.service.bitly.BitlyUrlShortenerFactory
import com.wutsi.twitter.service.twitter.TwitterProvider
import com.wutsi.user.UserApi
import com.wutsi.user.dto.User
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import twitter4j.Status
import twitter4j.StatusUpdate
import twitter4j.TwitterException
import java.net.HttpURLConnection
import java.net.URL
import javax.transaction.Transactional

@Service
public class ShareDelegate(
    @Autowired private val siteProvider: SiteProvider,
    @Autowired private val storyApi: StoryApi,
    @Autowired private val userApi: UserApi,
    @Autowired private val shareDao: ShareRepository,
    @Autowired private val secretDao: SecretRepository,
    @Autowired private val bitly: BitlyUrlShortenerFactory,
    @Autowired private val twitterProvider: TwitterProvider,
    @Autowired private val eventStream: EventStream,
    @Value("\${wutsi.blacklist}") private val userBlacklist: Long
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
        val site = siteProvider.get(story.siteId)
        if (!enabled(site)) {
            LOGGER.warn("Site#${story.siteId} doesn't have Twitter enabled. Ignoring the request")
            return
        }
        if (blacklisted(story.userId)) {
            LOGGER.warn("User#${story.userId} is blacklisted")
            return
        }
        val user = userApi.get(story.userId).user

        val secret = findSecret(story, user, site) ?: return
        val status = share(story, user, secret, site, message, pictureUrl, includeLink, postId)
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

    private fun blacklisted(userId: Long): Boolean =
        userBlacklist == userId

    private fun findSecret(story: Story, user: User, site: Site): SecretEntity? {
        // Find account of the author of the story
        var opt = secretDao.findByUserIdAndSiteId(story.userId, site.id)
        if (opt.isPresent)
            return opt.get()

        // User is a test account
        if (user.testUser) {
            LOGGER.warn("User#${user.id} is a test account. Can't tweet the Story")
            return null
        }

        // Author doesn't have twitter account, return the primary user
        val userId = userId(site)
        if (userId == null) {
            LOGGER.warn("User#${story.userId} doesn't have Twitter secrets. No primary account configured either")
            return null
        }
        opt = secretDao.findByUserIdAndSiteId(userId, site.id)
        if (opt.isPresent) {
            return opt.get()
        } else {
            LOGGER.warn("Primary user doesn't have Twitter secrets configured. Can't tweet the Story")
            return null
        }
    }

    private fun share(
        story: Story,
        user: User,
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
                retweet(status, user, secret, site)
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

    private fun retweet(status: Status, user: User, secret: SecretEntity, site: Site) {
        try {
            // Never retweet primary-user's Stories
            val userId = userId(site)
            if (userId == null || userId == secret.userId) {
                LOGGER.info("User#${secret.userId} is the primary user. No need to retweet his own Story")
                return
            }

            // Never retweet Stories from test account
            if (user.testUser) {
                LOGGER.info("User#${user.id} is a test account. Can't retweet the Story")
                return
            }

            // Retweet
            val primarySecret = secretDao.findByUserIdAndSiteId(userId, site.id)
            if (!primarySecret.isPresent) {
                LOGGER.warn("The primary account doesn't have Twitter secret. Can't retweet")
            } else {
                val twitter = twitterProvider.getTwitter(primarySecret.get().accessToken, primarySecret.get().accessTokenSecret, site) ?: return
                LOGGER.info("Retweeting ${status.id}")
                twitter.retweetStatus(status.id)
            }
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
        site.attributes.find { SiteAttribute.TWITTER_ENABLED.urn == it.urn }?.value == "true"

    private fun userId(site: Site): Long? {
        try {
            return site.attributes.find { SiteAttribute.TWITTER_USER_ID.urn == it.urn }?.value?.toLong()
        } catch (ex: Exception) {
            return null
        }
    }
}
