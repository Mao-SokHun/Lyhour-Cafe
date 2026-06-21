package com.example.demo.Service;

import com.example.demo.Models.CartItemDto;
import com.example.demo.Models.FulfillmentType;
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
import com.example.demo.Repositories.RecipeRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private CouponService couponService;

    @Autowired
    private TaxService taxService;

    @Autowired
    private TableService tableService;

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private IngredientInventoryService ingredientInventoryService;

    @Autowired
    private KitchenNotificationService kitchenNotificationService;

    @Autowired
    private RecipeRepository recipeRepository;

    @Value("${app.delivery.fee:3.00}")
    private BigDecimal deliveryFeeAmount;

    @Transactional
    public Order createOrder(User customer, List<CartItemDto> cartItems) {
        return createOrder(customer, cartItems, OrderSource.ONLINE, PaymentMethod.PAY_AT_PICKUP, null, null);
    }

    @Transactional
    public Order createOrder(User customer, List<CartItemDto> cartItems, OrderSource source) {
        PaymentMethod method = source == OrderSource.IN_SHOP ? PaymentMethod.CASH : PaymentMethod.PAY_AT_PICKUP;
        return createOrder(customer, cartItems, source, method, null, null);
    }

    @Transactional
    public Order createOrder(
            User customer,
            List<CartItemDto> cartItems,
            OrderSource source,
            PaymentMethod paymentMethod) {
        return createOrder(customer, cartItems, source, paymentMethod, null, null);
    }

    @Transactional
    public Order createOrder(
            User customer,
            List<CartItemDto> cartItems,
            OrderSource source,
            PaymentMethod paymentMethod,
            String couponCode,
            Long tableId) {
        return createOrderFull(customer, cartItems, source, paymentMethod, couponCode, tableId,
                FulfillmentType.PICKUP, null);
    }

    @Transactional
    public Order createOrderFull(
            User customer,
            List<CartItemDto> cartItems,
            OrderSource source,
            PaymentMethod paymentMethod,
            String couponCode,
            Long tableId,
            FulfillmentType fulfillmentType,
            String deliveryAddress) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setSource(source);
        order.setBranch(reservationService.getDefaultBranch());
        order.setFulfillmentType(fulfillmentType != null ? fulfillmentType : FulfillmentType.PICKUP);
        order.setDeliveryAddress(deliveryAddress);
        if (fulfillmentType == FulfillmentType.DELIVERY) {
            if (deliveryAddress == null || deliveryAddress.isBlank()) {
                throw new IllegalArgumentException("Delivery address required");
            }
            order.setDeliveryFee(deliveryFeeAmount);
        }
        tableService.assignToOrder(tableId, order);

        BigDecimal subtotal = BigDecimal.ZERO;
        int prepMinutes = 0;

        for (CartItemDto itemDto : cartItems) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemDto.getProductId()));

            if (!product.isAvailable()) {
                throw new IllegalStateException(product.getName() + " is unavailable");
            }

            inventoryService.decreaseStock(product.getId(), itemDto.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setSize(itemDto.getSize());
            orderItem.setNote(itemDto.getNote());
            orderItem.setPriceAtTimeOfOrder(itemDto.getPrice());

            order.getOrderItems().add(orderItem);
            subtotal = subtotal.add(itemDto.getPrice().multiply(new BigDecimal(itemDto.getQuantity())));
            prepMinutes += recipeRepository.findByProductId(product.getId())
                    .map(r -> r.getPrepMinutes()).orElse(5) * itemDto.getQuantity();
        }

        BigDecimal couponDiscount = couponService.calculateDiscount(couponCode, subtotal);
        BigDecimal promoDiscount = promotionService.calculatePromotionDiscount(subtotal);
        BigDecimal discount = couponDiscount.add(promoDiscount).min(subtotal);
        BigDecimal taxable = subtotal.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal tax = taxService.calculateTax(taxable);
        BigDecimal deliveryFee = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal total = taxable.add(tax).add(deliveryFee);

        order.setSubtotal(subtotal);
        order.setDiscountAmount(discount);
        order.setTaxAmount(tax);
        order.setTotalPrice(total);
        order.setEstimatedPrepMinutes(Math.max(prepMinutes, 5));
        order.setCouponCode(couponCode != null && !couponCode.isBlank() ? couponCode.trim().toUpperCase() : null);

        paymentService.applyPayment(order, paymentMethod);

        Order saved = orderRepository.save(order);

        try {
            ingredientInventoryService.deductForOrder(saved);
        } catch (IllegalStateException e) {
            restoreOrderStock(saved);
            tableService.releaseTable(saved);
            throw e;
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            finalizePaidOrder(saved);
        }

        if (shouldNotifyImmediately(paymentMethod)) {
            notificationService.sendOrderConfirmation(saved);
        }

        kitchenNotificationService.notifyNewOrder(saved.getId());
        notificationService.notifyStaffOrder(saved);
        notificationService.sendPushToUser(
                saved.getCustomer(),
                "Order #" + saved.getId(),
                "Received — $" + saved.getTotalPrice());
        auditLogService.log("ORDER_CREATE", "Order #" + saved.getId() + " total $" + saved.getTotalPrice());
        return saved;
    }

    private void finalizePaidOrder(Order order) {
        int points = loyaltyService.calculatePointsEarned(order.getTotalPrice());
        order.setLoyaltyPointsEarned(points);
        loyaltyService.awardPoints(order.getCustomer(), points);
        loyaltyService.recordSpending(order.getCustomer(), order.getTotalPrice());
        if (order.getCouponCode() != null) {
            couponService.markUsed(order.getCouponCode());
        }
        orderRepository.save(order);
    }

    private boolean shouldNotifyImmediately(PaymentMethod paymentMethod) {
        return paymentMethod != PaymentMethod.STRIPE;
    }

    @Transactional
    public void updateGuestDisplayName(Order order, String guestName) {
        order.setUsername(guestName);
        orderRepository.save(order);
    }

    @Transactional
    public Order updateStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.setStatus(status);
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            tableService.releaseTable(order);
        }
        Order saved = orderRepository.save(order);
        auditLogService.log("ORDER_STATUS", "Order #" + orderId + " → " + status);
        notificationService.notifyCustomerOrderStatus(saved);
        return saved;
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

    @Transactional(readOnly = true)
    public List<Order> findKitchenOrders() {
        List<Order> orders = orderRepository.findByStatusInOrderByOrderDateAsc(
                List.of(OrderStatus.PENDING, OrderStatus.PREPARING, OrderStatus.READY));
        orders.forEach(o -> o.getOrderItems().forEach(i -> i.getProduct().getId()));
        return orders;
    }

    @Transactional
    public void completeStripePayment(String sessionId) {
        Order order = orderRepository.findByPaymentReference(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for session: " + sessionId));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        finalizePaidOrder(order);
        orderRepository.save(order);
        notificationService.sendOrderConfirmation(order);
        auditLogService.log("STRIPE_PAID", "Order #" + order.getId());
    }

    @Transactional
    public void markOnlinePaymentPaid(Long orderId, String paymentReference) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.setPaymentReference(paymentReference);
        order.setPaymentStatus(PaymentStatus.PAID);
        finalizePaidOrder(order);
        orderRepository.save(order);
        notificationService.sendOrderConfirmation(order);
    }

    @Transactional
    public void setPaymentPendingReference(Long orderId, String paymentReference, PaymentMethod method) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.setPaymentMethod(method);
        order.setPaymentReference(paymentReference);
        order.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.save(order);
    }

    @Transactional
    public void cancelStripePayment(String sessionId) {
        Order order = orderRepository.findByPaymentReference(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for session: " + sessionId));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }

        restoreOrderStock(order);
        tableService.releaseTable(order);
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
