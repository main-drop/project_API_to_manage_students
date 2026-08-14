package com.example.StudentsApiC.Students.controller;

import com.example.StudentsApiC.Students.dto.request.StudentRequest;
import com.example.StudentsApiC.Students.entity.Student;
import com.example.StudentsApiC.Students.service.StudentService;
import com.example.StudentsApiC.client.ThirdPartyUserRequest;
import com.example.StudentsApiC.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService studentService;
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // CREATE STUDEN

    @PostMapping
    public ResponseEntity<ApiResponse<Student>> create(
            @Valid @RequestBody StudentRequest request) {

        Student student =
                studentService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(
                                true,
                                "Student created successfully",
                                student
                        )
                );
    }


    // =========================================================
    // GET ALL STUDENTS
    // =========================================================

    @GetMapping("/getlist")
    public ResponseEntity<ApiResponse<List<Student>>> findAll() {

        List<Student> students =
                studentService.findAll();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Get students successfully",
                        students
                )
        );
    }


    // =========================================================
    // GET STUDENT BY ID
    // =========================================================

    @GetMapping("/getOne/{studentId}")
    public ResponseEntity<ApiResponse<Student>> findById(
            @PathVariable Long studentId) {

        Student student =
                studentService.findById(studentId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Get student successfully",
                        student
                )
        );
    }


    // =========================================================
    // UPDATE STUDENT
    // =========================================================

    @PutMapping("/update/{studentId}")
    public ResponseEntity<ApiResponse<Student>> update(
            @PathVariable Long studentId,
            @Valid @RequestBody StudentRequest request) {

        Student student =
                studentService.update(
                        studentId,
                        request
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Student updated successfully",
                        student
                )
        );
    }


    // =========================================================
    // DELETE STUDENT
    // =========================================================

    @DeleteMapping("/delete/{studentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long studentId) {

        studentService.delete(studentId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Student deleted successfully",
                        null
                )
        );
    }


    // =========================================================
    // THIRD-PARTY API - GET ONE USER
    // =========================================================

    @GetMapping("/third-party/{userId}")
    public ResponseEntity<ApiResponse<Object>> getThirdPartyUser(
            @PathVariable Integer userId) {

        Map<String, Object> data =
                studentService.getThirdPartyUser(userId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Third-party API called successfully",
                        data
                )
        );
    }


    // =========================================================
    // THIRD-PARTY API - GET ALL USERS
    // =========================================================

    @GetMapping("/third-party")
    public ResponseEntity<ApiResponse<Object>> getAllThirdPartyUsers() {

        List<Map<String, Object>> data =
                studentService.getAllThirdPartyUsers();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Third-party API called successfully",
                        data
                )
        );
    }


    // =========================================================
    // THIRD-PARTY API - CREATE USER
    // =========================================================

    @PostMapping("/third-party")
    public ResponseEntity<ApiResponse<Object>> createThirdPartyUser(
            @Valid @RequestBody ThirdPartyUserRequest request) {

        Map<String, Object> data =
                studentService.createThirdPartyUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(
                                true,
                                "Third-party user created successfully",
                                data
                        )
                );
    }
}