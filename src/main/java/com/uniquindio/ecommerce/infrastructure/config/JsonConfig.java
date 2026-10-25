package com.uniquindio.ecommerce.infrastructure.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.EnumFeature;

@Configuration
public class JsonConfig {
    @Bean
    public JsonMapperBuilderCustomizer strictApiNumbers() {
        return builder -> builder.disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
    }
}
