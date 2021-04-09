package com.wutsi.twitter.delegate

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.site.SiteApi
import com.wutsi.site.dto.Attribute
import com.wutsi.site.dto.GetSiteResponse
import com.wutsi.site.dto.Site
import com.wutsi.story.StoryApi
import com.wutsi.story.dto.GetStoryResponse
import com.wutsi.story.dto.Story
import com.wutsi.stream.EventStream
import com.wutsi.twitter.AttributeUrn.CLIENT_ID
import com.wutsi.twitter.AttributeUrn.CLIENT_SECRET
import com.wutsi.twitter.AttributeUrn.ENABLED
import com.wutsi.twitter.AttributeUrn.USER_ID
import com.wutsi.twitter.dao.ShareRepository
import com.wutsi.twitter.service.bitly.BitlyUrlShortener
import com.wutsi.twitter.service.twitter.TwitterProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.jdbc.Sql
import twitter4j.Status
import twitter4j.StatusUpdate
import twitter4j.Twitter
import twitter4j.User

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(value = ["/db/clean.sql", "/db/ShareDelegate.sql"])
internal class ShareDelegateTest {
    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var delegate: ShareDelegate

    @Autowired
    private lateinit var dao: ShareRepository

    @MockBean
    private lateinit var siteApi: SiteApi

    @MockBean
    private lateinit var storyApi: StoryApi

    @MockBean
    private lateinit var twitterProvider: TwitterProvider

    private lateinit var twitter: Twitter

    @MockBean
    private lateinit var bitly: BitlyUrlShortener

    @MockBean
    private lateinit var eventStream: EventStream

    private val shortenUrl = "https://bit.ly/123"

    @BeforeEach
    fun setUp() {
        doReturn(shortenUrl).whenever(bitly).shorten(any(), any())

        twitter = mock()
        doReturn(twitter).whenever(twitterProvider).getTwitter(any(), any(), any())
    }

    @Test
    fun `save message to DB when sharing post`() {
        val site = createSite()
        doReturn(GetSiteResponse(site)).whenever(siteApi).get(1)

        val story = createStory()
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123)

        val status = createStatus(11L, 111L)
        doReturn(status).whenever(twitter).updateStatus(ArgumentMatchers.anyString())

        delegate.invoke(123, "Yo man", null, true, 111L)

        val shares = dao.findAll().toList()[0]
        assertEquals(status.id, shares.statusId)
        assertEquals(story.id, shares.storyId)
        assertEquals(site.id, shares.siteId)
        assertTrue(shares.success)
        assertNull(shares.errorCode)
        assertNull(shares.errorMessage)
        assertEquals(111L, shares.postId)
    }

    @Test
    fun `tweet when sharing a post`() {
        val site = createSite()
        doReturn(GetSiteResponse(site)).whenever(siteApi).get(1L)

        val story = createStory()
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        delegate.invoke(123, "Yo man", null, true, 111L)

        verify(twitter).updateStatus("Yo man https://bit.ly/123")
    }

    @Test
    fun `tweet when sharing a post without link`() {
        val site = createSite()
        doReturn(GetSiteResponse(site)).whenever(siteApi).get(1L)

        val story = createStory()
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        delegate.invoke(123, "Yo man", null, false, 111L)

        verify(twitter).updateStatus("Yo man")
    }

    @Test
    fun `tweet picture when sharing a post with picture`() {
        val site = createSite()
        doReturn(GetSiteResponse(site)).whenever(siteApi).get(1L)

        val story = createStory()
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        delegate.invoke(123, "Yo man", "https://picsum.photos/200", true, 111L)

        val update = argumentCaptor<StatusUpdate>()
        verify(twitter).updateStatus(update.capture())
    }

    private fun createStory(
        userId: Long = 1L,
        socialMediaMessage: String? = "This is nice"
    ) = Story(
        id = 123L,
        title = "This is a story title",
        slug = "/read/123/this-is-a-story-title",
        socialMediaMessage = socialMediaMessage,
        userId = userId
    )

    private fun createSite(
        attributes: List<Attribute> = listOf(
            Attribute(ENABLED.urn, "true"),
            Attribute(CLIENT_SECRET.urn, "client-secret"),
            Attribute(CLIENT_ID.urn, "client-id"),
            Attribute(USER_ID.urn, "666")
        )
    ) = Site(
        id = 1L,
        domainName = "www.wutsi.com",
        websiteUrl = "https://www.wutsi.com",
        attributes = attributes
    )

    private fun createStatus(id: Long, twitterId: Long): Status {
        val status = mock<Status>()
        doReturn(id).whenever(status).id

        val user = mock<User>()
        doReturn(twitterId).whenever(user).id
        doReturn(user).whenever(status).user

        return status
    }
}