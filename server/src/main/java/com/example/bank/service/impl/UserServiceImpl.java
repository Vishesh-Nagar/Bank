package com.example.bank.service.impl;

import com.example.bank.dto.LoginDto;
import com.example.bank.dto.LoginResponseDto;
import com.example.bank.dto.UserCreateDto;
import com.example.bank.dto.UserDto;
import com.example.bank.entity.User;
import com.example.bank.exception.UserException;
import com.example.bank.mapper.UserMapper;
import com.example.bank.repository.UserRepository;
import com.example.bank.service.UserService;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDto createUser(UserCreateDto userCreateDto) {
        userRepository.findByUsername(userCreateDto.getUsername()).ifPresent(u -> {
            throw new UserException("Username already exists");
        });

        userRepository.findByEmail(userCreateDto.getEmail()).ifPresent(u -> {
            throw new UserException("Email already exists");
        });

        User user = new User();
        user.setUsername(userCreateDto.getUsername());
        user.setPassword(DigestUtils.sha256Hex(userCreateDto.getPassword()));
        user.setEmail(userCreateDto.getEmail());

        User savedUser = userRepository.save(user);
        return UserMapper.mapToUserDto(savedUser);
    }

    @Override
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found"));
        return UserMapper.mapToUserDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(UserMapper::mapToUserDto).collect(Collectors.toList());
    }

    @Override
    public UserDto updateUser(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found"));

        if (userDto.getUsername() != null && !userDto.getUsername().equals(user.getUsername())) {
            userRepository.findByUsername(userDto.getUsername()).ifPresent(u -> {
                throw new UserException("Username already exists");
            });
            user.setUsername(userDto.getUsername());
        }

        if (userDto.getEmail() != null && !userDto.getEmail().equals(user.getEmail())) {
            userRepository.findByEmail(userDto.getEmail()).ifPresent(u -> {
                throw new UserException("Email already exists");
            });
            user.setEmail(userDto.getEmail());
        }

        // If updating password, hash it
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            user.setPassword(DigestUtils.sha256Hex(userDto.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        return UserMapper.mapToUserDto(updatedUser);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found"));
        userRepository.deleteById(id);
    }

    @Override
    public LoginResponseDto login(LoginDto loginDto) {
        User user = userRepository.findByUsername(loginDto.getUsername())
                .orElseThrow(() -> new UserException("Invalid username or password"));

        // Compare stored hash with hash of input password
        if (!DigestUtils.sha256Hex(loginDto.getPassword()).equals(user.getPassword())) {
            throw new UserException("Invalid username or password");
        }

        UserDto userDto = UserMapper.mapToUserDto(user);
        return new LoginResponseDto(userDto);
    }
}
