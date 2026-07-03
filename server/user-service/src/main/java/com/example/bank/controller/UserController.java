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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ─── Public endpoints ─────────────────────────────────────────────────────

    // EP-01: Register
    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody UserCreateDto dto, HttpServletRequest req) {
        UserDto created = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, req.getHeader("X-Request-Id")));
    }

    // EP-02: Login — extracts client IP for brute-force lockout
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Valid @RequestBody LoginDto dto, HttpServletRequest req) {
        String ip = Optional.ofNullable(req.getHeader("X-Forwarded-For"))
                .map(s -> s.split(",")[0].trim())
                .orElse(req.getRemoteAddr());
        LoginResponseDto response = userService.login(dto, ip);
        return ResponseEntity.ok(ApiResponse.success(response, req.getHeader("X-Request-Id")));
    }


    // EP-10: Verify email
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam @NotBlank String token, HttpServletRequest req) {
        userService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(null, req.getHeader("X-Request-Id")));
    }

    // EP-11: Resend verification email (always 200)
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestParam @Email @NotBlank String email, HttpServletRequest req) {
        userService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success(null, req.getHeader("X-Request-Id")));
    }

    // EP-12: Refresh access token
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponseDto>> refresh(
            @RequestParam @NotBlank String refreshToken, HttpServletRequest req) {
        LoginResponseDto response = userService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response, req.getHeader("X-Request-Id")));
    }

    // ─── Authenticated endpoints ──────────────────────────────────────────────

    // EP-13: Logout — revokes refresh token
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestParam @NotBlank String refreshToken, HttpServletRequest req) {
        userService.logout(refreshToken);
        return ResponseEntity.noContent().build(); // 204
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

    // EP-06: Delete user (self or ADMIN)
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
        return ResponseEntity.ok(ApiResponse.successPaginated(result,
                req.getHeader("X-Request-Id"), pagination));
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────────

    // EP-14: Lock / unlock user — ADMIN only
    @PatchMapping("/{id:\\d+}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> setAccountLock(
            @PathVariable Long id,
            @RequestParam boolean locked,
            HttpServletRequest req) {
        UserDto updated = userService.setAccountLocked(id, locked);
        return ResponseEntity.ok(ApiResponse.success(updated, req.getHeader("X-Request-Id")));
    }
}