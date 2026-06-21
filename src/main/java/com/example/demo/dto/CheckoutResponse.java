package com.example.demo.dto;

public record CheckoutResponse(
        OrderResponse order,
        String redirectUrl
) {}
