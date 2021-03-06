package com.wutsi.twitter.endpoint

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.bitly.BitlyUrlShortener
import com.wutsi.platform.site.SiteProvider
import com.wutsi.site.SiteAttribute
import com.wutsi.site.dto.Attribute
import com.wutsi.site.dto.Site
import com.wutsi.story.StoryApi
import com.wutsi.story.dto.GetStoryResponse
import com.wutsi.story.dto.Story
import com.wutsi.stream.EventStream
import com.wutsi.twitter.dao.ShareRepository
import com.wutsi.twitter.event.TwitterEventType
import com.wutsi.twitter.event.TwitterSharedEventPayload
import com.wutsi.twitter.service.bitly.BitlyUrlShortenerFactory
import com.wutsi.twitter.service.twitter.TwitterProvider
import com.wutsi.user.UserApi
import com.wutsi.user.dto.GetUserResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.jdbc.Sql
import twitter4j.Status
import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.User
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(value = ["/db/clean.sql", "/db/ShareController.sql"])
internal class ShareControllerTest : ControllerTestBase() {
    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var dao: ShareRepository

    @MockBean
    private lateinit var siteProvider: SiteProvider

    @MockBean
    private lateinit var storyApi: StoryApi

    @MockBean
    private lateinit var twitterProvider: TwitterProvider

    private lateinit var twitter: Twitter

    @MockBean
    private lateinit var bitlyFactory: BitlyUrlShortenerFactory

    @MockBean
    private lateinit var eventStream: EventStream

    @MockBean
    private lateinit var userApi: UserApi

    private val shortenUrl = "https://bit.ly/123"

    lateinit var site: Site

    @BeforeEach
    override fun setUp() {
        super.setUp()

        val bitly = mock<BitlyUrlShortener>()
        doReturn(shortenUrl).whenever(bitly).shorten(any())
        doReturn(bitly).whenever(bitlyFactory).get(any())

        twitter = mock()
        doReturn(twitter).whenever(twitterProvider).getTwitter(any(), any(), any())

        val user = createUser()
        doReturn(GetUserResponse(user)).whenever(userApi).get(any())

        site = createSite()
        doReturn(site).whenever(siteProvider).get(1)

        login("twitter-share")
    }

    @Test
    @Sql(value = ["/db/clean.sql", "/db/ShareController.sql"])
    fun `save message to DB when sharing story`() {
        val story = createStory()
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        val status = createStatus(11L, 111L)
        doReturn(status).whenever(twitter).updateStatus(anyString())

        val url = "http://127.0.0.1:$port/v1/twitter/share?story-id=123"
        get(url, Any::class.java)

        val shares = dao.findAll().toList()[0]
        assertEquals(status.id, shares.statusId)
        assertEquals(story.id, shares.storyId)
        assertEquals(site.id, shares.siteId)
        assertTrue(shares.success)
        assertNull(shares.errorCode)
        assertNull(shares.errorMessage)
    }

    @Test
    @Sql(value = ["/db/clean.sql", "/db/ShareController.sql"])
    fun `save error to DB when sharing story fails`() {
        val story = createStory()
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        val ex = mock<TwitterException>()
        doReturn(666).whenever(ex).errorCode
        doReturn("Failed !!!").whenever(ex).errorMessage
        doThrow(ex).whenever(twitter).updateStatus(anyString())

        val url = "http://127.0.0.1:$port/v1/twitter/share?story-id=123"
        get(url, Any::class.java)

        val shares = dao.findAll().toList()[0]
        assertNull(shares.statusId)
        assertEquals(story.id, shares.storyId)
        assertEquals(site.id, shares.siteId)
        assertFalse(shares.success)
        assertEquals(666, shares.errorCode)
        assertEquals("Failed !!!", shares.errorMessage)
    }

    @Test
    fun `tweet when story sharing story with socialMediaMessage`() {
        val story = createStory()
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        val url = "http://127.0.0.1:$port/v1/twitter/share?story-id=123"
        get(url, Any::class.java)

        verify(twitter).updateStatus("This is nice https://bit.ly/123")
    }

    @Test
    fun `tweet when story sharing story  without socialMediaMessage`() {
        val story = createStory(socialMediaMessage = null)
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        val url = "http://127.0.0.1:$port/v1/twitter/share?story-id=123"
        get(url, Any::class.java)

        verify(twitter).updateStatus("${story.title} https://bit.ly/123")
    }

    @Test
    @Sql(value = ["/db/clean.sql", "/db/ShareController.sql"])
    fun `tweet using primary account when sharing a story having author with no twitter secret`() {
        val story = createStory(socialMediaMessage = null, userId = 999)
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        val status = createStatus(11L, 111L)
        doReturn(status).whenever(twitter).updateStatus(anyString())

        val url = "http://127.0.0.1:$port/v1/twitter/share?story-id=123"
        get(url, Any::class.java)

        verify(twitter).updateStatus("${story.title} https://bit.ly/123")

        val shares = dao.findAll().toList()[0]
        assertEquals(666, shares.secret.id)
    }

