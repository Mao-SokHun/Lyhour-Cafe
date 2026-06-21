package com.example.demo.Controller;

import com.example.demo.Models.CheckoutRequest;
import com.example.demo.Models.User;
import com.example.demo.Service.CheckoutService;
import com.example.demo.Service.StripePaymentService;
import com.example.demo.dto.CheckoutResponse;
import com.example.demo.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/stripe")
public class ApiStripeController {

    private final CheckoutService checkoutService;
    private final StripePaymentService stripePaymentService;

    public ApiStripeController(CheckoutService checkoutService, StripePaymentService stripePaymentService) {
        this.checkoutService = checkoutService;
        this.stripePaymentService = stripePaymentService;
    }

    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("enabled", stripePaymentService.isEnabled());
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal User customer) {
        if (customer == null) {
            return ResponseEntity.status(401).build();
        }
        if (!stripePaymentService.isEnabled()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Stripe is not configured on the server"));
        }

        request.setPaymentMethod(com.example.demo.Models.PaymentMethod.STRIPE);
        try {
            CheckoutService.CheckoutResult result = checkoutService.checkout(customer, request);
            return ResponseEntity.ok(new CheckoutResponse(
                    OrderResponse.from(result.order()),
                    result.redirectUrl()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
