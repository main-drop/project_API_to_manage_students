package com.example.StudentsApiC.client;
import com.example.StudentsApiC.Students.entity.Student;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Logger;
import feign.Request;
import feign.Response;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
public class ThirdPartyFeignLogger extends Logger {

    private static final org.slf4j.Logger log =
            LoggerFactory.getLogger("THIRD_PARTY_API");

    private final ObjectMapper objectMapper;

    // Stores request metadata + body across the thread lifecycle
    private final ThreadLocal<RequestInfo> requestInfo =
            new ThreadLocal<>();

    public ThirdPartyFeignLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // =====================================
    // REQUEST INFORMATION
    // =====================================

    private static class RequestInfo {

        private final String method;
        private final String url;
        private final Object body;

        public RequestInfo(
                String method,
                String url,
                Object body
        ) {
            this.method = method;
            this.url = url;
            this.body = body;
        }
    }

    // =====================================
    // SUPPRESS DEFAULT FEIGN LOGGING
    // =====================================

    @Override
    protected void log(
            String configKey,
            String format,
            Object... args
    ) {
        // Suppress default Feign multi-line logging
    }
    // =====================================
    // 1. REQUEST LOGGING
    // =====================================

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

        Object parsedBody = null;

        // =====================================
        // REQUEST BODY
        // =====================================

        if (request.body() != null) {

            String requestBodyStr =
                    new String(
                            request.body(),
                            StandardCharsets.UTF_8
                    );

            try {

                parsedBody =
                        objectMapper.readValue(
                                requestBodyStr,
                                Object.class
                        );

            } catch (Exception exception) {

                parsedBody = requestBodyStr;
            }
        }

        // =====================================
        // STORE REQUEST INFO
        // =====================================

        requestInfo.set(
                new RequestInfo(
                        method,
                        url,
                        parsedBody
                )
        );

        // =====================================
        // BUILD REQUEST LOG
        // =====================================

        Map<String, Object> requestLog =
                new LinkedHashMap<>();

        requestLog.put(
                "dateTime",
                LocalDateTime.now().toString()
        );

        // USER
        requestLog.put(
                "user",
                getCurrentUser()
        );

        requestLog.put(
                "method",
                method
        );

        requestLog.put(
                "url",
                url
        );

        requestLog.put(
                "body",
                parsedBody
        );

        // =====================================
        // LOG REQUEST
        // =====================================

