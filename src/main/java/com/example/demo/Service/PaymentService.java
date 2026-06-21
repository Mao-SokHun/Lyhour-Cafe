package com.example.demo.Service;

import com.example.demo.Models.Order;
import com.example.demo.Models.PaymentMethod;
import com.example.demo.Models.PaymentStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {

    public void applyPayment(Order order, PaymentMethod method) {
        order.setPaymentMethod(method);
        switch (method) {
            case ONLINE_MOCK -> {
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setPaymentReference("MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            }
            case CASH, CARD_AT_SHOP -> {
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setPaymentReference("SHOP-" + System.currentTimeMillis());
            }
            case PAY_AT_PICKUP -> {
                order.setPaymentStatus(PaymentStatus.PENDING);
                order.setPaymentReference(null);
            }
            case STRIPE -> {
                order.setPaymentStatus(PaymentStatus.PENDING);
                order.setPaymentReference(null);
            }
        }
    }
}
