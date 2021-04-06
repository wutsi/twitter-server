package com.wutsi.twitter.event

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.wutsi.story.event.StoryEventType.PUBLISHED
import com.wutsi.stream.Event
import com.wutsi.twitter.delegate.ShareDelegate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class EventHandlerTest {
    lateinit var shareDelegate: ShareDelegate

    lateinit var handler: EventHandler

    @BeforeEach
    fun setUp() {
        shareDelegate = mock()
        handler = EventHandler(shareDelegate)
    }

    @Test
    fun `published stories are shared on telegram`() {
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
}
