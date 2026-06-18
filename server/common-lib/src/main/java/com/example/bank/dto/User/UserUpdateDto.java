package com.example.bank.dto.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDto {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Email(message = "Invalid email format")
    private String email;

    // Required only when newPassword is provided
    private String currentPassword;

    @Size(min = 6, max = 128, message = "Password must be at least 6 characters")
    private String newPassword;
}
