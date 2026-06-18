package com.example.bank.controller;

import com.example.bank.common.ApiResponse;
import com.example.bank.common.PaginationMeta;
import com.example.bank.dto.Login.LoginDto;
import com.example.bank.dto.Login.LoginResponseDto;
import com.example.bank.dto.User.UserCreateDto;
import com.example.bank.dto.User.UserDto;
import com.example.bank.dto.User.UserUpdateDto;
import com.example.bank.enums.ErrorCode;
import com.example.bank.exception.UserException;
import com.example.bank.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // EP-01: Register
    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody UserCreateDto dto, HttpServletRequest req) {
        UserDto created = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, req.getHeader("X-Request-Id")));
    }

    // EP-02: Login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Valid @RequestBody LoginDto dto, HttpServletRequest req) {
        LoginResponseDto response = userService.login(dto);
        return ResponseEntity.ok(ApiResponse.success(response, req.getHeader("X-Request-Id")));
    }

    // EP-03: Get current user (by principal)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getMe(Principal principal, HttpServletRequest req) {
        UserDto user = userService.getUserByUsername(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(user, req.getHeader("X-Request-Id")));
    }

    // EP-04: Get user by ID (self or ADMIN)
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(
            @PathVariable Long id, Principal principal, HttpServletRequest req) {
        UserDto user = userService.getUserById(id);
        // Enforce ownership — only self or ADMIN
        UserDto currentUser = userService.getUserByUsername(principal.getName());
        if (!currentUser.getId().equals(id) && !"ADMIN".equals(currentUser.getRole())) {
            throw new UserException(ErrorCode.ACCESS_DENIED,
                    "You do not have permission to view this user.");
        }
        return ResponseEntity.ok(ApiResponse.success(user, req.getHeader("X-Request-Id")));
    }

    // EP-05: Update user (self only)
    @PutMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id, @Valid @RequestBody UserUpdateDto dto,
            Principal principal, HttpServletRequest req) {
        UserDto updated = userService.updateUser(id, dto, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(updated, req.getHeader("X-Request-Id")));
    }

    // EP-06: Delete user (self only)
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id, Principal principal, HttpServletRequest req) {
        UserDto currentUser = userService.getUserByUsername(principal.getName());
        if (!currentUser.getId().equals(id) && !"ADMIN".equals(currentUser.getRole())) {
            throw new UserException(ErrorCode.ACCESS_DENIED,
                    "You do not have permission to delete this user.");
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build(); // 204
    }

    // EP-07: List all users — ADMIN only
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest req) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<UserDto> result = userService.getAllUsers(pageable);
        PaginationMeta pagination = new PaginationMeta(
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.hasNext(), result.hasPrevious());
        return ResponseEntity.ok(ApiResponse.successPaginated(result, req.getHeader("X-Request-Id"), pagination));
    }


}