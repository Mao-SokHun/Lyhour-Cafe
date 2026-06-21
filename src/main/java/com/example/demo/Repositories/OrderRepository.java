package com.example.demo.Repositories;

import com.example.demo.Models.Order;
import com.example.demo.Models.OrderStatus;
import com.example.demo.Models.User;
import org.springframework.data.domain.Pageable;
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

    List<Order> findByStatusInOrderByOrderDateAsc(List<OrderStatus> statuses);

    @Query("""
            SELECT oi.name_product, SUM(oi.quantity)
            FROM OrderItem oi
            JOIN oi.order o
            WHERE o.status = com.example.demo.Models.OrderStatus.COMPLETED
            GROUP BY oi.name_product
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<Object[]> findTopSellingProducts(Pageable pageable);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.status = :status AND o.orderDate >= :since")
    BigDecimal sumTotalPriceByStatusSince(@Param("status") OrderStatus status, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate >= :since")
    long countOrdersSince(@Param("since") LocalDateTime since);

    @Query("""
            SELECT o.customer.username, SUM(o.totalPrice)
            FROM Order o
            WHERE o.status = com.example.demo.Models.OrderStatus.COMPLETED
            GROUP BY o.customer.username
            ORDER BY SUM(o.totalPrice) DESC
            """)
    List<Object[]> findTopCustomers(org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT FUNCTION('DATE', o.orderDate), COALESCE(SUM(o.totalPrice), 0)
            FROM Order o
            WHERE o.status = com.example.demo.Models.OrderStatus.COMPLETED
              AND o.orderDate >= :since
            GROUP BY FUNCTION('DATE', o.orderDate)
            ORDER BY FUNCTION('DATE', o.orderDate)
            """)
    List<Object[]> dailyRevenueSince(@Param("since") LocalDateTime since);

    @Query("""
            SELECT oi.name_product, SUM(oi.quantity)
            FROM OrderItem oi
            JOIN oi.order o
            WHERE o.status = com.example.demo.Models.OrderStatus.COMPLETED
            GROUP BY oi.name_product
            ORDER BY SUM(oi.quantity) ASC
            """)
    List<Object[]> findWorstSellingProducts(org.springframework.data.domain.Pageable pageable);
}
