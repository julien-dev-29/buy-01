package com.jurol.buy01.user.controller;

import com.jurol.buy01.common.dto.AuthRequest;
import com.jurol.buy01.common.dto.UserDTO;
import com.jurol.buy01.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody AuthRequest request) {
        UserDTO user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody AuthRequest request) {
        Map<String, String> response = userService.login(request);
        return ResponseEntity.ok(response);
    }
}