package com.example.StudentsApiC.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        // Key
        template.setKeySerializer(
                new StringRedisSerializer()
        );

        // Value
        JacksonJsonRedisSerializer<Object> jsonSerializer =
                new JacksonJsonRedisSerializer<>(
                        Object.class
                );

        template.setValueSerializer(jsonSerializer);

        // Hash key
        template.setHashKeySerializer(
                new StringRedisSerializer()
        );

        // Hash value
        template.setHashValueSerializer(
                jsonSerializer
        );

        template.afterPropertiesSet();

        return template;
    }
}