package com.example.bank.service;

import com.example.bank.dto.Login.LoginDto;
import com.example.bank.dto.Login.LoginResponseDto;
import com.example.bank.dto.User.UserCreateDto;
import com.example.bank.dto.User.UserDto;
import com.example.bank.dto.User.UserUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserDto createUser(UserCreateDto userCreateDto);

    UserDto getUserById(Long id);

    UserDto getUserByUsername(String username);

    Page<UserDto> getAllUsers(Pageable pageable);

    UserDto updateUser(Long id, UserUpdateDto userUpdateDto, String principalName);

    void deleteUser(Long id);

    /** Login with IP tracking for brute-force lockout. */
    LoginResponseDto login(LoginDto loginDto, String clientIp);

    /** Issue a new access token from a valid refresh token. */
    LoginResponseDto refresh(String refreshToken);

    /** Revoke the given refresh token (logout). */
    void logout(String refreshToken);

    /** Admin: lock or unlock a user account. */
    UserDto setAccountLocked(Long id, boolean locked);

    /** Consume email verification token. */
    void verifyEmail(String token);

    /** Resend email verification link (always succeeds silently). */
    void resendVerificationEmail(String email);
}
