package com.example.StudentsApiC.auth.controller;

import com.example.StudentsApiC.common.response.ApiResponse;
import com.example.StudentsApiC.auth.dto.LoginRequest;
import com.example.StudentsApiC.auth.dto.LoginResponse;
import com.example.StudentsApiC.Students.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final StudentService studentService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request) {
        LoginResponse response = studentService.login(request);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Login successful",
                        response
                )
        );
    }
}