package com.jurol.buy01.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jurol.buy01.common.dto.AuthRequest;
import com.jurol.buy01.common.dto.UserDTO;
import com.jurol.buy01.common.security.JwtUtil;
import com.jurol.buy01.user.kafka.UserEventProducer;
import com.jurol.buy01.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserEventProducer userEventProducer;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_shouldReturnCreatedUser() throws Exception {
        AuthRequest request = new AuthRequest("test@example.com", "password123");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setRole("CLIENT");

        UserDTO userDTO = new UserDTO("user123", "test@example.com", "John", "Doe", "CLIENT", null, Instant.now(), Instant.now());

        when(userService.register(any(AuthRequest.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void login_shouldReturnToken() throws Exception {
        AuthRequest request = new AuthRequest("test@example.com", "password123");

        when(userService.login(any(AuthRequest.class))).thenReturn(Map.of("token", "jwt-token", "role", "CLIENT"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }
}

