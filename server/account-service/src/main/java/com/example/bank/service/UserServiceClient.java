package com.example.bank.service;

import com.example.bank.dto.User.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserServiceClient {

    @GetMapping("/internal/users/by-username/{username}")
    UserDto getUserByUsername(@PathVariable("username") String username,
                              @RequestHeader("X-Internal-Auth") String internalSecret);
}
