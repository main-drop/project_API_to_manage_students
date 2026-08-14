package com.example.StudentsApiC.config;

import com.example.StudentsApiC.common.response.ApiResponse;
import com.example.StudentsApiC.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

@Configuration
public class SecurityConfig {

    private static final Logger log =
            LoggerFactory.getLogger("API_SECURITY");

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final ObjectMapper objectMapper;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    // =========================================================
    // SECURITY FILTER CHAIN
    // =========================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // =================================================
                // CSRF
                // =================================================

                .csrf(AbstractHttpConfigurer::disable)

                // =================================================
                // SESSION
                // =================================================

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // =================================================
                // EXCEPTION HANDLING
                // =================================================

                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(
                                        authenticationEntryPoint()
                                )
                                .accessDeniedHandler(
                                        accessDeniedHandler()
                                )
                )

                // =================================================
                // AUTHORIZATION
                // =================================================

                .authorizeHttpRequests(auth -> auth

                        // Login
                        .requestMatchers("/api/auth/**")
                        .permitAll()

                        // Student APIs
                        .requestMatchers("/api/students/**")
                        .authenticated()

                        // Everything else
                        .anyRequest()
                        .authenticated()
                )

                // =================================================
                // JWT FILTER
                // =================================================

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // =========================================================
    // 401 UNAUTHORIZED
    // =========================================================

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {

        return (request, response, authException) -> {

            String requestUrl =
                    (String) request.getAttribute(
                            RequestDispatcher.ERROR_REQUEST_URI
                    );

            if (requestUrl == null || requestUrl.isBlank()) {
                requestUrl = request.getRequestURI();
            }

            // =================================================
            // REQUEST LOG
            // =================================================

            log.error(
                    "REQUEST {}",
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "method",
                                    request.getMethod(),

                                    "url",
                                    requestUrl
                            )
                    )
            );

            // =================================================
            // RESPONSE LOG
            // =================================================

            log.error(
                    "RESPONSE {}",
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "status",
                                    401,

                                    "message",
                                    "Unauthorized: Please login first"
                            )
                    )
            );

            // =================================================
            // RESPONSE BODY
            // =================================================

            ApiResponse<Object> body =
                    new ApiResponse<>(
                            false,
                            "Unauthorized: Please login first",
                            null
                    );

            response.setContentType(
                    MediaType.APPLICATION_JSON_VALUE
            );

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.getWriter().write(
                    objectMapper.writeValueAsString(body)
            );
        };
    }

    // =========================================================
    // 403 FORBIDDEN
    // =========================================================

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {

        return (request, response, accessDeniedException) -> {

            String requestUrl =
                    (String) request.getAttribute(
                            RequestDispatcher.ERROR_REQUEST_URI
                    );

            if (requestUrl == null || requestUrl.isBlank()) {
                requestUrl = request.getRequestURI();
            }

            // =================================================
            // REQUEST LOG
            // =================================================

            log.error(
                    "REQUEST {}",
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "method",
                                    request.getMethod(),

                                    "url",
                                    requestUrl
                            )
                    )
            );
            // =================================================
            // RESPONSE LOG
            // =================================================

            log.error("RESPONSE {}", objectMapper.writeValueAsString(
                            Map.of(
                                    "status",
                                    403,

                                    "message",
                                    "Forbidden: You don't have permission"
                            )
                    )
            );
            // =================================================
            // RESPONSE BODY
            // =================================================

            ApiResponse<Object> body =
                    new ApiResponse<>(
                            false,
                            "Forbidden: You don't have permission",
                            null
                    );

            response.setContentType(
                    MediaType.APPLICATION_JSON_VALUE
            );

            response.setStatus(
                    HttpServletResponse.SC_FORBIDDEN
            );

            response.getWriter().write(
                    objectMapper.writeValueAsString(body)
            );
        };
    }
    // =========================================================
    // PASSWORD ENCODER
    // =========================================================

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }
}