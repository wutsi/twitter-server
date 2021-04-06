package com.wutsi.twitter.service.twitter

import com.wutsi.site.dto.Site
import com.wutsi.twitter.AttributeUrn
import org.springframework.stereotype.Service
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

@Service
class TwitterProvider {
    fun getTwitter(accessToken: String, accessTokenSecret: String, site: Site): Twitter? {
        val clientId = clientId(site)
        val clientSecret = clientSecret(site)
        if (clientId == null || clientSecret == null)
            return null

        val cf = ConfigurationBuilder()
        cf
            .setOAuthConsumerKey(clientId)
            .setOAuthConsumerSecret(clientSecret)
            .setOAuthAccessToken(accessToken)
            .setOAuthAccessTokenSecret(accessTokenSecret)

        return TwitterFactory(cf.build()).instance
    }

    private fun clientId(site: Site): String? =
        site.attributes.find { AttributeUrn.CLIENT_ID.urn == it.urn }?.value

    private fun clientSecret(site: Site): String? =
        site.attributes.find { AttributeUrn.CLIENT_SECRET.urn == it.urn }?.value
}
