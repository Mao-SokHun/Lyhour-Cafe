package com.example.demo.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productName,
        int quantity,
        String size,
        String note,
        BigDecimal price
) {}
