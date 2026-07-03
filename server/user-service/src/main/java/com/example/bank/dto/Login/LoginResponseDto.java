package com.example.bank.dto.Login;

import com.example.bank.dto.User.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {
    private UserDto user;
    /** Short-lived JWT access token (15 minutes). */
    private String token;
    /** Long-lived refresh token (7 days) for silent renewal. */
    private String refreshToken;
}
