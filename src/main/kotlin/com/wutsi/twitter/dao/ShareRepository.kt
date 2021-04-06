package com.wutsi.twitter.dao

import com.wutsi.twitter.entity.ShareEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ShareRepository : CrudRepository<ShareEntity, Long>
