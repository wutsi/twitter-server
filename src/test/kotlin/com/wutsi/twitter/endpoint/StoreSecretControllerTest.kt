package com.wutsi.twitter.endpoint

import com.wutsi.twitter.dao.SecretRepository
import com.wutsi.twitter.dto.StoreSecretRequest
import com.wutsi.twitter.dto.StoreSecretResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(value = ["/db/clean.sql", "/db/StoreSecretController.sql"])
internal class StoreSecretControllerTest : ControllerTestBase() {
    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var dao: SecretRepository

    @BeforeEach
    override fun setUp() {
        login("twitter-manage")
    }

    @Test
    fun `create secret`() {
        val request = StoreSecretRequest(
            siteId = 1L,
            userId = 11L,
            accessTokenSecret = "secret",
            accessToken = "token",
            twitterId = 111L
        )

        val url = "http://localhost:$port/v1/twitter/secrets"
        val response = post(url, request, StoreSecretResponse::class.java)
        assertEquals(OK, response.statusCode)

        val secret = dao.findById(response.body.secretId).get()
        assertEquals(request.userId, secret.userId)
        assertEquals(request.siteId, secret.siteId)
        assertEquals(request.accessToken, secret.accessToken)
        assertEquals(secret.accessTokenSecret, secret.accessTokenSecret)
    }

    @Test
    fun `update secret`() {
        val request = StoreSecretRequest(
            siteId = 1L,
            userId = 1L,
            accessTokenSecret = "secret",
            accessToken = "token",
            twitterId = 222L
        )

        val url = "http://localhost:$port/v1/twitter/secrets"
        val response = post(url, request, StoreSecretResponse::class.java)
        assertEquals(OK, response.statusCode)

        val secret = dao.findById(response.body.secretId).get()
        assertEquals(request.userId, secret.userId)
        assertEquals(request.siteId, secret.siteId)
        assertEquals(request.twitterId, secret.twitterId)
        assertEquals(request.accessToken, secret.accessToken)
        assertEquals(secret.accessTokenSecret, secret.accessTokenSecret)
    }
}
