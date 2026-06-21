package com.example.demo.Service;

import com.example.demo.Models.CartItemDto;
import com.example.demo.Models.Order;
import com.example.demo.Models.OrderItem;
import com.example.demo.Models.OrderSource;
import com.example.demo.Models.OrderStatus;
import com.example.demo.Models.PaymentMethod;
import com.example.demo.Models.PaymentStatus;
import com.example.demo.Models.Product;
import com.example.demo.Models.User;
import com.example.demo.Repositories.OrderRepository;
import com.example.demo.Repositories.ProductRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ReservationService reservationService;

    @Transactional
    public Order createOrder(User customer, List<CartItemDto> cartItems) {
        return createOrder(customer, cartItems, OrderSource.ONLINE, PaymentMethod.PAY_AT_PICKUP);
    }

    @Transactional
    public Order createOrder(User customer, List<CartItemDto> cartItems, OrderSource source) {
        PaymentMethod method = source == OrderSource.IN_SHOP ? PaymentMethod.CASH : PaymentMethod.PAY_AT_PICKUP;
        return createOrder(customer, cartItems, source, method);
    }

    @Transactional
    public Order createOrder(
            User customer,
            List<CartItemDto> cartItems,
            OrderSource source,
            PaymentMethod paymentMethod) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setSource(source);
        order.setBranch(reservationService.getDefaultBranch());

        BigDecimal totalOrderPrice = BigDecimal.ZERO;

        for (CartItemDto itemDto : cartItems) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemDto.getProductId()));

            inventoryService.decreaseStock(product.getId(), itemDto.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setSize(itemDto.getSize());
            orderItem.setNote(itemDto.getNote());
            orderItem.setPriceAtTimeOfOrder(itemDto.getPrice());

            order.getOrderItems().add(orderItem);
            totalOrderPrice = totalOrderPrice.add(
                    itemDto.getPrice().multiply(new BigDecimal(itemDto.getQuantity())));
        }

        order.setTotalPrice(totalOrderPrice);
        paymentService.applyPayment(order, paymentMethod);

        Order saved = orderRepository.save(order);
        if (shouldNotifyImmediately(paymentMethod)) {
            notificationService.sendOrderConfirmation(saved);
        }
        return saved;
    }

    private boolean shouldNotifyImmediately(PaymentMethod paymentMethod) {
        return paymentMethod != PaymentMethod.STRIPE;
    }

    @Transactional
    public Order updateStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> findOrdersForCustomer(User customer) {
        List<Order> orders = orderRepository.findByCustomerOrderByOrderDateDesc(customer);
        orders.forEach(order -> order.getOrderItems().forEach(item -> item.getProduct().getId()));
        return orders;
    }

    @Transactional(readOnly = true)
    public Order findOrderForCustomer(Long orderId, User customer) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        order.getOrderItems().forEach(item -> item.getProduct().getId());
        return order;
    }

    @Transactional
    public void completeStripePayment(String sessionId) {
        Order order = orderRepository.findByPaymentReference(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for session: " + sessionId));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);
        notificationService.sendOrderConfirmation(order);
    }

    @Transactional
    public void cancelStripePayment(String sessionId) {
        Order order = orderRepository.findByPaymentReference(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for session: " + sessionId));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }

        restoreOrderStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.FAILED);
        orderRepository.save(order);
    }

    private void restoreOrderStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            inventoryService.restoreStock(item.getProduct().getId(), item.getQuantity());
        }
    }
}
