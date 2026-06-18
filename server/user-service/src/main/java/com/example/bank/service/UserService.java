package com.example.bank.service;

import com.example.bank.dto.Login.LoginDto;
import com.example.bank.dto.Login.LoginResponseDto;
import com.example.bank.dto.User.UserCreateDto;
import com.example.bank.dto.User.UserDto;

import java.util.List;

public interface UserService {
    UserDto createUser(UserCreateDto userCreateDto);

    UserDto getUserById(Long id);

    UserDto getUserByUsername(String username);

    List<UserDto> getAllUsers();

    UserDto updateUser(Long id, UserDto userDto);

    void deleteUser(Long id);

    LoginResponseDto login(LoginDto loginDto);
}
