package com.example.StudentsApiC.common.exception;

import com.example.StudentsApiC.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger("ERROR_LOG");

    private static final ObjectMapper objectMapper =
            new ObjectMapper();

    /*
     * ==========================================
     * Runtime Exception
     * ==========================================
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>>
    handleRuntimeException(RuntimeException ex) {

        try {

            Map<String, Object> errorLog =
                    new LinkedHashMap<>();

            errorLog.put(
                    "event",
                    "RUNTIME_EXCEPTION"
            );

            errorLog.put(
                    "type",
                    ex.getClass().getSimpleName()
            );

            errorLog.put(
                    "message",
                    ex.getMessage()
            );

            log.error(
                    "{}",
                    objectMapper.writeValueAsString(
                            errorLog
                    )
            );

        } catch (Exception e) {

            log.error(
                    ex.getMessage(),
                    ex
            );
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ApiResponse<>(
                                false,
                                ex.getMessage(),
                                null
                        )
                );
    }

    /*
     * ==========================================
     * Validation Exception
     * ==========================================
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>>
    handleValidation(
            MethodArgumentNotValidException ex
    ) {

        Map<String, String> errors =
                new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> {

                    String field =
                            error.getField();

                    String message =
                            error.getDefaultMessage();

                    errors.put(
                            field,
                            message
                    );

                    try {

                        Map<String, Object> errorLog =
                                new LinkedHashMap<>();

                        errorLog.put(
                                "event",
                                "VALIDATION_ERROR"
                        );

                        errorLog.put(
                                "field",
                                field
                        );

                        errorLog.put(
                                "message",
                                message
                        );

                        log.error(
                                "{}",
                                objectMapper.writeValueAsString(
                                        errorLog
                                )
                        );

                    } catch (Exception ignored) {
                    }
                });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ApiResponse<>(
                                false,
                                "Validation failed",
                                errors
                        )
                );
    }

    /*

     * Path Variable Type Mismatch

     */
    @ExceptionHandler(
            MethodArgumentTypeMismatchException.class
    )
    public ResponseEntity<ApiResponse<Object>>
    handleTypeMismatch(
            MethodArgumentTypeMismatchException ex
    ) {

        String parameterName =
                ex.getName();

        Object value =
                ex.getValue();

        String expectedType =
                ex.getRequiredType() != null
                        ? ex.getRequiredType()
                        .getSimpleName()
                        : "Unknown";

        try {

            Map<String, Object> errorLog =
                    new LinkedHashMap<>();

            errorLog.put(
                    "event",
                    "TYPE_MISMATCH_ERROR"
            );

            errorLog.put(
                    "parameter",
                    parameterName
            );

            errorLog.put(
                    "value",
                    value
            );

            errorLog.put(
                    "expectedType",
                    expectedType
            );

            log.error(
                    "{}",
                    objectMapper.writeValueAsString(
                            errorLog
                    )
            );

        } catch (Exception e) {

            log.error(
                    ex.getMessage(),
                    ex
            );
        }

        String message =
                "Invalid value for parameter '"
                        + parameterName
                        + "'. Expected type: "
                        + expectedType;

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ApiResponse<>(
                                false,
                                message,
                                null
                        )
                );
    }

    /*
     * ==========================================
     * Unknown Exception
     * ==========================================
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>>
    handleException(Exception ex) {

        try {

            Map<String, Object> errorLog =
                    new LinkedHashMap<>();

            errorLog.put(
                    "event",
                    "INTERNAL_SERVER_ERROR"
            );

            errorLog.put(
                    "type",
                    ex.getClass().getSimpleName()
            );

            errorLog.put(
                    "message",
                    ex.getMessage()
            );

            log.error(
                    "{}",
                    objectMapper.writeValueAsString(
                            errorLog
                    )
            );

        } catch (Exception e) {

            log.error(
                    ex.getMessage(),
                    ex
            );
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        new ApiResponse<>(
                                false,
                                "Internal server error",
                                null
                        )
                );
    }
}