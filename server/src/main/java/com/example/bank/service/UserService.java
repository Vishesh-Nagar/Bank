package com.example.bank.service;

import com.example.bank.dto.LoginDto;
import com.example.bank.dto.LoginResponseDto;
import com.example.bank.dto.UserCreateDto;
import com.example.bank.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto createUser(UserCreateDto userCreateDto);

    UserDto getUserById(Long id);

    List<UserDto> getAllUsers();

    UserDto updateUser(Long id, UserDto userDto);

    void deleteUser(Long id);

    LoginResponseDto login(LoginDto loginDto);
}
