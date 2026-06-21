package com.example.demo.Service;

import com.example.demo.Models.CheckoutRequest;
import com.example.demo.Models.Order;
import com.example.demo.Models.OrderSource;
import com.example.demo.Models.PaymentMethod;
import com.example.demo.Models.User;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final OrderService orderService;
    private final StripePaymentService stripePaymentService;

    public CheckoutService(OrderService orderService, StripePaymentService stripePaymentService) {
        this.orderService = orderService;
        this.stripePaymentService = stripePaymentService;
    }

    public CheckoutResult checkout(User customer, CheckoutRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        PaymentMethod method = request.getPaymentMethod() != null
                ? request.getPaymentMethod()
                : PaymentMethod.PAY_AT_PICKUP;

        Order order = orderService.createOrder(customer, request.getItems(), OrderSource.ONLINE, method);

        if (method == PaymentMethod.STRIPE) {
            String checkoutUrl = stripePaymentService.createCheckoutSession(order);
            return new CheckoutResult(order, checkoutUrl);
        }

        return new CheckoutResult(order, "/order/success");
    }

    public record CheckoutResult(Order order, String redirectUrl) {}
}
