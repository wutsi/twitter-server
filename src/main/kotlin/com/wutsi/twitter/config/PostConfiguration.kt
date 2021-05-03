package com.wutsi.twitter.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.post.PostApi
import com.wutsi.post.PostApiBuilder
import com.wutsi.security.apikey.ApiKeyRequestInterceptor
import com.wutsi.tracing.TracingRequestInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles

@Configuration
public class PostConfiguration(
    @Autowired private val env: Environment,
    @Autowired private val mapper: ObjectMapper,
    @Autowired private val tracingRequestInterceptor: TracingRequestInterceptor,
    @Autowired private val apiKeyRequestInterceptor: ApiKeyRequestInterceptor
) {
    @Bean
    fun postApi(): PostApi =
        PostApiBuilder()
            .build(
                env = postEnvironment(),
                mapper = mapper,
                interceptors = listOf(tracingRequestInterceptor, apiKeyRequestInterceptor)
            )

    fun postEnvironment(): com.wutsi.post.Environment =
        if (env.acceptsProfiles(Profiles.of("prod")))
            com.wutsi.post.Environment.PRODUCTION
        else
            com.wutsi.post.Environment.SANDBOX
}
