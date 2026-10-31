package com.campusbook.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * 统一 LocalDateTime 的收发格式为 yyyy-MM-dd HH:mm:ss。
 * Spring Boot 的 spring.jackson.date-format 只对 java.util.Date 生效，
 * java.time 系列默认走 ISO-8601，前端按常见格式传参就会解析失败。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter PATTERN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> {
            builder.serializers(new LocalDateTimeSerializer(PATTERN));
            builder.deserializers(new LocalDateTimeDeserializer(PATTERN));
        };
    }
}
