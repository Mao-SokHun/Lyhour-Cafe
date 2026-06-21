package com.example.demo.Models;

import java.util.List;

public class CheckoutRequest {
    private List<CartItemDto> items;
    private PaymentMethod paymentMethod = PaymentMethod.PAY_AT_PICKUP;

    public List<CartItemDto> getItems() { return items; }
    public void setItems(List<CartItemDto> items) { this.items = items; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
}
