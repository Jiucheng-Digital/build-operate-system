package com.neucity.common.security.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2025/10/11 10:23
 */
@Configuration
public class WebConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper jacksonMapper(Jackson2ObjectMapperBuilder builder){
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        SimpleModule sm = new SimpleModule();
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        sm.addSerializer(Long.class, ToStringSerializer.instance);
        sm.addSerializer(long.class, ToStringSerializer.instance);
        sm.addSerializer(Long.TYPE, ToStringSerializer.instance);
        sm.addSerializer(BigDecimal.class, ToStringSerializer.instance);
        objectMapper.registerModule(sm);
        return objectMapper;
    }

}
