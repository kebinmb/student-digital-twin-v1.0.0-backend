package com.sdt.web_app.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.util.List;
import java.util.Map;

@Configuration
public class JacksonHardeningConfig {
    @Bean
    public JsonMapperBuilderCustomizer polymorphicTypeValidatorCustomizer(){
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType("com.sdt.web_app.dto.")
                .allowIfSubType("com.sdt.web_app.dto.")
                .allowIfBaseType(List.class)
                .allowIfBaseType(Map.class)
                .build();
        return jsonMapperBuilder ->  jsonMapperBuilder.polymorphicTypeValidator(ptv);
    }
}
