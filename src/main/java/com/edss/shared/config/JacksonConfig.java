package com.edss.shared.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class JacksonConfig {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer edssJacksonCustomizer() {
        return builder ->
                builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                        .serializationInclusion(JsonInclude.Include.NON_NULL)
                        .featuresToDisable(
                                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .modules(new JavaTimeModule());
    }
}
