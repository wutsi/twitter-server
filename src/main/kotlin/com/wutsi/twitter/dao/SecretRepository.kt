package com.wutsi.twitter.dao

import com.wutsi.twitter.entity.SecretEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SecretRepository : CrudRepository<SecretEntity, Long> {
    fun findByUserIdAndSiteId(userId: Long, siteId: Long): Optional<SecretEntity>
}
