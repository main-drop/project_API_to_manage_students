package com.example.StudentsApiC.security;

import com.example.StudentsApiC.Students.entity.Student;
import com.example.StudentsApiC.Students.repository.StudentRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final StudentRepository studentRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            StudentRepository studentRepository
    ) {
        this.jwtService = jwtService;
        this.studentRepository = studentRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader =
                request.getHeader("Authorization");

        // No JWT
        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        // Get JWT
        String token =
                authHeader.substring(7);

        // Get email from JWT
        String email =
                jwtService.extractUsername(token);

        // Find student
        Student student =
                studentRepository
                        .findByEmail(email)
                        .orElse(null);

        // Validate JWT
        if (student != null
                && jwtService.isTokenValid(
                token,
                student.getEmail()
        )) {

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            student,
                            null,
                            AuthorityUtils.NO_AUTHORITIES
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            // Save Student into SecurityContext
            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}