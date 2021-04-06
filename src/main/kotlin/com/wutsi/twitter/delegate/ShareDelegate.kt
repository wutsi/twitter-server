package com.wutsi.twitter.`delegate`

import com.wutsi.site.SiteApi
import com.wutsi.site.dto.Site
import com.wutsi.story.StoryApi
import com.wutsi.story.dto.Story
import com.wutsi.twitter.AttributeUrn
import com.wutsi.twitter.dao.SecretRepository
import com.wutsi.twitter.dao.ShareRepository
import com.wutsi.twitter.entity.SecretEntity
import com.wutsi.twitter.entity.ShareEntity
import com.wutsi.twitter.service.bitly.BitlyUrlShortener
import com.wutsi.twitter.service.twitter.TwitterProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import twitter4j.Status
import twitter4j.TwitterException
import javax.transaction.Transactional

@Service
public class ShareDelegate(
    @Autowired private val siteApi: SiteApi,
    @Autowired private val storyApi: StoryApi,
    @Autowired private val shareDao: ShareRepository,
    @Autowired private val secretDao: SecretRepository,
    @Autowired private val bitly: BitlyUrlShortener,
    @Autowired private val twitterProvider: TwitterProvider
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(ShareDelegate::class.java)
    }

    @Transactional
    fun invoke(storyId: Long) {
        val story = storyApi.get(storyId).story
        val site = siteApi.get(1).site
        if (!enabled(site))
            return

        val opt = secretDao.findByUserIdAndSiteId(story.userId, site.id)
        if (!opt.isPresent)
            return

        val account = opt.get()
        try {
            val status = share(story, account, site)
            if (status != null) {
                save(story, site, account, status)
            }
        } catch (ex: TwitterException) {
            LOGGER.error("Unable to share the story", ex)
            save(story, site, account, ex)
        }
    }

    private fun share(story: Story, secret: SecretEntity, site: Site): Status? {
        val twitter = twitterProvider.getTwitter(secret.accessToken, secret.accessTokenSecret, site) ?: return null

        val text = text(story, site)
        LOGGER.info("Sharing to ${secret.twitterId}: $text")
        return twitter.updateStatus(text)
    }

    private fun save(story: Story, site: Site, secret: SecretEntity, ex: TwitterException) {
        try {
            shareDao.save(
                ShareEntity(
                    storyId = story.id,
                    siteId = site.id,
                    secret = secret,
                    statusId = null,
                    success = false,
                    errorCode = ex.errorCode,
                    errorMessage = ex.errorMessage
                )
            )
        } catch (ex: Exception) {
            LOGGER.warn("Unable to store the share information", ex)
        }
    }

    private fun save(story: Story, site: Site, secret: SecretEntity, status: Status) {
        try {
            shareDao.save(
                ShareEntity(
                    storyId = story.id,
                    siteId = site.id,
                    secret = secret,
                    statusId = status.id,
                    success = true
                )
            )
        } catch (ex: Exception) {
            LOGGER.warn("Unable to store the share information", ex)
        }
    }

    private fun text(story: Story, site: Site): String {
        val url = bitly.shorten("${site.websiteUrl}${story.slug}?utm_source=twitter", site)
        return if (story.socialMediaMessage.isNullOrEmpty())
            "${story.title} $url"
        else
            "${story.socialMediaMessage} $url"
    }

    private fun enabled(site: Site): Boolean =
        site.attributes.find { AttributeUrn.ENABLED.urn == it.urn }?.value == "true"
}
