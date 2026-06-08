package com.example.bank.service.impl;

import com.example.bank.dto.Login.LoginDto;
import com.example.bank.dto.Login.LoginResponseDto;
import com.example.bank.dto.User.UserCreateDto;
import com.example.bank.dto.User.UserDto;
import com.example.bank.entity.User;
import com.example.bank.exception.UserException;
import com.example.bank.repository.UserRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userService;

    private User user;
    private UserCreateDto userCreateDto;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, "mySuperSecretKeyForJwtTokenGenerationInTestsWhichMustBeLongEnough");
        user = new User(1L, "testuser", DigestUtils.sha256Hex("password"), "test@example.com", new ArrayList<>());
        userCreateDto = new UserCreateDto("testuser", "password", "test@example.com");
    }

    @Test
    void createUser_success() {
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto result = userService.createUser(userCreateDto);

        assertNotNull(result);
        assertEquals(user.getUsername(), result.getUsername());
    }

    @Test
    void createUser_usernameExists() {
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(user));

        assertThrows(UserException.class, () -> userService.createUser(userCreateDto));
    }

    @Test
    void createUser_emailExists() {
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));

        assertThrows(UserException.class, () -> userService.createUser(userCreateDto));
    }

    @Test
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserDto result = userService.getUserById(1L);
        assertNotNull(result);
        assertEquals(user.getUsername(), result.getUsername());
    }

    @Test
    void getUserById_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> userService.getUserById(1L));
    }

    @Test
    void getAllUsers_success() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        List<UserDto> result = userService.getAllUsers();
        assertEquals(1, result.size());
    }

    @Test
    void updateUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto updateDto = new UserDto(1L, "newuser", "new@example.com", null);
        UserDto result = userService.updateUser(1L, updateDto);

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
    }

    @Test
    void deleteUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).deleteById(1L);
        userService.deleteUser(1L);
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void login_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        LoginDto loginDto = new LoginDto("testuser", "password");

        LoginResponseDto result = userService.login(loginDto);
        assertNotNull(result.getToken());
        assertEquals("testuser", result.getUser().getUsername());
    }

    @Test
    void login_invalidPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        LoginDto loginDto = new LoginDto("testuser", "wrongpassword");

        assertThrows(UserException.class, () -> userService.login(loginDto));
    }
}
