package com.example.demo.Service;

import com.example.demo.Models.Order;
import com.example.demo.Models.OrderStatus;
import com.example.demo.Repositories.OrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportExportService {

    private final OrderRepository orderRepository;
    private final DashboardService dashboardService;
    private final AccountingService accountingService;

    public ReportExportService(
            OrderRepository orderRepository,
            DashboardService dashboardService,
            AccountingService accountingService) {
        this.orderRepository = orderRepository;
        this.dashboardService = dashboardService;
        this.accountingService = accountingService;
    }

    public byte[] exportSalesCsv() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter w = new PrintWriter(out, true, StandardCharsets.UTF_8);
        w.println("Order ID,Customer,Date,Total,Status,Payment");
        for (Order order : orderRepository.findAll()) {
            w.printf("%d,%s,%s,%s,%s,%s%n",
                    order.getId(),
                    order.getUsername(),
                    order.getOrderDate(),
                    order.getTotalPrice(),
                    order.getStatus(),
                    order.getPaymentMethod());
        }
        w.flush();
        return out.toByteArray();
    }

    public byte[] exportSummaryCsv() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter w = new PrintWriter(out, true, StandardCharsets.UTF_8);
        w.println("Metric,Value");
        w.println("Today Revenue," + dashboardService.getTodayRevenue());
        w.println("Week Revenue," + dashboardService.getWeekRevenue());
        w.println("Month Revenue," + dashboardService.getMonthRevenue());
        w.println("Month Expenses," + accountingService.getMonthExpenses());
        w.println("Month Profit," + accountingService.getMonthProfit());
        w.println("Pending Orders," + dashboardService.getPendingOrders());
        w.flush();
        return out.toByteArray();
    }

    public List<Object[]> getDailyRevenueChart() {
        return orderRepository.dailyRevenueSince(LocalDate.now().minusDays(6).atStartOfDay());
    }

    public List<Object[]> getTopCustomers() {
        return orderRepository.findTopCustomers(PageRequest.of(0, 10));
    }

    public List<Object[]> getWorstSellers() {
        return orderRepository.findWorstSellingProducts(PageRequest.of(0, 5));
    }
}
