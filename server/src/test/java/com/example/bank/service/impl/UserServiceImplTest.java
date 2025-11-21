package com.example.bank.service.impl;

import com.example.bank.dto.UserCreateDto;
import com.example.bank.dto.UserDto;
import com.example.bank.entity.User;
import com.example.bank.exception.UserException;
import com.example.bank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserCreateDto userCreateDto;

    @BeforeEach
    void setUp() {
        user = new User(1L, "testuser", "password", "test@example.com", new ArrayList<>());
        userCreateDto = new UserCreateDto("testuser", "password", "test@example.com");
    }

    @Test
    void createUser_success() {
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto result = userService.createUser(userCreateDto);

        assertNotNull(result);
        assertEquals(user.getUsername(), result.getUsername());
    }

    @Test
    void createUser_usernameExists() {
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(user));

        assertThrows(UserException.class, () -> {
            userService.createUser(userCreateDto);
        });
    }

    @Test
    void createUser_emailExists() {
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));

        assertThrows(UserException.class, () -> {
            userService.createUser(userCreateDto);
        });
    }
}
