package com.example.demo.Service;

import com.example.demo.Models.Order;
import com.example.demo.Models.PaymentMethod;
import com.example.demo.Models.PaymentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class KhqrPaymentService {

    @Value("${app.payments.khqr.enabled:false}")
    private boolean khqrEnabled;

    @Value("${app.payments.khqr.demo-mode:true}")
    private boolean demoMode;

    @Value("${app.payments.bakong.enabled:false}")
    private boolean bakongEnabled;

    @Value("${app.payments.aba.enabled:false}")
    private boolean abaEnabled;

    @Value("${app.payments.wing.enabled:false}")
    private boolean wingEnabled;

    public boolean isKhqrEnabled() { return khqrEnabled || demoMode; }
    public boolean isBakongEnabled() { return bakongEnabled || demoMode; }
    public boolean isAbaEnabled() { return abaEnabled || demoMode; }
    public boolean isWingEnabled() { return wingEnabled || demoMode; }
    public boolean isDemoMode() { return demoMode; }

    public boolean isOnlineKhMethod(PaymentMethod method) {
        return method == PaymentMethod.KHQR || method == PaymentMethod.BAKONG
                || method == PaymentMethod.ABA || method == PaymentMethod.WING
                || method == PaymentMethod.ACLEDA;
    }

    public boolean isMethodEnabled(PaymentMethod method) {
        return switch (method) {
            case KHQR -> khqrEnabled || demoMode;
            case BAKONG -> bakongEnabled || demoMode;
            case ABA -> abaEnabled || demoMode;
            case WING -> wingEnabled || demoMode;
            case ACLEDA -> khqrEnabled || bakongEnabled || demoMode;
            default -> false;
        };
    }

    public KhqrCheckoutResult createPayment(Order order, PaymentMethod method) {
        if (!isMethodEnabled(method)) {
            throw new IllegalStateException(method + " payments are not enabled.");
        }
        String ref = method.name() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String qrPayload = "khqr://lyhour.cafe/pay?order=" + order.getId() + "&amount="
                + order.getTotalPrice() + "&ref=" + ref + "&method=" + method.name();
        String redirectUrl = demoMode
                ? "/order/success?khqr_ref=" + ref + "&demo=1"
                : "/order/khqr-pay?ref=" + ref + "&orderId=" + order.getId() + "&method=" + method.name();
        return new KhqrCheckoutResult(ref, qrPayload, redirectUrl, demoMode);
    }

    public record KhqrCheckoutResult(String reference, String qrPayload, String redirectUrl, boolean demoPaid) {}

    public void applyKhPayment(Order order, PaymentMethod method, KhqrCheckoutResult result) {
        order.setPaymentMethod(method);
        order.setPaymentReference(result.reference());
        order.setPaymentStatus(result.demoPaid() ? PaymentStatus.PAID : PaymentStatus.PENDING);
    }

    public Map<String, Object> status(String reference) {
        return Map.of("reference", reference, "status", "PENDING", "message", "Awaiting KHQR scan confirmation");
    }
}
