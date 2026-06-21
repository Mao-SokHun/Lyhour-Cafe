package com.example.demo.Service;

import com.example.demo.Models.CheckoutRequest;
import com.example.demo.Models.Order;
import com.example.demo.Models.OrderSource;
import com.example.demo.Models.PaymentMethod;
import com.example.demo.Models.User;
import com.example.demo.Models.FulfillmentType;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final OrderService orderService;
    private final StripePaymentService stripePaymentService;
    private final KhqrPaymentService khqrPaymentService;

    public CheckoutService(
            OrderService orderService,
            StripePaymentService stripePaymentService,
            KhqrPaymentService khqrPaymentService) {
        this.orderService = orderService;
        this.stripePaymentService = stripePaymentService;
        this.khqrPaymentService = khqrPaymentService;
    }

    public CheckoutResult checkout(User customer, CheckoutRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        PaymentMethod method = request.getPaymentMethod() != null
                ? request.getPaymentMethod()
                : PaymentMethod.PAY_AT_PICKUP;

        OrderSource source = request.getTableId() != null ? OrderSource.QR_TABLE : OrderSource.ONLINE;
        FulfillmentType fulfillment = request.getFulfillmentType();
        if (request.getTableId() != null && (fulfillment == null || fulfillment == FulfillmentType.PICKUP)) {
            fulfillment = FulfillmentType.DINE_IN;
        }

        Order order = orderService.createOrderFull(
                customer,
                request.getItems(),
                source,
                method,
                request.getCouponCode(),
                request.getTableId(),
                fulfillment,
                request.getDeliveryAddress());

        if (request.getGuestName() != null && !request.getGuestName().isBlank()) {
            orderService.updateGuestDisplayName(order, request.getGuestName().trim());
        }

        if (method == PaymentMethod.STRIPE) {
            String checkoutUrl = stripePaymentService.createCheckoutSession(order);
            return new CheckoutResult(order, checkoutUrl);
        }

        if (khqrPaymentService.isOnlineKhMethod(method)) {
            KhqrPaymentService.KhqrCheckoutResult kh = khqrPaymentService.createPayment(order, method);
            if (kh.demoPaid()) {
                orderService.markOnlinePaymentPaid(order.getId(), kh.reference());
            } else {
                orderService.setPaymentPendingReference(order.getId(), kh.reference(), method);
            }
            return new CheckoutResult(order, kh.redirectUrl());
        }

        String successUrl = "/order/success?orderId=" + order.getId();
        if (source == OrderSource.QR_TABLE) {
            successUrl += "&qr=1";
        }
        return new CheckoutResult(order, successUrl);
    }

    public record CheckoutResult(Order order, String redirectUrl) {}
}
