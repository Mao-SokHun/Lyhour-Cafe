package com.example.demo.Controller;

import com.example.demo.Models.CheckoutRequest;
import com.example.demo.Models.Order;
import com.example.demo.Models.User;
import com.example.demo.Repositories.OrderRepository;
import com.example.demo.Repositories.UserRepository;
import com.example.demo.Service.CheckoutService;
import com.example.demo.Service.StripePaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private StripePaymentService stripePaymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> processCheckout(
            @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal User customer,
            HttpSession session) {

        if (customer == null) {
            if (request.getTableId() != null) {
                User walkin = userRepository.findByUsername("walkin");
                if (walkin == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Guest ordering unavailable"));
                }
                customer = walkin;
            } else {
                session.setAttribute("pendingCart", request.getItems());
                session.setAttribute("pendingPaymentMethod", request.getPaymentMethod());
                return ResponseEntity.status(401).body(Map.of("redirectUrl", "/signin"));
            }
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
            @RequestParam(value = "orderId", required = false) Long orderId,
            @RequestParam(value = "qr", required = false) String qr,
            Model model) {

        if (sessionId != null && stripePaymentService.isSessionPaid(sessionId)) {
            try {
                stripePaymentService.handleCheckoutCompleted(sessionId);
            } catch (IllegalArgumentException ignored) {
                // Already processed by webhook
            }
            model.addAttribute("stripePaid", true);
        }

        if (orderId != null) {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null) {
                model.addAttribute("order", order);
                model.addAttribute("qrTableOrder", "1".equals(qr)
                        || order.getSource() == com.example.demo.Models.OrderSource.QR_TABLE);
            }
        }

        return "order-success";
    }
}
