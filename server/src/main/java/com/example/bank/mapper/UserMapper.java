package com.example.bank.mapper;

import com.example.bank.dto.UserDto;
import com.example.bank.entity.User;

public class UserMapper {

    public static User mapToUser(UserDto userDto) {
        User user = new User();
        user.setId(userDto.getId());
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        // Note: We are not mapping the password from the DTO.
        // Password should be handled separately, for example, when creating a new user.
        return user;
    }

    public static UserDto mapToUserDto(User user) {
        return new UserDto(
                user.getId() != null ? user.getId() : null,
                user.getUsername() != null ? user.getUsername() : null,
                user.getEmail() != null ? user.getEmail() : null
        );
    }
}