        log.info(
                "REQUEST {}",
                toJson(requestLog)
        );
    }

    // =====================================
    // 2. SUCCESS / HTTP RESPONSE
    // =====================================

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
                        : response.request().url();

        // =====================================
        // RESPONSE BODY
        // =====================================

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

        // =====================================
        // BUILD RESPONSE LOG
        // =====================================

        Map<String, Object> responseLog =
                new LinkedHashMap<>();

        responseLog.put(
                "dateTime",
                LocalDateTime.now().toString()
        );

        // USER
        responseLog.put(
                "user",
                getCurrentUser()
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
                durationTime + "ms"
        );

        // =====================================
        // RESPONSE DATA
        // =====================================

        if (!responseBody.isBlank()) {

            try {

                responseLog.put(
                        "data",
                        objectMapper.readValue(
                                responseBody,
                                Object.class
                        )
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

        String json =
                toJson(responseLog);

        // =====================================
        // HTTP ERROR
        // =====================================

        if (response.status() >= 400) {

            log.error(
                    "RESPONSE ERROR {}",
                    json
            );

            triggerAlertNotification(
                    "HTTP Error "
                            + response.status()
                            + " on "
                            + method
                            + " "
                            + url,
                    json
            );

        } else {

            // =====================================
            // SUCCESS
            // =====================================

            log.info(
                    "RESPONSE SUCCESS {}",
                    json
            );
        }

        // =====================================
        // CLEAN THREADLOCAL
        // =====================================

        requestInfo.remove();

        // =====================================
        // RETURN RESPONSE TO FEIGN
        // =====================================

        return response
                .toBuilder()
                .body(
                        responseBody,
                        StandardCharsets.UTF_8
                )
                .build();
    }

    // =====================================
    // 3. TIMEOUT / NETWORK ERROR
    // =====================================

    @Override
    protected IOException logIOException(
            String configKey,
            Level logLevel,
            IOException ioe,
            long elapsedTime
    ) {

        RequestInfo info =
                requestInfo.get();

        String method =
                info != null
                        ? info.method
                        : "UNKNOWN";

        String url =
                info != null
                        ? info.url
                        : "UNKNOWN";

        // =====================================
        // CLASSIFY ERROR
        // =====================================

        String errorType;
        String message;

        String exceptionMessage =
                ioe.getMessage() != null
                        ? ioe.getMessage().toLowerCase()
                        : "";

        if (exceptionMessage.contains("connect timed out")
                || exceptionMessage.contains(
                "connection timed out"
        )) {

            errorType =
                    "CONNECT_TIMEOUT";

            message =
                    "Third-party API connection timeout";

        } else if (
                exceptionMessage.contains("read timed out")
                        || exceptionMessage.contains(
                        "sockettimeout"
                )
        ) {

            errorType =
                    "READ_TIMEOUT";

            message =
                    "Third-party API response read timeout";

        } else if (
                ioe instanceof java.net.SocketTimeoutException
        ) {

            errorType =
                    "TIMEOUT";

            message =
                    "Third-party API timeout";

        } else {

            errorType =
                    "NETWORK_ERROR";

            message =
                    "Third-party API network error";
        }

        // =====================================
        // BUILD ERROR LOG
        // =====================================

        Map<String, Object> errorLog =
                new LinkedHashMap<>();

        errorLog.put(
                "dateTime",
                LocalDateTime.now().toString()
        );

        // USER
        errorLog.put(
                "user",
                getCurrentUser()
        );

        errorLog.put(
                "method",
                method
        );

        errorLog.put(
                "url",
                url
        );

        errorLog.put(
                "errorType",
                errorType
        );

        errorLog.put(
                "message",
                message
        );

        errorLog.put(
                "duration",
                elapsedTime + "ms"
        );

        errorLog.put(
                "exception",
                ioe.getClass()
                        .getSimpleName()
        );

        errorLog.put(
                "error",
                ioe.getMessage()
        );

        String json =
                toJson(errorLog);

        // =====================================
        // LOG ERROR
        // =====================================

        log.error(
                "RESPONSE {}",
                json
        );

        // =====================================
        // ALERT
        // =====================================

        triggerAlertNotification(
                message
                        + " ["
                        + errorType
                        + "]",
                json
        );

        // =====================================
        // CLEAN THREADLOCAL
        // =====================================

        requestInfo.remove();

        return ioe;
    }

    // =====================================
    // GET CURRENT USER
    // =====================================

    private Map<String, Object> getCurrentUser() {

        Map<String, Object> userLog =
                new LinkedHashMap<>();

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        // No authenticated user
        if (authentication == null
                || !authentication.isAuthenticated()) {

            return userLog;
        }

        Object principal =
                authentication.getPrincipal();

        // Student authenticated user
        if (principal instanceof Student student) {

            userLog.put(
                    "studentCode",
                    student.getStudentCode()
            );

            userLog.put(
                    "name",
                    student.getName()
            );

            userLog.put(
                    "email",
                    student.getEmail()
            );
        }

        return userLog;
    }

    // =====================================
    // OBJECT -> JSON
    // =====================================

    private String toJson(Object data) {

        try {

            return objectMapper.writeValueAsString(
                    data
            );

        } catch (Exception exception) {

            return "{}";
        }
    }

    // =====================================
    // ALERT
    // =====================================

    private void triggerAlertNotification(
            String summary,
            String jsonPayload
    ) {

        System.err.println(
                "ALERT SENT: " + summary
        );
    }
}