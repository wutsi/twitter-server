package com.wutsi.twitter.delegate

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.bitly.BitlyUrlShortener
import com.wutsi.platform.site.SiteProvider
import com.wutsi.site.SiteAttribute.TWITTER_CLIENT_ID
import com.wutsi.site.SiteAttribute.TWITTER_CLIENT_SECRET
import com.wutsi.site.SiteAttribute.TWITTER_ENABLED
import com.wutsi.site.SiteAttribute.TWITTER_USER_ID
import com.wutsi.site.dto.Attribute
import com.wutsi.site.dto.Site
import com.wutsi.story.StoryApi
import com.wutsi.story.dto.GetStoryResponse
import com.wutsi.story.dto.Story
import com.wutsi.twitter.dao.ShareRepository
import com.wutsi.twitter.service.bitly.BitlyUrlShortenerFactory
import com.wutsi.twitter.service.twitter.TwitterProvider
import com.wutsi.user.UserApi
import com.wutsi.user.dto.GetUserResponse
import com.wutsi.user.dto.User
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
    private lateinit var siteProvider: SiteProvider

    @MockBean
    private lateinit var storyApi: StoryApi

    @MockBean
    private lateinit var userApi: UserApi

    @MockBean
    private lateinit var twitterProvider: TwitterProvider

    private lateinit var twitter: Twitter

    @MockBean
    private lateinit var bitlyFactory: BitlyUrlShortenerFactory

    private val shortenUrl = "https://bit.ly/123"

    private lateinit var site: Site

    @BeforeEach
    fun setUp() {
        val bitly = mock<BitlyUrlShortener>()
        doReturn(shortenUrl).whenever(bitly).shorten(any())
        doReturn(bitly).whenever(bitlyFactory).get(any())

        twitter = mock()
        doReturn(twitter).whenever(twitterProvider).getTwitter(any(), any(), any())

        val user = createUser()
        doReturn(GetUserResponse(user)).whenever(userApi).get(any())

        site = createSite()
        doReturn(site).whenever(siteProvider).get(1)
    }

    @Test
    fun `save message to DB when sharing post`() {
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
        val story = createStory()
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        delegate.invoke(123, "Yo man", null, true, 111L)

        verify(twitter).updateStatus("Yo man https://bit.ly/123")
    }

    @Test
    fun `tweet when sharing a post without link`() {
        val story = createStory()
        doReturn(GetStoryResponse(story)).whenever(storyApi).get(123L)

        delegate.invoke(123, "Yo man", null, false, 111L)

        verify(twitter).updateStatus("Yo man")
    }

    @Test
    fun `tweet picture when sharing a post with picture`() {
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
            Attribute(TWITTER_ENABLED.urn, "true"),
            Attribute(TWITTER_CLIENT_SECRET.urn, "client-secret"),
            Attribute(TWITTER_CLIENT_ID.urn, "client-id"),
            Attribute(TWITTER_USER_ID.urn, "666")
        )
    ) = Site(
        id = 1L,
        domainName = "www.wutsi.com",
        websiteUrl = "https://www.wutsi.com",
        attributes = attributes
    )

    private fun createUser(testUser: Boolean = false) = User(
        id = 1,
        testUser = testUser
    )

    private fun createStatus(id: Long, twitterId: Long): Status {
        val status = mock<Status>()
        doReturn(id).whenever(status).id

        val user = mock<twitter4j.User>()
        doReturn(twitterId).whenever(user).id
        doReturn(user).whenever(status).user

        return status
    }
}
