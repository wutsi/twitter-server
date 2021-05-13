package com.wutsi.twitter.endpoint

import com.wutsi.twitter.`delegate`.RevokeSecretDelegate
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.`annotation`.DeleteMapping
import org.springframework.web.bind.`annotation`.PathVariable
import org.springframework.web.bind.`annotation`.RestController
import javax.validation.constraints.NotNull
import kotlin.Long

@RestController
public class RevokeSecretController(
    private val `delegate`: RevokeSecretDelegate
) {
    @DeleteMapping("/v1/twitter/secrets/{id}")
    @PreAuthorize(value = "hasAuthority('twitter-manage')")
    public fun invoke(@PathVariable(name = "id") @NotNull id: Long) {
        delegate.invoke(id)
    }
}
