package com.example.demo.Service;

import com.example.demo.Models.OrderStatus;
import com.example.demo.Repositories.OrderRepository;
import com.example.demo.Repositories.ProductRepository;
import com.example.demo.Repositories.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TableService tableService;

    public DashboardService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            TableService tableService) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.tableService = tableService;
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

    public BigDecimal getWeekRevenue() {
        return orderRepository.sumTotalPriceByStatusSince(
                OrderStatus.COMPLETED, LocalDate.now().minusDays(6).atStartOfDay());
    }

    public BigDecimal getMonthRevenue() {
        return orderRepository.sumTotalPriceByStatusSince(
                OrderStatus.COMPLETED, LocalDate.now().withDayOfMonth(1).atStartOfDay());
    }

    public BigDecimal getTotalRevenue() {
        return orderRepository.sumTotalPriceByStatusSince(
                OrderStatus.COMPLETED,
                LocalDateTime.of(2000, 1, 1, 0, 0));
    }

    public long getLowStockCount() {
        return productRepository.countByStockLessThanEqual(10);
    }

    public long getAvailableTables() {
        return tableService.countAvailable();
    }

    public List<Object[]> getBestSellingProducts() {
        return orderRepository.findTopSellingProducts(PageRequest.of(0, 5));
    }

    public List<Object[]> getTopCustomers() {
        return orderRepository.findTopCustomers(PageRequest.of(0, 5));
    }

    public List<Object[]> getDailyRevenueChart() {
        return orderRepository.dailyRevenueSince(LocalDate.now().minusDays(6).atStartOfDay());
    }

    private LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }
}
