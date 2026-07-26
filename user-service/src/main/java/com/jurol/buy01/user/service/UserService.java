package com.jurol.buy01.user.service;

import com.jurol.buy01.common.dto.AuthRequest;
import com.jurol.buy01.common.dto.UserDTO;
import com.jurol.buy01.common.events.UserRegisteredEvent;
import com.jurol.buy01.common.security.JwtUtil;
import com.jurol.buy01.user.kafka.UserEventProducer;
import com.jurol.buy01.user.model.User;
import com.jurol.buy01.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserEventProducer eventProducer;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, UserEventProducer eventProducer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.eventProducer = eventProducer;
    }

    public UserDTO register(AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        String role = request.getRole() != null ? request.getRole().toUpperCase() : "CLIENT";
        if (!role.equals("CLIENT") && !role.equals("SELLER")) {
            throw new RuntimeException("Invalid role. Must be CLIENT or SELLER");
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName(),
                role
        );

        User saved = userRepository.save(user);

        eventProducer.sendUserRegisteredEvent(
                new UserRegisteredEvent(saved.getId(), saved.getEmail(), saved.getRole())
        );

        return toDTO(saved);
    }

    public Map<String, String> login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        return Map.of("token", token, "role", user.getRole());
    }

    public UserDTO getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toDTO(user);
    }

    public UserDTO updateProfile(String userId, UserDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getAvatar() != null && "SELLER".equals(user.getRole())) {
            user.setAvatar(dto.getAvatar());
        }

        User saved = userRepository.save(user);
        return toDTO(saved);
    }

    private UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getAvatar(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

