package com.example.bank.controller;

import com.example.bank.dto.User.UserDto;
import com.example.bank.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserDto> getUserByUsernameInternal(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<UserDto> getUserByIdInternal(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}
