package com.wutsi.twitter.`delegate`

import com.wutsi.twitter.dao.SecretRepository
import com.wutsi.twitter.dto.StoreSecretRequest
import com.wutsi.twitter.dto.StoreSecretResponse
import com.wutsi.twitter.entity.SecretEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import javax.transaction.Transactional

@Service
public class StoreSecretDelegate(
    private val dao: SecretRepository
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(StoreSecretDelegate::class.java)
    }

    @Transactional
    fun invoke(request: StoreSecretRequest): StoreSecretResponse {
        val opt = dao.findByUserIdAndSiteId(request.userId, request.siteId)
        val secret = if (opt.isPresent)
            update(opt.get(), request)
        else
            create(request)

        return StoreSecretResponse(secretId = secret.id!!)
    }

    private fun create(request: StoreSecretRequest): SecretEntity {
        LOGGER.info("Creating secret for User#${request.userId} on Site#${request.siteId}")
        return dao.save(
            SecretEntity(
                userId = request.userId,
                siteId = request.siteId,
                twitterId = request.twitterId,
                accessToken = request.accessToken,
                accessTokenSecret = request.accessTokenSecret
            )
        )
    }

    private fun update(secret: SecretEntity, request: StoreSecretRequest): SecretEntity {
        LOGGER.info("Updating secret for User#${request.userId} on Site#${request.siteId}")
        secret.accessToken = request.accessToken
        secret.accessTokenSecret = request.accessTokenSecret
        secret.modificationDateTime = OffsetDateTime.now()
        return dao.save(secret)
    }
}
