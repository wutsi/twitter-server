package com.wutsi.twitter.entity

import java.time.OffsetDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "T_SECRET")
data class SecretEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "site_id")
    val siteId: Long = -1,

    @Column(name = "user_id")
    val userId: Long = -1,

    @Column(name = "twitter_id")
    val twitterId: Long = -1,

    @Column(name = "access_token")
    var accessToken: String = "",

    @Column(name = "access_token_secret")
    var accessTokenSecret: String = "",

    @Column(name = "creation_date_time")
    val creationDateTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "modification_date_time")
    var modificationDateTime: OffsetDateTime = OffsetDateTime.now()
)
