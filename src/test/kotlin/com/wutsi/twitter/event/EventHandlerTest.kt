package com.wutsi.twitter.event

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.wutsi.story.event.StoryEventType.PUBLISHED
import com.wutsi.stream.Event
import com.wutsi.twitter.delegate.RevokeSecretDelegate
import com.wutsi.twitter.delegate.ShareDelegate
import com.wutsi.twitter.delegate.StoreSecretDelegate
import com.wutsi.twitter.dto.StoreSecretRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

internal class EventHandlerTest {
    lateinit var shareDelegate: ShareDelegate

    lateinit var storeSecretDelegate: StoreSecretDelegate

    lateinit var revokedSecretDelegate: RevokeSecretDelegate

    lateinit var handler: EventHandler

    @BeforeEach
    fun setUp() {
        shareDelegate = mock()
        storeSecretDelegate = mock()
        revokedSecretDelegate = mock()
        handler = EventHandler(shareDelegate, storeSecretDelegate, revokedSecretDelegate)
    }

    @Test
    fun `published stories are shared`() {
        handler.onEvent(
            Event(
                id = UUID.randomUUID().toString(),
                type = PUBLISHED.urn,
                payload = """
                    {
                        "storyId": 123
                    }
                """.trimIndent()
            )
        )

        verify(shareDelegate).invoke(123)
    }

    @Test
    fun `submitted secrets are stored`() {
        handler.onEvent(
            Event(
                id = UUID.randomUUID().toString(),
                type = TwitterEventType.SECRET_SUBMITTED.urn,
                payload = """
                    {
                        "userId": 11,
                        "siteId": 1,
                        "twitterId": 4409403,
                        "accessToken": "token",
                        "accessTokenSecret": "secret"
                    }
                """.trimIndent()
            )
        )

        val request = argumentCaptor<StoreSecretRequest>()
        verify(storeSecretDelegate).invoke(request.capture())

        assertEquals(1L, request.firstValue.siteId)
        assertEquals(11L, request.firstValue.userId)
        assertEquals(4409403L, request.firstValue.twitterId)
        assertEquals("token", request.firstValue.accessToken)
        assertEquals("secret", request.firstValue.accessTokenSecret)
    }

    @Test
    fun `revoked secrets are deleted`() {
        handler.onEvent(
            Event(
                id = UUID.randomUUID().toString(),
                type = TwitterEventType.SECRET_REVOKED.urn,
                payload = """
                    {
                        "userId": 11,
                        "siteId": 1
                    }
                """.trimIndent()
            )
        )

        verify(revokedSecretDelegate).invoke(11L, 1L)
    }
}
