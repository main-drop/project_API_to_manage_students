package com.example.StudentsApiC.cache;

import com.example.StudentsApiC.Students.entity.Student;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.Duration;

@Service
public class StudentCacheService {
    private static final String KEY_PREFIX = "student:";
    private static final String ALL_STUDENTS_KEY = "students:all";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public StudentCacheService(
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper) {

        this.redisTemplate = redisTemplate;

        this.objectMapper = objectMapper.copy();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // SAVE One Student TO CATCH
    public void save(Long studentId, Student student) {

        String key = KEY_PREFIX + studentId;

        redisTemplate.opsForValue().set(
                key,
                student,
                Duration.ofMinutes(2)
        );
    }

    // GET One for Student
    public Student get(Long studentId) {

        String key = KEY_PREFIX + studentId;

        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        return objectMapper.convertValue(
                value,
                Student.class
        );
    }

    // DELETE Student
    public void delete(Long studentId) {

        String key = KEY_PREFIX + studentId;

        redisTemplate.delete(key);
    }

    

    // SAVE ALL STUDENTS TO CATCH
    public void saveAll(List<Student> students) {
        redisTemplate.opsForValue().set(
                ALL_STUDENTS_KEY,
                students,
                Duration.ofMinutes(10)
        );
    }

    // GET ALL STUDENTS
    public List<Student> getAll() {
        Object value = redisTemplate.opsForValue().get(ALL_STUDENTS_KEY);
        if (value == null) {
            return null;
        }
        return objectMapper.convertValue(
                value,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Student.class)
        );
    }

    // DELETE ALL STUDENTS
    public void deleteAll() {
        redisTemplate.delete(ALL_STUDENTS_KEY);
    }
}