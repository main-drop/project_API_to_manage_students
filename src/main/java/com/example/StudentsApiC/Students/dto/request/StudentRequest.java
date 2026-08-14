package com.example.StudentsApiC.Students.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudentRequest {


    @NotBlank(message = "Name is required")
    private String name;

    private String gender;

    @NotBlank(message = "Email is required")
    @Email(message = "Email invalid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String phone;

    private String address;
}