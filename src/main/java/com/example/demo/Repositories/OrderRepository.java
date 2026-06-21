package com.example.demo.Repositories;

import com.example.demo.Models.Order;
import com.example.demo.Models.OrderStatus;
import com.example.demo.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerOrderByOrderDateDesc(User customer);

    Optional<Order> findByPaymentReference(String paymentReference);

    long countByStatus(OrderStatus status);

    List<Order> findByStatusOrderByOrderDateAsc(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.status = :status AND o.orderDate >= :since")
    BigDecimal sumTotalPriceByStatusSince(@Param("status") OrderStatus status, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate >= :since")
    long countOrdersSince(@Param("since") LocalDateTime since);
}
