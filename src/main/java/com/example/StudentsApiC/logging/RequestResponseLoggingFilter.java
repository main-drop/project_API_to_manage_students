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
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RequestResponseLoggingFilter
        extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

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


        // REQUEST

        Map<String, Object> requestLog = new LinkedHashMap<>();

        requestLog.put("dateTime", LocalDateTime.now().toString());

        requestLog.put("method", request.getMethod());

        requestLog.put("path", request.getRequestURI());

        log.info("REQUEST {}", toJson(requestLog));



        // RESPONSE WRAPPER


        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, wrappedResponse);
        } finally {

            long duration = System.currentTimeMillis() - start;

            // RESPONSE BODY

            String responseBody =
                    new String(
                            wrappedResponse
                                    .getContentAsByteArray(),
                            wrappedResponse
                                    .getCharacterEncoding()
                    );


            // RESPONSE LOG

            Map<String, Object> responseLog = new LinkedHashMap<>();

            responseLog.put("dateTime", LocalDateTime.now().toString());

            responseLog.put("method", request.getMethod());

            responseLog.put("path", request.getRequestURI());

            responseLog.put("status", wrappedResponse.getStatus());

            responseLog.put("durationMs", duration);



            // RESPONSE DATA


            if (!responseBody.isBlank()) {

                try {

                    Object responseData =
                            objectMapper.readValue(
                                    responseBody,
                                    Object.class
                            );

                    responseData = removeSensitiveData(responseData);

                    responseLog.put("data", responseData);

                } catch (Exception exception) {

                    responseLog.put("data", responseBody);
                }
            }



            // LOG RESPONSE


            log.info("RESPONSE {}", toJson(responseLog));



            // COPY RESPONSE


            wrappedResponse.copyBodyToResponse();
        }
    }



    // REMOVE SENSITIVE DATA


    private Object removeSensitiveData(
            Object data
    ) {

        if (data instanceof Map<?, ?> map) {

            Map<String, Object> result =
                    new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry :
                    map.entrySet()) {

                String key =
                        String.valueOf(entry.getKey());

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



    // SENSITIVE FIELDS


    private boolean isSensitiveField(
            String fieldName
    ) {

        return switch (
                fieldName.toLowerCase()
                ) {

            case "token",
                 "password",
                 "authorization",
                 "access_token",
                 "refresh_token",
                 "secret",
                 "jwt" -> true;

            default -> false;
        };
    }



    // OBJECT → JSON


    private String toJson(Object data) {

        try {

            return objectMapper.writeValueAsString(data);

        } catch (JsonProcessingException exception) {

            return "{}";
        }
    }
}

