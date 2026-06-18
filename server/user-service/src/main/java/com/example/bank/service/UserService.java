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

    LoginResponseDto login(LoginDto loginDto);
}
