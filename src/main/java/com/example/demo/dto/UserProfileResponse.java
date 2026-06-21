package com.example.demo.dto;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String role
) {}
