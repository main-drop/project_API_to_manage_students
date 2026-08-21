package com.example.StudentsApiC.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@FeignClient(
        name = "thirdPartyClient",
        url = "https://jsonplaceholder.typicode.com/users",
        configuration = ThirdPartyClientConfig.class
)
public interface ThirdPartyClient {

    // CREATE - POST

    @PostMapping
    Map<String, Object> createUser(
            @RequestBody Map<String, Object> user
    );

    // READ - GET ONE
    @GetMapping("/{id}")
    Map<String, Object> getUser(
//            @RequestParam("_delay") int delayInMilliseconds
            @PathVariable("id") Integer id
    );

    // READ - GET ALL
    @GetMapping
    List<Map<String, Object>> getAllUsers();

    // UPDATE - PUT
    @PutMapping("/{id}")
    Map<String, Object> updateUser(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Object> user
    );

    // UPDATE - PATCH
    @PatchMapping("/{id}")
    Map<String, Object> patchUser(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Object> user
    );

    // DELETE
    @DeleteMapping("/{id}")
    void deleteUser(
            @PathVariable("id") Integer id
    );
}