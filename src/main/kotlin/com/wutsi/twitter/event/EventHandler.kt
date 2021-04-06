package com.wutsi.twitter.event

import com.wutsi.channel.event.ChannelEventType
import com.wutsi.channel.event.ChannelSecretRevokedEventPayload
import com.wutsi.channel.event.ChannelSecretSubmittedEventPayload
import com.wutsi.story.event.StoryEventPayload
import com.wutsi.story.event.StoryEventType
import com.wutsi.stream.Event
import com.wutsi.stream.ObjectMapperBuilder
import com.wutsi.twitter.delegate.RevokeSecretDelegate
import com.wutsi.twitter.delegate.ShareDelegate
import com.wutsi.twitter.delegate.StoreSecretDelegate
import com.wutsi.twitter.dto.StoreSecretRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class EventHandler(
    @Autowired private val shareDelegate: ShareDelegate,
    @Autowired private val storeSecretDelegate: StoreSecretDelegate,
    @Autowired private val revokeSecretDelegate: RevokeSecretDelegate
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(EventHandler::class.java)
    }

    @EventListener
    fun onEvent(event: Event) {
        LOGGER.info("onEvent($event)")

        if (event.type == StoryEventType.PUBLISHED.urn) {
            val payload = ObjectMapperBuilder().build().readValue(event.payload, StoryEventPayload::class.java)
            shareDelegate.invoke(payload.storyId)
        } else if (event.type == ChannelEventType.SECRET_SUBMITTED.urn) {
            val payload = ObjectMapperBuilder().build().readValue(event.payload, ChannelSecretSubmittedEventPayload::class.java)
            if (payload.type == "twitter") {
                storeSecretDelegate.invoke(
                    request = StoreSecretRequest(
                        userId = payload.userId,
                        siteId = payload.siteId,
                        twitterId = payload.twitterId,
                        accessToken = payload.accessToken,
                        accessTokenSecret = payload.accessTokenSecret
                    )
                )
            } else {
                LOGGER.info("Ignore event for ${payload.type}")
            }
        } else if (event.type == ChannelEventType.SECRET_REVOKED.urn) {
            val payload = ObjectMapperBuilder().build().readValue(event.payload, ChannelSecretRevokedEventPayload::class.java)
            if (payload.type == "twitter") {
                revokeSecretDelegate.invoke(
                    userId = payload.userId,
                    siteId = payload.siteId
                )
            } else {
                LOGGER.info("Ignore event for ${payload.type}")
            }
        }
    }
}
