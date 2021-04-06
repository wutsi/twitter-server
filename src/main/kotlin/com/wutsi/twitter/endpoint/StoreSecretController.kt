package com.wutsi.twitter.endpoint

import com.wutsi.twitter.delegate.StoreSecretDelegate
import com.wutsi.twitter.dto.StoreSecretRequest
import com.wutsi.twitter.dto.StoreSecretResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
public class StoreSecretController(
    private val `delegate`: StoreSecretDelegate
) {
    @PostMapping("/v1/twitter/secrets/{user-id}")
    public fun invoke(
        @PathVariable(name = "user-id") userId: Long,
        @Valid @RequestBody
        request: StoreSecretRequest
    ): StoreSecretResponse = delegate.invoke(userId, request)
}
