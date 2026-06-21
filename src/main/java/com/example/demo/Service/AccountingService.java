package com.example.demo.Service;

import com.example.demo.Models.Expense;
import com.example.demo.Models.ExpenseCategory;
import com.example.demo.Models.OrderStatus;
import com.example.demo.Repositories.ExpenseRepository;
import com.example.demo.Repositories.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class AccountingService {

    private final ExpenseRepository expenseRepository;
    private final OrderRepository orderRepository;

    public AccountingService(ExpenseRepository expenseRepository, OrderRepository orderRepository) {
        this.expenseRepository = expenseRepository;
        this.orderRepository = orderRepository;
    }

    public List<Expense> findAllExpenses() {
        return expenseRepository.findAllByOrderByExpenseDateDesc();
    }

    @Transactional
    public Expense saveExpense(Expense expense) {
        return expenseRepository.save(expense);
    }

    public BigDecimal getMonthRevenue() {
        return orderRepository.sumTotalPriceByStatusSince(
                OrderStatus.COMPLETED, LocalDate.now().withDayOfMonth(1).atStartOfDay());
    }

    public BigDecimal getMonthExpenses() {
        return expenseRepository.sumSince(LocalDate.now().withDayOfMonth(1));
    }

    public BigDecimal getMonthProfit() {
        return getMonthRevenue().subtract(getMonthExpenses());
    }

    public BigDecimal getTotalExpenses() {
        return expenseRepository.sumSince(LocalDate.of(2000, 1, 1));
    }
}
