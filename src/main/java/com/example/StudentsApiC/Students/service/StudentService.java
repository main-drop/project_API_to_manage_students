package com.example.StudentsApiC.Students.service;

import com.example.StudentsApiC.Students.dto.request.StudentRequest;
import com.example.StudentsApiC.Students.entity.Student;
import com.example.StudentsApiC.Students.repository.StudentRepository;
import com.example.StudentsApiC.auth.dto.LoginRequest;
import com.example.StudentsApiC.auth.dto.LoginResponse;
import com.example.StudentsApiC.cache.StudentCacheService;
import com.example.StudentsApiC.client.ThirdPartyClient;
import com.example.StudentsApiC.client.ThirdPartyUserRequest;
import com.example.StudentsApiC.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentService {
    private static final Logger logger = LoggerFactory.getLogger(StudentService.class);

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ThirdPartyClient thirdPartyClient;
    private final StudentCacheService studentCacheService;

    public StudentService(
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ThirdPartyClient thirdPartyClient,
            StudentCacheService studentCacheService) {

        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.thirdPartyClient = thirdPartyClient;
        this.studentCacheService = studentCacheService;
    }


    // THIRD-PARTY API - GET ONE USER

    public Map<String, Object> getThirdPartyUser(Integer userId) {
        logger.info("Calling third-party API: userId={}", userId);
        try {
            Map<String, Object> result = thirdPartyClient.getUser(userId);

            logger.info("Third-party API response received: userId={}", userId);

            return result;

        } catch (Exception exception) {

            logger.error("Third-party API failed: userId={}, error={}",
                    userId,
                    exception.getMessage(),
                    exception
            );

            throw new RuntimeException("Third-party API failed", exception);
        }
    }


    // THIRD-PARTY API - GET ALL USERS

    public List<Map<String, Object>> getAllThirdPartyUsers() {

        logger.info("Calling third-party API: get all users");

        try {

            List<Map<String, Object>> result = thirdPartyClient.getAllUsers();

            logger.info("Third-party API response received: totalUsers={}", result.size());

            return result;

        } catch (Exception exception) {

            logger.error("Third-party API failed while getting all users: error={}",
                    exception.getMessage(),
                    exception
            );

            throw new RuntimeException("Third-party API failed", exception);
        }
    }


    // THIRD-PARTY API - CREATE USER

    public Map<String, Object> createThirdPartyUser(ThirdPartyUserRequest request) {
        logger.info("THIRD_PARTY_REQUEST: method=POST, path=/users, data={}", request);

        try {

            Map<String, Object> user = new HashMap<>();
            user.put("name", request.getName());
            user.put("username", request.getUsername());
            user.put("email", request.getEmail());
            Map<String, Object> response = thirdPartyClient.createUser(user);

            logger.info("THIRD_PARTY_RESPONSE: method=POST, path=/users, data={}", response);

            return response;

        } catch (Exception exception) {

            logger.error("THIRD_PARTY_ERROR: method=POST, path=/users, message={}",
                    exception.getMessage(),
                    exception
            );

            throw exception;
        }
    }

    // CREATE STUDENT

    public Student create(StudentRequest request) {

        logger.info("Creating new student: email={}", request.getEmail());

        try {

            Student student = new Student();
            student.setStudentCode("STU-" + System.currentTimeMillis());
            student.setName(request.getName());
            student.setGender(request.getGender());
            student.setEmail(request.getEmail());
            student.setPassword(passwordEncoder.encode(request.getPassword()));
            student.setPhone(request.getPhone());
            student.setAddress(request.getAddress());
            student.setCreatedAt(LocalDateTime.now());
            Student savedStudent = studentRepository.save(student);

            logger.info("Student created successfully: studentCode={}", savedStudent.getStudentCode());

            /*
             * We don't have to put the new student into Redis
             * immediately.
             *
             * The cache will be created when GET /{id}
             * is called.
             */

            return savedStudent;

        } catch (Exception exception) {

            logger.error("Failed to create student: email={}, error={}",
                    request.getEmail(),
                    exception.getMessage(),
                    exception
            );

            throw exception;
        }
    }


    // GET ALL STUDENTS

    public List<Student> findAll() {

        logger.info("Getting all students");

        try {

            // 1. Check Redis
            List<Student> cachedStudents = studentCacheService.getAll();
            if (cachedStudents != null) {

                logger.info("Students found in Redis cache: count={}", cachedStudents.size());

                return cachedStudents;
            }

            // 2. Redis MISS → MySQL
            logger.info("Students not found in Redis, querying MySQL");

            List<Student> students = studentRepository.findAll();

            // 3. Save to Redis
            studentCacheService.saveAll(students);

            logger.info("Students cached in Redis: count={}", students.size());

            return students;

        } catch (Exception exception) {

            logger.error("Failed to get students: error={}",
                    exception.getMessage(),
                    exception
            );

            throw exception;
        }
    }


    // GET STUDENT BY ID

    public Student findById(Long studentId) {

        logger.info("Getting student: studentId={}", studentId);

        try {


            // 1. CHECK REDIS FIRST


            Student cachedStudent = studentCacheService.get(studentId);

            if (cachedStudent != null) {

                logger.info("Student found in Redis cache: studentId={}", studentId);

                return cachedStudent;
            }



            // 2. REDIS MISS → CHECK MYSQL


            logger.info("Student not found in Redis, querying MySQL: studentId={}", studentId);

            Student student = studentRepository
                            .findById(studentId)
                            .orElseThrow(() -> {

                                logger.warn("Student not found: studentId={}", studentId);

                                return new RuntimeException("Student not found");
                            });


            // =================================================
            // 3. SAVE MYSQL RESULT INTO REDIS
            // =================================================

            studentCacheService.save(studentId, student);

            logger.info("Student cached in Redis: studentId={}", studentId);

            return student;

        } catch (RuntimeException exception) {

            logger.error(
                    "Failed to get student: studentId={}, error={}",
                    studentId,
                    exception.getMessage(),
                    exception
            );

            throw exception;
        }
    }



    // UPDATE STUDENT


    public Student update(
            Long studentId,
            StudentRequest request) {

        logger.info(
                "Updating student: studentId={}",
                studentId
        );

        try {

            // Get existing student
            Student student = findById(studentId);
            student.setName(request.getName());
            student.setGender(request.getGender());

            student.setEmail(request.getEmail());

            student.setPhone(request.getPhone());

            student.setAddress(request.getAddress());


            // Update password only when provided
            if (request.getPassword() != null && !request.getPassword().isBlank()) {

                logger.info("Updating password: studentId={}", studentId);
                student.setPassword(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                );
            }
            student.setUpdatedAt(LocalDateTime.now());
            // Save to MySQL

            Student updatedStudent = studentRepository.save(student);

            // UPDATE REDIS
            studentCacheService.save(studentId, updatedStudent);

            logger.info("Redis cache updated: studentId={}", studentId);


            logger.info("Student updated successfully: studentId={}", studentId);

            return updatedStudent;

        } catch (Exception exception) {

            logger.error("Failed to update student: studentId={}, error={}",
                    studentId,
                    exception.getMessage(),
                    exception
            );

            throw exception;
        }
    }



    // DELETE STUDENT


    public void delete(Long studentId) {

        logger.info("Deleting student: studentId={}", studentId);

        try {

            Student student = findById(studentId);

            // Delete from MySQL
            studentRepository.delete(student);



            // DELETE FROM REDIS
            studentCacheService.delete(studentId);

            logger.info("Student removed from Redis: studentId={}", studentId);


            logger.info("Student deleted successfully: studentId={}", studentId);

        } catch (Exception exception) {

            logger.error(
                    "Failed to delete student: studentId={}, error={}",
                    studentId,
                    exception.getMessage(),
                    exception
            );

            throw exception;
        }
    }


    // LOGIN

    public LoginResponse login(LoginRequest request) {

        Student student = studentRepository.findByEmail(request.getEmail()).orElse(null);

        if (student == null) {

            logger.warn("Login failed: user does not exist, email={}", request.getEmail());

            throw new RuntimeException("Invalid email or password");
        }

        boolean passwordMatches =
                passwordEncoder.matches(request.getPassword(), student.getPassword());

        if (!passwordMatches) {
            logger.warn("Login failed: invalid password, email={}", request.getEmail());
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtService.generateToken(student.getEmail());

        logger.info("Login successful: email={}, studentCode={}", student.getEmail(), student.getStudentCode());


        return new LoginResponse(
                token,
                student.getStudentCode(),
                student.getName(),
                student.getEmail()
        );
    }
}