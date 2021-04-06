package com.wutsi.twitter.dto

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import kotlin.Long
import kotlin.String

public data class Account(
    public val id: Long = 0,
    @get:NotNull
    public val twitterId: Long = 0,
    @get:NotBlank
    public val twitterScreenName: String = "",
    public val pictureUrl: String? = null,
    @get:NotBlank
    public val accessToken: String = "",
    @get:NotBlank
    public val accessTokenSecret: String = ""
)
