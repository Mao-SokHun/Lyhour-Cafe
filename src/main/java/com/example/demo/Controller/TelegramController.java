package com.example.demo.Controller;

import com.example.demo.Models.*;
import com.example.demo.Repositories.BranchRepository;
import com.example.demo.Repositories.CafeTableRepository;
import com.example.demo.Repositories.OrderRepository;
import com.example.demo.Repositories.ProductRepository;
import com.example.demo.Repositories.UserRepository;
import com.example.demo.Service.*;
import com.example.demo.dto.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class TelegramController {

    private final TelegramService telegramService;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final CafeTableRepository tableRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CheckoutService checkoutService;
    private final KhqrPaymentService khqrPaymentService;
    private final StripePaymentService stripePaymentService;
    private final QrCodeService qrCodeService;

    public TelegramController(
            TelegramService telegramService,
            ProductRepository productRepository,
            BranchRepository branchRepository,
            CafeTableRepository tableRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            CheckoutService checkoutService,
            KhqrPaymentService khqrPaymentService,
            StripePaymentService stripePaymentService,
            QrCodeService qrCodeService) {
        this.telegramService = telegramService;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.checkoutService = checkoutService;
        this.khqrPaymentService = khqrPaymentService;
        this.stripePaymentService = stripePaymentService;
        this.qrCodeService = qrCodeService;
    }

    /**
     * Renders the modern, full-screen Telegram Mini App view.
     */
    @GetMapping("/telegram-app")
    public String telegramApp(
            @RequestParam(value = "table", required = false) String tableParam,
            @RequestParam(value = "orderSuccess", required = false) Long orderSuccessId,
            Model model) {

        List<Product> products = productRepository.findAll().stream()
                .filter(Product::isAvailable)
                .toList();

        List<String> categories = products.stream()
                .map(Product::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .toList();

        List<CafeTable> tables = tableRepository.findAllByOrderByTableNumberAsc();
        List<Branch> branches = branchRepository.findByActiveTrue();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("tables", tables);
        model.addAttribute("branches", branches);
        model.addAttribute("selectedTable", tableParam);
        model.addAttribute("orderSuccessId", orderSuccessId);

        model.addAttribute("khqrEnabled", khqrPaymentService.isKhqrEnabled());
        model.addAttribute("bakongEnabled", khqrPaymentService.isBakongEnabled());
        model.addAttribute("khqrDemo", khqrPaymentService.isDemoMode());
        model.addAttribute("stripeEnabled", stripePaymentService.isEnabled());

        var info = telegramService.getMiniAppInfo();
        model.addAttribute("botUsername", info.get("botUsername"));
        model.addAttribute("miniAppUrl", info.get("miniAppUrl"));

        return "telegram-app";
    }

    /**
     * Authenticates or registers a Telegram user using Telegram WebApp initData.
     */
    @PostMapping("/api/v1/telegram/auth")
    @ResponseBody
    public ResponseEntity<?> authenticateTelegramUser(@RequestBody TelegramAuthRequest request) {
        try {
            TelegramAuthResponse response = telegramService.authenticateTelegramUser(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Authentication error: " + e.getMessage()));
        }
    }

    /**
     * Direct checkout endpoint optimized for the Telegram Mini App.
     */
    @PostMapping("/api/v1/telegram/order")
    @ResponseBody
    public ResponseEntity<?> createTelegramOrder(
            @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal User authenticatedUser) {

        if (request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cart is empty"));
        }

        User customer = authenticatedUser;

        // If not authenticated via JWT, find or link by telegramChatId
        if (customer == null && request.getTelegramChatId() != null) {
            customer = userRepository.findByTelegramUserId(request.getTelegramChatId()).orElse(null);
        }

        // Fallback to walkin user for guest Telegram orders
        if (customer == null) {
            customer = userRepository.findByUsername("walkin");
            if (customer == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Guest ordering currently unavailable"));
            }
        }

        request.setSource(OrderSource.TELEGRAM);

        try {
            CheckoutService.CheckoutResult result = checkoutService.checkout(customer, request);
            Order order = result.order();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orderId", order.getId());
            data.put("status", order.getStatus().name());
            data.put("totalPrice", order.getTotalPrice());
            data.put("paymentMethod", order.getPaymentMethod().name());
            data.put("paymentStatus", order.getPaymentStatus().name());
            data.put("paymentReference", order.getPaymentReference());
            data.put("estimatedPrepMinutes", order.getEstimatedPrepMinutes());
            data.put("redirectUrl", result.redirectUrl());

            // If KHQR, generate QR code payload
            if (khqrPaymentService.isOnlineKhMethod(order.getPaymentMethod())) {
                String qrUrl = "/api/v1/telegram/qr?text=" + java.net.URLEncoder.encode(
                        "khqr://lyhour.cafe/pay?order=" + order.getId() + "&amount=" + order.getTotalPrice()
                                + "&ref=" + order.getPaymentReference(), java.nio.charset.StandardCharsets.UTF_8);
                data.put("qrCodeUrl", qrUrl);
                data.put("demoPaid", khqrPaymentService.isDemoMode());
            }

            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Retrieves recent orders for a customer in the Telegram Mini App.
     */
    @GetMapping("/api/v1/telegram/my-orders")
    @ResponseBody
    public ResponseEntity<?> getMyOrders(
            @RequestParam(required = false) Long telegramChatId,
            @AuthenticationPrincipal User user) {

        List<Order> orders;
        if (user != null) {
            orders = orderRepository.findByCustomerOrderByOrderDateDesc(user);
        } else if (telegramChatId != null) {
            orders = orderRepository.findByTelegramChatIdOrderByOrderDateDesc(telegramChatId);
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "User identification required"));
        }

        List<OrderResponse> responses = orders.stream()
                .limit(15)
                .map(OrderResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Live order status tracker endpoint for the Mini App.
     */
    @GetMapping("/api/v1/telegram/order/{id}")
    @ResponseBody
    public ResponseEntity<?> getOrderStatus(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(order -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("id", order.getId());
                    data.put("status", order.getStatus().name());
                    data.put("paymentStatus", order.getPaymentStatus().name());
                    data.put("paymentMethod", order.getPaymentMethod().name());
                    data.put("paymentReference", order.getPaymentReference());
                    data.put("totalPrice", order.getTotalPrice());
                    data.put("estimatedPrepMinutes", order.getEstimatedPrepMinutes());
                    data.put("fulfillmentType", order.getFulfillmentType().name());
                    data.put("tableNumber", order.getCafeTable() != null ? order.getCafeTable().getTableNumber() : null);
                    return ResponseEntity.ok(data);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Generates a QR Code image (PNG) for instant display in Telegram Mini App (e.g. Bakong KHQR).
     */
    @GetMapping("/api/v1/telegram/qr")
    public ResponseEntity<byte[]> generateQr(@RequestParam String text, @RequestParam(defaultValue = "300") int size) {
        try {
            byte[] png = qrCodeService.generatePng(text, Math.min(size, 600));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            return ResponseEntity.ok().headers(headers).body(png);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Webhook receiver for Telegram Bot updates.
     */
    @PostMapping("/api/v1/telegram/webhook")
    @ResponseBody
    public ResponseEntity<String> telegramWebhook(@RequestBody Map<String, Object> update) {
        telegramService.handleWebhookUpdate(update);
        return ResponseEntity.ok("OK");
    }

    /**
     * Admin action to set the Telegram Bot persistent menu button to launch the Mini App.
     */
    @PostMapping("/admin/telegram/setup-menu-button")
    public String setupMenuButton() {
        telegramService.setupBotMenuButton();
        telegramService.setupBotCommands();
        return "redirect:/admin/settings?telegramMiniApp=configured";
    }
}
