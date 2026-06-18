package com.example.bank.controller;

import com.example.bank.dto.Login.LoginDto;
import com.example.bank.dto.Login.LoginResponseDto;
import com.example.bank.dto.User.UserCreateDto;
import com.example.bank.dto.User.UserDto;
import com.example.bank.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Create User
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateDto userCreateDto) {
        UserDto createdUser = userService.createUser(userCreateDto);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    // Get User by ID
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserDto userDto = userService.getUserById(id);
        return ResponseEntity.ok(userDto);
    }

    // Get All Users
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // Update User
    @PutMapping("/{id:\\d+}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody UserDto userDto) {
        UserDto updatedUser = userService.updateUser(id, userDto);
        return ResponseEntity.ok(updatedUser);
    }

    // Delete User
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginDto loginDto) {
        try {
            // Use only your custom service—NO Spring Security authentication!
            LoginResponseDto loginResponse = userService.login(loginDto);
            return ResponseEntity.ok(loginResponse);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    // Internal endpoint - API Gateway strips /internal/** from public routes
    @GetMapping("/internal/by-username/{username}")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @GetMapping("/internal/{id}")
    public ResponseEntity<UserDto> getUserByIdInternal(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}