package com.example.StudentsApiC.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Logger;
import feign.Request;
import feign.Response;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class ThirdPartyFeignLogger extends Logger {

    private static final org.slf4j.Logger log =
            LoggerFactory.getLogger("THIRD_PARTY_API");

    private final ObjectMapper objectMapper;

    private final ThreadLocal<RequestInfo> requestInfo =
            new ThreadLocal<>();


    public ThirdPartyFeignLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    // REQUEST INFORMATION

    private static class RequestInfo {

        private final String method;
        private final String url;

        public RequestInfo(
                String method,
                String url
        ) {
            this.method = method;
            this.url = url;
        }
    }


    // DISABLE DEFAULT FEIGN LOGGING

    @Override
    protected void log(
            String configKey,
            String format,
            Object... args
    ) {
        // Custom logging
    }


    // REQUEST

    @Override
    protected void logRequest(
            String configKey,
            Level logLevel,
            Request request
    ) {

        String method =
                request.httpMethod().name();

        String url =
                request.url();


        requestInfo.set(
                new RequestInfo(
                        method,
                        url
                )
        );


        Map<String, Object> requestLog =
                new LinkedHashMap<>();

        requestLog.put(
                "dateTime",
                LocalDateTime.now().toString()
        );

        requestLog.put(
                "method",
                method
        );

        requestLog.put(
                "url",
                url
        );


        log.info(
                "REQUEST {}",
                toJson(requestLog)
        );
    }


    // RESPONSE

    @Override
    protected Response logAndRebufferResponse(
            String configKey,
            Level logLevel,
            Response response,
            long durationTime
    ) throws IOException {

        RequestInfo info =
                requestInfo.get();


        String method =
                info != null
                        ? info.method
                        : response.request()
                        .httpMethod()
                        .name();


        String url =
                info != null
                        ? info.url
                        : response.request()
                        .url();


        // RESPONSE BODY

        String responseBody = "";

        if (response.body() != null) {

            responseBody =
                    new String(
                            response.body()
                                    .asInputStream()
                                    .readAllBytes(),
                            StandardCharsets.UTF_8
                    );
        }


        // RESPONSE LOG

        Map<String, Object> responseLog =
                new LinkedHashMap<>();

        responseLog.put(
                "dateTime",
                LocalDateTime.now().toString()
        );

        responseLog.put(
                "method",
                method
        );

        responseLog.put(
                "url",
                url
        );

        responseLog.put(
                "status",
                response.status()
        );

        responseLog.put(
                "duration",
                durationTime+"ms"
        );


        // RESPONSE DATA

        if (!responseBody.isBlank()) {

            try {

                Object responseData =
                        objectMapper.readValue(
                                responseBody,
                                Object.class
                        );

                responseLog.put(
                        "data",
                        responseData
                );

            } catch (Exception exception) {

                responseLog.put(
                        "data",
                        responseBody
                );
            }

        } else {

            responseLog.put(
                    "data",
                    null
            );
        }


        // LOG RESPONSE

        String json =
                toJson(responseLog);


        if (response.status() >= 400) {

            log.error(
                    "RESPONSE {}",
                    json
            );

        } else {

            log.info(
                    "RESPONSE {}",
                    json
            );
        }


        // CLEAN

        requestInfo.remove();


        // RETURN RESPONSE

        return response.toBuilder()
                .body(
                        responseBody,
                        StandardCharsets.UTF_8
                )
                .build();
    }


    // OBJECT → ONE LINE JSON

    private String toJson(Object data) {

        try {

            return objectMapper.writeValueAsString(data);

        } catch (Exception exception) {

            return "{}";
        }
    }
}