package com.example.demo.Controller;



import com.example.demo.Models.CheckoutRequest;

import com.example.demo.Models.User;

import com.example.demo.Service.CheckoutService;

import com.example.demo.Service.StripePaymentService;



import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;



import jakarta.servlet.http.HttpSession;



import java.util.Map;



@Controller

@RequestMapping("/order")

public class OrderController {



    @Autowired

    private CheckoutService checkoutService;



    @Autowired

    private StripePaymentService stripePaymentService;



    @PostMapping("/checkout")

    public ResponseEntity<Map<String, String>> processCheckout(

            @RequestBody CheckoutRequest request,

            @AuthenticationPrincipal User customer,

            HttpSession session) {

        if (customer == null) {

            session.setAttribute("pendingCart", request.getItems());

            session.setAttribute("pendingPaymentMethod", request.getPaymentMethod());

            return ResponseEntity.status(401).body(Map.of("redirectUrl", "/signin"));

        }

        if (request.getItems() == null || request.getItems().isEmpty()) {

            return ResponseEntity.badRequest().body(Map.of("message", "Cart is empty"));

        }



        try {

            CheckoutService.CheckoutResult result = checkoutService.checkout(customer, request);

            return ResponseEntity.ok(Map.of("redirectUrl", result.redirectUrl()));

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));

        }

    }



    @GetMapping("/success")

    public String orderSuccess(

            @RequestParam(value = "clear", required = false) String clear,

            @RequestParam(value = "session_id", required = false) String sessionId,

            Model model) {

        if (sessionId != null && stripePaymentService.isSessionPaid(sessionId)) {
            try {
                stripePaymentService.handleCheckoutCompleted(sessionId);
            } catch (IllegalArgumentException ignored) {
                // Already processed by webhook
            }
            model.addAttribute("stripePaid", true);
        }

        return "order-success";

    }

}


