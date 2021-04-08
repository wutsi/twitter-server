package com.wutsi.twitter.event

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.channel.event.ChannelEventType
import com.wutsi.post.PostApi
import com.wutsi.post.dto.GetPostResponse
import com.wutsi.post.dto.Post
import com.wutsi.post.event.PostEventType
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

    lateinit var postApi: PostApi

    @BeforeEach
    fun setUp() {
        shareDelegate = mock()
        storeSecretDelegate = mock()
        revokedSecretDelegate = mock()
        postApi = mock()
        handler = EventHandler(shareDelegate, storeSecretDelegate, revokedSecretDelegate, postApi)
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
                type = ChannelEventType.SECRET_SUBMITTED.urn,
                payload = """
                    {
                        "userId": 11,
                        "siteId": 1,
                        "channelUserId": "4409403",
                        "accessToken": "token",
                        "accessTokenSecret": "secret",
                        "type": "twitter"
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
                type = ChannelEventType.SECRET_REVOKED.urn,
                payload = """
                    {
                        "userId": 11,
                        "siteId": 1,
                        "type": "twitter"
                    }
                """.trimIndent()
            )
        )

        verify(revokedSecretDelegate).invoke(11L, 1L)
    }

    @Test
    fun `post to Twitter submitted are shared`() {
        val post = Post(
            id = 1L,
            message = "Yo man",
            includeLink = true,
            storyId = 11L,
            channelType = "twitter",
            pictureUrl = "https://www.google.com/1.png"
        )
        doReturn(GetPostResponse(post)).whenever(postApi).get(any())

        handler.onEvent(
            Event(
                id = UUID.randomUUID().toString(),
                type = PostEventType.SUBMITTED.urn,
                payload = """
                    {
                        "postId": 1
                    }
                """.trimIndent()
            )
        )

        verify(shareDelegate).invoke(11L, "Yo man", "https://www.google.com/1.png", true, 1L)
    }

    @Test
    fun `post to LinkedIn submitted are not shared`() {
        val post = Post(
            id = 1L,
            message = "Yo man",
            includeLink = true,
            storyId = 11L,
            channelType = "linkedin",
            pictureUrl = "https://www.google.com/1.png"
        )
        doReturn(GetPostResponse(post)).whenever(postApi).get(any())

        handler.onEvent(
            Event(
                id = UUID.randomUUID().toString(),
                type = PostEventType.SUBMITTED.urn,
                payload = """
                    {
                        "postId": 1
                    }
                """.trimIndent()
            )
        )

        verify(shareDelegate, never()).invoke(any(), any(), any(), any(), any())
    }
}
