package com.example.demo.dto;

public record TelegramAuthRequest(
        String initData,
        Long telegramUserId,
        String username,
        String firstName,
        String lastName,
        String photoUrl
) {}
