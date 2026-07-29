package com.jurol.buy01.user.service;

import com.jurol.buy01.common.dto.AuthRequest;
import com.jurol.buy01.common.dto.UserDTO;
import com.jurol.buy01.common.events.UserRegisteredEvent;
import com.jurol.buy01.common.security.JwtUtil;
import com.jurol.buy01.user.kafka.UserEventProducer;
import com.jurol.buy01.user.model.User;
import com.jurol.buy01.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserEventProducer eventProducer;

    @InjectMocks
    private UserService userService;

    private User user;
    private AuthRequest registerRequest;

    @BeforeEach
    void setUp() {
        user = new User("test@example.com", "encodedPassword", "John", "Doe", "CLIENT");
        user.setId("user123");

        registerRequest = new AuthRequest("test@example.com", "password123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setRole("CLIENT");
    }

    @Test
    void register_shouldCreateUser() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        doNothing().when(eventProducer).sendUserRegisteredEvent(any(UserRegisteredEvent.class));

        UserDTO result = userService.register(registerRequest);

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("CLIENT", result.getRole());
        verify(eventProducer).sendUserRegisteredEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void register_shouldThrowForDuplicateEmail() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.register(registerRequest));
    }

    @Test
    void login_shouldReturnToken() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("user123", "test@example.com", "CLIENT")).thenReturn("jwt-token");

        var result = userService.login(new AuthRequest("test@example.com", "password123"));

        assertEquals("jwt-token", result.get("token"));
    }

    @Test
    void login_shouldThrowForInvalidCredentials() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThrows(RuntimeException.class, () ->
                userService.login(new AuthRequest("test@example.com", "wrongpassword")));
    }
}

