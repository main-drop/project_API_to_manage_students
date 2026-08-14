package com.example.StudentsApiC.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Logger;
import org.springframework.context.annotation.Bean;

public class ThirdPartyClientConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public Logger thirdPartyLogger(
            ObjectMapper objectMapper
    ) {
        return new ThirdPartyFeignLogger(objectMapper);
    }
}