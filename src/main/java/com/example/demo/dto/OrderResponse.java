package com.example.demo.dto;

import com.example.demo.Models.Order;
import com.example.demo.Models.OrderItem;
import com.example.demo.Models.PaymentMethod;
import com.example.demo.Models.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String username,
        LocalDateTime orderDate,
        BigDecimal totalPrice,
        String status,
        String source,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        String paymentReference,
        String branchName,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(OrderMapper::toItemResponse)
                .toList();
        String branchName = order.getBranch() != null ? order.getBranch().getName() : null;
        return new OrderResponse(
                order.getId(),
                order.getUsername(),
                order.getOrderDate(),
                order.getTotalPrice(),
                order.getStatus().name(),
                order.getSource().name(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getPaymentReference(),
                branchName,
                items);
    }
}
