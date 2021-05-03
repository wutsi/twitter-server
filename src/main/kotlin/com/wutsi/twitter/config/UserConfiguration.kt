package com.wutsi.twitter.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.security.apikey.ApiKeyRequestInterceptor
import com.wutsi.tracing.TracingRequestInterceptor
import com.wutsi.user.UserApi
import com.wutsi.user.UserApiBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.`annotation`.Bean
import org.springframework.context.`annotation`.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles

@Configuration
public class UserConfiguration(
    @Autowired private val env: Environment,
    @Autowired private val mapper: ObjectMapper,
    @Autowired private val tracingRequestInterceptor: TracingRequestInterceptor,
    @Autowired private val apiKeyRequestInterceptor: ApiKeyRequestInterceptor
) {
    @Bean
    fun userApi(): UserApi =
        UserApiBuilder()
            .build(
                env = userEnvironment(),
                mapper = mapper,
                interceptors = listOf(tracingRequestInterceptor, apiKeyRequestInterceptor)
            )

    fun userEnvironment(): com.wutsi.user.Environment =
        if (env.acceptsProfiles(Profiles.of("prod")))
            com.wutsi.user.Environment.PRODUCTION
        else
            com.wutsi.user.Environment.SANDBOX
}
