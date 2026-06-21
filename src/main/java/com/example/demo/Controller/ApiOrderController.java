package com.example.demo.Controller;

import com.example.demo.Models.Order;
import com.example.demo.Models.CheckoutRequest;
import com.example.demo.Models.PaymentMethod;
import com.example.demo.Models.User;
import com.example.demo.Service.CheckoutService;
import com.example.demo.Service.OrderService;
import com.example.demo.dto.CheckoutResponse;
import com.example.demo.dto.OrderResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class ApiOrderController {

    private final OrderService orderService;
    private final CheckoutService checkoutService;

    public ApiOrderController(OrderService orderService, CheckoutService checkoutService) {
        this.orderService = orderService;
        this.checkoutService = checkoutService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> list(@AuthenticationPrincipal User customer) {
        if (customer == null) {
            return ResponseEntity.status(401).build();
        }
        List<OrderResponse> orders = orderService.findOrdersForCustomer(customer).stream()
                .map(OrderResponse::from)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal User customer) {
        if (customer == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            Order order = orderService.findOrderForCustomer(id, customer);
            return ResponseEntity.ok(OrderResponse.from(order));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal User customer) {
        if (customer == null) {
            return ResponseEntity.status(401).build();
        }

        PaymentMethod method = request.getPaymentMethod() != null
                ? request.getPaymentMethod()
                : PaymentMethod.PAY_AT_PICKUP;

        try {
            if (method == PaymentMethod.STRIPE) {
                CheckoutService.CheckoutResult result = checkoutService.checkout(customer, request);
                return ResponseEntity.ok(new CheckoutResponse(
                        OrderResponse.from(result.order()),
                        result.redirectUrl()));
            }
            var order = orderService.createOrder(
                    customer, request.getItems(),
                    com.example.demo.Models.OrderSource.ONLINE, method);
            return ResponseEntity.ok(OrderResponse.from(order));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
