package com.example.bank.controller;

import com.example.bank.common.ApiResponse;
import com.example.bank.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Validated
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    // EP-08: Request password reset
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestParam @Email @NotBlank String email,
            HttpServletRequest req) {
        passwordResetService.requestReset(email);
        // Always 200 — don't reveal whether the email exists
        return ResponseEntity.ok(ApiResponse.success(null, req.getHeader("X-Request-Id")));
    }

    // EP-09: Confirm password reset
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestParam @NotBlank String token,
            @RequestParam @NotBlank @Size(min = 6) String newPassword,
            HttpServletRequest req) {
        passwordResetService.resetPassword(token, newPassword);
        return ResponseEntity.ok(ApiResponse.success(null, req.getHeader("X-Request-Id")));
    }
}
