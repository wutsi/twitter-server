package com.wutsi.twitter.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.platform.security.apikey.ApiKeyRequestInterceptor
import com.wutsi.story.StoryApi
import com.wutsi.story.StoryApiBuilder
import com.wutsi.tracing.TracingRequestInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles

@Configuration
public class StoryConfiguration(
    @Autowired private val env: Environment,
    @Autowired private val mapper: ObjectMapper,
    @Autowired private val tracingRequestInterceptor: TracingRequestInterceptor,
    @Autowired private val apiKeyRequestInterceptor: ApiKeyRequestInterceptor
) {
    @Bean
    fun storyApi(): StoryApi =
        StoryApiBuilder()
            .build(
                env = storyEnvironment(),
                mapper = mapper,
                interceptors = listOf(tracingRequestInterceptor, apiKeyRequestInterceptor)
            )

    fun storyEnvironment(): com.wutsi.story.Environment =
        if (env.acceptsProfiles(Profiles.of("prod")))
            com.wutsi.story.Environment.PRODUCTION
        else
            com.wutsi.story.Environment.SANDBOX
}
