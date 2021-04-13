package com.wutsi.twitter.dao

import com.wutsi.twitter.entity.ShareEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ShareRepository : CrudRepository<ShareEntity, Long> {
    fun findByStoryIdAndPostId(storyId: Long, postId: Long?): Optional<ShareEntity>
}
