package com.example.bank.service;

import com.example.bank.dto.User.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserServiceClient {
    @GetMapping("/api/users/internal/by-username/{username}")
    UserDto getUserByUsername(@PathVariable("username") String username);
}
