package com.wutsi.twitter.dto

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

public data class StoreSecretRequest(
    public val siteId: Long = 0,
    @get:NotNull
    public val twitterId: Long = 0,
    @get:NotBlank
    public val accessToken: String = "",
    @get:NotBlank
    public val accessTokenSecret: String = ""
)
