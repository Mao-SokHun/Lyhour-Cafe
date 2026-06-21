package com.example.demo.Service;

import com.example.demo.Config.StripeProperties;
import com.example.demo.Models.Order;
import com.example.demo.Models.OrderItem;
import com.example.demo.Models.OrderStatus;
import com.example.demo.Models.PaymentStatus;
import com.example.demo.Repositories.OrderRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class StripePaymentService {

    private final StripeProperties stripeProperties;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public StripePaymentService(
            StripeProperties stripeProperties,
            OrderRepository orderRepository,
            @Lazy OrderService orderService) {
        this.stripeProperties = stripeProperties;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    public boolean isEnabled() {
        return stripeProperties.isEnabled()
                && stripeProperties.getSecretKey() != null
                && !stripeProperties.getSecretKey().isBlank();
    }

    @Transactional
    public String createCheckoutSession(Order order) {
        if (!isEnabled()) {
            throw new IllegalStateException("Stripe is not configured. Set app.stripe.enabled=true and add your API keys.");
        }

        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(stripeProperties.getSuccessUrl())
                .setCancelUrl(stripeProperties.getCancelUrl())
                .putMetadata("orderId", String.valueOf(order.getId()))
                .putMetadata("username", order.getUsername());

        long totalCents = toStripeAmount(order.getTotalPrice());
        String description = "Order #" + order.getId();
        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            description += " (includes discount)";
        }

        params.addLineItem(
                SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("usd")
                                        .setUnitAmount(totalCents)
                                        .setProductData(
                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                        .setName("Lyhour Coffee — " + description)
                                                        .setDescription(buildItemSummary(order))
                                                        .build())
                                        .build())
                        .build());

        try {
            Session session = Session.create(params.build());
            order.setPaymentReference(session.getId());
            orderRepository.save(order);
            return session.getUrl();
        } catch (StripeException e) {
            throw new IllegalStateException("Could not start Stripe checkout: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void handleCheckoutCompleted(String sessionId) {
        orderService.completeStripePayment(sessionId);
    }

    @Transactional
    public void handleCheckoutExpired(String sessionId) {
        orderService.cancelStripePayment(sessionId);
    }

    public boolean isSessionPaid(String sessionId) {
        if (!isEnabled() || sessionId == null || sessionId.isBlank()) {
            return false;
        }
        try {
            Session session = Session.retrieve(sessionId);
            return "paid".equals(session.getPaymentStatus());
        } catch (StripeException e) {
            return false;
        }
    }

    private long toStripeAmount(BigDecimal dollars) {
        return dollars.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private String buildItemSummary(Order order) {
        StringBuilder sb = new StringBuilder();
        for (OrderItem item : order.getOrderItems()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(item.getQuantity()).append("x ").append(item.getName_product());
        }
        return sb.toString();
    }
}
