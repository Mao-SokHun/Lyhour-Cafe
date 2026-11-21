package com.example.demo.dto;

import java.util.List;

public record TelegramAuthResponse(
        String token,
        TelegramUserResponse user,
        List<OrderResponse> recentOrders,
        String message
) {}