    @Test
    @Sql(value = ["/db/clean.sql", "/db/ShareController.sql"])
    fun `do not tweet tweet using primary account when sharing a story having test-account with no twitter secret`() {
        val user = createUser(id = 999, testUser = true)
        doReturn(GetUserResponse(user)).whenever(userApi).get(999)

        val story = createStory(socialMediaMessage = null, userId = 999)
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        val url = "http://127.0.0.1:$port/v1/twitter/share?story-id=123"
        get(url, Any::class.java)

        verify(twitter, never()).updateStatus("${story.title} https://bit.ly/123")
        assertTrue(dao.findAll().toList().isEmpty())
    }

    @Test
    @Sql(value = ["/db/clean.sql", "/db/ShareController.sql"])
    fun `do not tweet tweet blacklist users`() {
        val blacklistUserId = 2142L
        val user = createUser(id = blacklistUserId)
        doReturn(GetUserResponse(user)).whenever(userApi).get(blacklistUserId)

        val story = createStory(socialMediaMessage = null, userId = blacklistUserId)
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        val url = "http://127.0.0.1:$port/v1/twitter/share?story-id=123"
        get(url, Any::class.java)

        verify(twitter, never()).updateStatus("${story.title} https://bit.ly/123")
        assertTrue(dao.findAll().toList().isEmpty())
    }

    @Test
    @Sql(value = ["/db/clean.sql", "/db/ShareController.sql"])
    fun `do not tweet when sharing a story having author with no twitter secret - no primary user set`() {
        val site = createSite(
            attributes = listOf(
                Attribute(SiteAttribute.TWITTER_ENABLED.urn, "true"),
                Attribute(SiteAttribute.TWITTER_CLIENT_SECRET.urn, "client-secret"),
                Attribute(SiteAttribute.TWITTER_CLIENT_ID.urn, "client-id")
            )
        )
        doReturn(site).whenever(siteProvider).get(1)

        val story = createStory(socialMediaMessage = null, userId = 999)
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        val url = "http://127.0.0.1:$port/v1/twitter/share?story-id=123"
        get(url, Any::class.java)

        verify(twitter, never()).updateStatus(anyString())

        val shares = dao.findAll()
        assertTrue(shares.toList().isEmpty())
    }

    @Test
    fun `do not tweet when flag not enabled`() {
        val site = createSite(
            attributes = listOf(
                Attribute(SiteAttribute.TWITTER_CLIENT_SECRET.urn, "client-secret"),
                Attribute(SiteAttribute.TWITTER_CLIENT_ID.urn, "client-id"),
            )
        )
        doReturn(site).whenever(siteProvider).get(1)

        val story = createStory()
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        val url = "http://127.0.0.1:$port/v1/twitter/share?story-id=123"
        get(url, Any::class.java)

        verify(twitter, never()).updateStatus(anyString())
    }

    @Test
    fun `do not tween when twitter instance not available`() {
        val story = createStory()
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        doReturn(null).whenever(twitterProvider).getTwitter(any(), any(), any())

        val url = "http://127.0.0.1:$port/v1/twitter/share?story-id=123"
        get(url, Any::class.java)

        verify(twitter, never()).updateStatus(anyString())
    }

    @Test
    fun `retweet when sharing a story from a non-primary user`() {
        val story = createStory()
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        val status = createStatus(11L, 111L)
        doReturn(status).whenever(twitter).updateStatus(anyString())

        val url = "http://127.0.0.1:$port/v1/twitter/share?story-id=123"
        get(url, Any::class.java)

        verify(twitter).retweetStatus(11L)
    }

    @Test
    fun `do not retweet when sharing a story from primary user`() {
        val story = createStory(userId = 666L)
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        val status = createStatus(11L, 111L)
        doReturn(status).whenever(twitter).updateStatus(anyString())

        val url = "http://127.0.0.1:$port/v1/twitter/share?story-id=123"
        get(url, Any::class.java)

        verify(twitter, never()).retweetStatus(any())
    }

    @Test
    fun `do not retweet when sharing a story from test user`() {
        val user = createUser(id = 2, testUser = true)
        doReturn(GetUserResponse(user)).whenever(userApi).get(2)

        val story = createStory(userId = 2)
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        val status = createStatus(11L, 111L)
        doReturn(status).whenever(twitter).updateStatus(anyString())

        val url = "http://127.0.0.1:$port/v1/twitter/share?story-id=123"
        get(url, Any::class.java)

        verify(twitter, never()).retweetStatus(any())
    }

    @Test
    fun `event send when sharing a story`() {
        val story = createStory()
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        val status = createStatus(11L, 111L)
        doReturn(status).whenever(twitter).updateStatus(anyString())

        val url = "http://127.0.0.1:$port/v1/twitter/share?story-id=123"
        get(url, Any::class.java)

        val payload = argumentCaptor<TwitterSharedEventPayload>()
        verify(eventStream).publish(eq(TwitterEventType.SHARED.urn), payload.capture())
        assertEquals(11L, payload.firstValue.twitterStatusId)
        assertNull(payload.firstValue.postId)
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
            Attribute(SiteAttribute.TWITTER_ENABLED.urn, "true"),
            Attribute(SiteAttribute.TWITTER_CLIENT_SECRET.urn, "client-secret"),
            Attribute(SiteAttribute.TWITTER_CLIENT_ID.urn, "client-id"),
            Attribute(SiteAttribute.TWITTER_USER_ID.urn, "666")
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

    private fun createUser(id: Long = 1, testUser: Boolean = false) = com.wutsi.user.dto.User(
        id = id,
        testUser = testUser
    )
}
