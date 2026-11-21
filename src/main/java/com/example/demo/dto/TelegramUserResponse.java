package com.example.demo.dto;

public record TelegramUserResponse(
        Long id,
        String username,
        String email,
        String role,
        Long telegramUserId,
        String telegramUsername,
        String firstName,
        String lastName,
        int loyaltyPoints,
        String membershipTier,
        int orderCount
) {}
