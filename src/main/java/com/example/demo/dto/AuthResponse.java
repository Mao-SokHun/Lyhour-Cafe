package com.example.demo.dto;

public record AuthResponse(
        String token,
        String type,
        Long userId,
        String username,
        String email,
        String role
) {
    public AuthResponse(String token, Long userId, String username, String email, String role) {
        this(token, "Bearer", userId, username, email, role);
    }
}
