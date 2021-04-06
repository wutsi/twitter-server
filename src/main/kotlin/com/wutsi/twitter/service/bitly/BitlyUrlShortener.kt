package com.wutsi.twitter.service.bitly

import com.wutsi.site.dto.Site
import com.wutsi.twitter.AttributeUrn
import net.swisstech.bitly.BitlyClient
import net.swisstech.bitly.model.Response
import net.swisstech.bitly.model.v3.ShortenResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BitlyUrlShortener {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(BitlyUrlShortener::class.java)
    }

    fun shorten(url: String, site: Site): String {
        val accessToken = accessToken(site) ?: return url

        val client = BitlyClient(accessToken)
        val resp: Response<ShortenResponse> = client.shorten()
            .setLongUrl(url)
            .call()

        if (resp.status_code / 100 == 2)
            return resp.data.url
        else {
            LOGGER.warn("Unable to shorten $url. Error=${resp.status_code} - ${resp.status_txt}")
            return url
        }
    }

    private fun accessToken(site: Site): String? =
        site.attributes.find { it.urn == AttributeUrn.BITLY_ACCESS_TOKEN.urn }?.value
}
