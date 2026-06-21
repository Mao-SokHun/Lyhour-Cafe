package com.example.demo.Service;

import com.example.demo.Models.OrderStatus;
import com.example.demo.Repositories.OrderRepository;
import com.example.demo.Repositories.ProductRepository;
import com.example.demo.Repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class DashboardService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public DashboardService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            UserRepository userRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    public long getTotalProducts() {
        return productRepository.count();
    }

    public long getTotalCustomers() {
        return userRepository.findAll().stream()
                .filter(user -> "CUSTOMER".equals(user.getRole()))
                .count();
    }

    public long getPendingOrders() {
        return orderRepository.countByStatus(OrderStatus.PENDING);
    }

    public long getPreparingOrders() {
        return orderRepository.countByStatus(OrderStatus.PREPARING);
    }

    public long getTodayOrderCount() {
        return orderRepository.countOrdersSince(startOfToday());
    }

    public BigDecimal getTodayRevenue() {
        return orderRepository.sumTotalPriceByStatusSince(OrderStatus.COMPLETED, startOfToday());
    }

    public BigDecimal getTotalRevenue() {
        return orderRepository.sumTotalPriceByStatusSince(
                OrderStatus.COMPLETED,
                LocalDateTime.of(2000, 1, 1, 0, 0));
    }

    public long getLowStockCount() {
        return productRepository.countByStockLessThanEqual(10);
    }

    private LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }
}
