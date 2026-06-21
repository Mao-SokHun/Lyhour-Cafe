package com.example.demo.dto;

public record NotificationStatusResponse(
        boolean telegramEnabled,
        boolean telegramConfigured,
        boolean pushEnabled,
        boolean fcmConfigured,
        boolean webhookConfigured
) {}
