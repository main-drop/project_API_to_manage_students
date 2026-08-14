package com.example.StudentsApiC;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class StudentsApiCApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudentsApiCApplication.class, args);
	}

}