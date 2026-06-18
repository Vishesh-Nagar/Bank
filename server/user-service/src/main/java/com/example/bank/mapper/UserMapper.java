package com.example.bank.mapper;

import com.example.bank.dto.User.UserDto;
import com.example.bank.entity.User;

public class UserMapper {

    public static UserDto mapToUserDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    // mapToUser is intentionally not provided — entities are constructed directly
    // in the service layer to avoid accidentally mapping unintended fields (e.g. passwords).
}
