package com.wutsi.twitter.service.twitter

import com.wutsi.site.SiteAttribute
import com.wutsi.site.dto.Site
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

@Service
class TwitterProvider {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(TwitterProvider::class.java)
    }

    fun getTwitter(accessToken: String, accessTokenSecret: String, site: Site): Twitter? {
        val clientId = clientId(site)
        val clientSecret = clientSecret(site)
        if (clientId == null || clientSecret == null) {
            LOGGER.warn("Twitter clientId or clientSecret not configured. No Twitter connectivity available")
            return null
        }

        val cf = ConfigurationBuilder()
        cf
            .setOAuthConsumerKey(clientId)
            .setOAuthConsumerSecret(clientSecret)
            .setOAuthAccessToken(accessToken)
            .setOAuthAccessTokenSecret(accessTokenSecret)

        return TwitterFactory(cf.build()).instance
    }

    private fun clientId(site: Site): String? =
        site.attributes.find { SiteAttribute.TWITTER_CLIENT_ID.urn == it.urn }?.value

    private fun clientSecret(site: Site): String? =
        site.attributes.find { SiteAttribute.TWITTER_CLIENT_SECRET.urn == it.urn }?.value
}
