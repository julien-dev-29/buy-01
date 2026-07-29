package com.jurol.buy01.user.controller;

import com.jurol.buy01.common.dto.UserDTO;
import com.jurol.buy01.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getProfile(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        UserDTO user = userService.getProfile(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateProfile(Authentication authentication, @RequestBody UserDTO dto) {
        String userId = (String) authentication.getPrincipal();
        UserDTO updated = userService.updateProfile(userId, dto);
        return ResponseEntity.ok(updated);
    }
}