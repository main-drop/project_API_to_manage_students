package com.example.StudentsApiC.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    private final ObjectMapper objectMapper;

    public RequestResponseLoggingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long start = System.currentTimeMillis();

        ContentCachingRequestWrapper wrappedRequest =
                new ContentCachingRequestWrapper(
                        request,
                        10240
                );

        ContentCachingResponseWrapper wrappedResponse =
                new ContentCachingResponseWrapper(response);

        try {

            // =====================================
            // EXECUTE REQUEST
            // =====================================

            filterChain.doFilter(
                    wrappedRequest,
                    wrappedResponse
            );

        } finally {

            long duration =
                    System.currentTimeMillis() - start;


            // =====================================
            // REQUEST BODY
            // =====================================

            String requestBody =
                    new String(
                            wrappedRequest.getContentAsByteArray(),
                            StandardCharsets.UTF_8
                    );

            Object requestData = null;

            if (!requestBody.isBlank()) {

                try {

                    requestData =
                            objectMapper.readValue(
                                    requestBody,
                                    Object.class
                            );

                    requestData =
                            removeSensitiveData(
                                    requestData
                            );

                } catch (Exception exception) {

                    requestData = requestBody;
                }
            }


            // =====================================
            // RESPONSE BODY
            // =====================================

            String responseBody =
                    new String(
                            wrappedResponse.getContentAsByteArray(),
                            wrappedResponse.getCharacterEncoding()
                                    != null
                                    ? wrappedResponse.getCharacterEncoding()
                                    : StandardCharsets.UTF_8.name()
                    );

            Object responseData = null;

            if (!responseBody.isBlank()) {

                try {

                    responseData =
                            objectMapper.readValue(
                                    responseBody,
                                    Object.class
                            );

                    responseData =
                            removeSensitiveData(
                                    responseData
                            );

                } catch (Exception exception) {

                    responseData = responseBody;
                }
            }


            // =====================================
            // BUILD REQUEST LOG
            // =====================================

            Map<String, Object> requestLog =
                    new LinkedHashMap<>();

            requestLog.put(
                    "dateTime",
                    LocalDateTime.now().toString()
            );

            requestLog.put(
                    "method",
                    request.getMethod()
            );

            requestLog.put(
                    "path",
                    request.getRequestURI()
            );


            // =====================================
            // LOGIN SPECIAL CASE
            // =====================================

            if (request.getRequestURI().equals("/api/auth/login")
                    && responseData instanceof Map<?, ?> responseMap) {

                Object data =
                        responseMap.get("data");

                if (data instanceof Map<?, ?> dataMap) {

                    Map<String, Object> loginRequest =
                            new LinkedHashMap<>();

                    Object studentCode =
                            dataMap.get("studentCode");

                    Object email =
                            dataMap.get("email");

                    if (studentCode != null) {

                        loginRequest.put(
                                "studentCode",
                                studentCode
                        );
                    }

                    if (email != null) {

                        loginRequest.put(
                                "email",
                                email
                        );
                    }

                    requestLog.put(
                            "request",
                            loginRequest
                    );

                } else {

                    requestLog.put(
                            "request",
                            requestData != null
                                    ? requestData
                                    : new LinkedHashMap<>()
                    );
                }

            } else {

                // =====================================
                // NORMAL REQUEST
                // =====================================

                requestLog.put(
                        "request",
                        requestData != null
                                ? requestData
                                : new LinkedHashMap<>()
                );
            }


            // =====================================
            // LOG REQUEST
            // =====================================

            log.info(
                    "REQUEST {}",
                    toJson(requestLog)
            );


            // =====================================
            // BUILD RESPONSE LOG
            // =====================================

            Map<String, Object> responseLog =
                    new LinkedHashMap<>();

            responseLog.put(
                    "dateTime",
                    LocalDateTime.now().toString()
            );

            responseLog.put(
                    "method",
                    request.getMethod()
            );

            responseLog.put(
                    "path",
                    request.getRequestURI()
            );

            responseLog.put(
                    "status",
                    wrappedResponse.getStatus()
            );

            responseLog.put(
                    "duration",
                    duration + "ms"
            );

            responseLog.put(
                    "body",
                    responseData
            );


            // =====================================
            // LOG RESPONSE
            // =====================================

            log.info(
                    "RESPONSE {}",
                    toJson(responseLog)
            );


            // =====================================
            // COPY RESPONSE BACK TO CLIENT
            // =====================================

            wrappedResponse.copyBodyToResponse();
        }
    }


    // =====================================
    // REMOVE SENSITIVE DATA
    // =====================================

    private Object removeSensitiveData(
            Object data
    ) {

        if (data instanceof Map<?, ?> map) {

            Map<String, Object> result =
                    new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry :
                    map.entrySet()) {

                String key =
                        String.valueOf(
                                entry.getKey()
                        );

                if (isSensitiveField(key)) {
                    continue;
                }

                result.put(
                        key,
                        removeSensitiveData(
                                entry.getValue()
                        )
                );
            }

            return result;
        }


        if (data instanceof List<?> list) {

            return list.stream()
                    .map(this::removeSensitiveData)
                    .toList();
        }


        return data;
    }


    // =====================================
    // SENSITIVE FIELDS
    // =====================================

    private boolean isSensitiveField(
            String fieldName
    ) {

        return switch (
                fieldName.toLowerCase()
                ) {

            case "password",
                 "token",
                 "access_token",
                 "refresh_token",
                 "authorization",
                 "secret",
                 "jwt" -> true;

            default -> false;
        };
    }


    // =====================================
    // OBJECT -> JSON
    // =====================================

    private String toJson(
            Object data
    ) {

        try {

            return objectMapper.writeValueAsString(
                    data
            );

        } catch (JsonProcessingException exception) {

            return "{}";
        }
    }
}