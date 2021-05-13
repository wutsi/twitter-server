package com.wutsi.twitter.endpoint

import com.wutsi.twitter.dao.SecretRepository
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.jdbc.Sql

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(value = ["/db/clean.sql", "/db/RevokeSecretController.sql"])
internal class RevokeSecretControllerTest : ControllerTestBase() {
    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var dao: SecretRepository

    @BeforeEach
    override fun setUp() {
        super.setUp()
        login("twitter-manage")
    }

    @Test
    operator fun invoke() {
        val url = "http://localhost:$port/v1/twitter/secrets/1"
        delete(url)

        val secret = dao.findById(1)
        assertFalse(secret.isPresent)
    }
}
