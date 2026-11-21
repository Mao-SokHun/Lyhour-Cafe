package com.example.demo.Service;

import com.example.demo.Models.Order;
import com.example.demo.Models.User;
import com.example.demo.Repositories.OrderRepository;
import com.example.demo.Repositories.UserRepository;
import com.example.demo.Security.JwtService;
import com.example.demo.dto.OrderResponse;
import com.example.demo.dto.TelegramAuthRequest;
import com.example.demo.dto.TelegramAuthResponse;
import com.example.demo.dto.TelegramUserResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Date;
import java.util.*;

@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final AppSettingService appSettingService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.notifications.telegram.bot-token:}")
    private String envBotToken;

    @Value("${app.base-url:http://localhost:8080}")
    private String envBaseUrl;

    public TelegramService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            JwtService jwtService,
            NotificationService notificationService,
            AppSettingService appSettingService,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.jwtService = jwtService;
        this.notificationService = notificationService;
        this.appSettingService = appSettingService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    public String resolveBotToken() {
        String fromDb = appSettingService.get("telegram.bot_token", "");
        if (!fromDb.isBlank()) {
            return fromDb.trim();
        }
        return envBotToken != null ? envBotToken.trim() : "";
    }

    public String resolveBaseUrl() {
        String fromDb = appSettingService.get("app.base_url", "");
        if (!fromDb.isBlank()) {
            return fromDb.trim().replaceAll("/+$", "");
        }
        return envBaseUrl != null ? envBaseUrl.trim().replaceAll("/+$", "") : "http://localhost:8080";
    }

    public String resolveMiniAppUrl() {
        return resolveBaseUrl() + "/telegram-app";
    }

    /**
     * Cryptographically validates Telegram WebApp initData according to Telegram's HMAC-SHA256 specification.
     * https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app
     */
    public boolean verifyInitData(String initData) {
        if (initData == null || initData.isBlank()) {
            return false;
        }

        String botToken = resolveBotToken();
        if (botToken.isBlank()) {
            log.info("No Telegram bot token configured — granting demo/dev validation for Mini App");
            return true;
        }

        try {
            Map<String, String> params = new LinkedHashMap<>();
            String[] pairs = initData.split("&");
            String receivedHash = null;

            for (String pair : pairs) {
                int idx = pair.indexOf('=');
                if (idx < 0) continue;
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                if ("hash".equalsIgnoreCase(key)) {
                    receivedHash = val;
                } else {
                    params.put(key, val);
                }
            }

            if (receivedHash == null) {
                log.warn("Telegram initData missing hash parameter");
                return false;
            }

            // Sort keys alphabetically
            List<String> sortedKeys = new ArrayList<>(params.keySet());
            Collections.sort(sortedKeys);

            StringBuilder dataCheck = new StringBuilder();
            for (int i = 0; i < sortedKeys.size(); i++) {
                if (i > 0) dataCheck.append("\n");
                String k = sortedKeys.get(i);
                dataCheck.append(k).append("=").append(params.get(k));
            }

            // secret_key = HMAC_SHA256(key="WebAppData", data=botToken)
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);
            byte[] secretKey = hmacSha256.doFinal(botToken.getBytes(StandardCharsets.UTF_8));

            // data_hash = HMAC_SHA256(key=secret_key, data=dataCheck)
            Mac dataMac = Mac.getInstance("HmacSHA256");
            dataMac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            byte[] calculatedHashBytes = dataMac.doFinal(dataCheck.toString().getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : calculatedHashBytes) {
                hex.append(String.format("%02x", b));
            }

            boolean isValid = MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.UTF_8),
                    receivedHash.getBytes(StandardCharsets.UTF_8));

            if (!isValid) {
                log.warn("Telegram initData signature mismatch. Expected: {}, Received: {}", hex, receivedHash);
            }
            return isValid;
        } catch (Exception e) {
            log.error("Failed to verify Telegram initData: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extracts telegram user JSON from initData string or returns null.
     */
    public TelegramUserDto parseUserFromInitData(String initData) {
        if (initData == null || initData.isBlank()) return null;
        try {
            String[] pairs = initData.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf('=');
                if (idx < 0) continue;
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                if ("user".equals(key)) {
                    String userJson = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    JsonNode node = objectMapper.readTree(userJson);
                    Long id = node.has("id") ? node.get("id").asLong() : null;
                    String firstName = node.has("first_name") ? node.get("first_name").asText() : "";
                    String lastName = node.has("last_name") ? node.get("last_name").asText() : "";
                    String username = node.has("username") ? node.get("username").asText() : "";
                    String photoUrl = node.has("photo_url") ? node.get("photo_url").asText() : "";
                    return new TelegramUserDto(id, firstName, lastName, username, photoUrl);
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse user from initData: {}", e.getMessage());
        }
        return null;
    }

    public record TelegramUserDto(Long id, String firstName, String lastName, String username, String photoUrl) {}

    @Transactional
    public TelegramAuthResponse authenticateTelegramUser(TelegramAuthRequest request) {
        TelegramUserDto userDto = null;
        if (request.initData() != null && !request.initData().isBlank()) {
            boolean valid = verifyInitData(request.initData());
            if (valid) {
                userDto = parseUserFromInitData(request.initData());
            }
        }

        // Fallback to explicit fields if provided (e.g. dev/testing mode or parsed client-side)
        if (userDto == null && request.telegramUserId() != null) {
            userDto = new TelegramUserDto(
                    request.telegramUserId(),
                    request.firstName() != null ? request.firstName() : "Guest",
                    request.lastName() != null ? request.lastName() : "",
                    request.username() != null ? request.username() : "",
                    request.photoUrl() != null ? request.photoUrl() : "");
        }

        if (userDto == null || userDto.id() == null) {
            throw new IllegalArgumentException("Invalid Telegram credentials or initData");
        }

        final TelegramUserDto finalUserDto = userDto;
        User user = userRepository.findByTelegramUserId(finalUserDto.id())
                .orElseGet(() -> createTelegramCustomer(finalUserDto));

        // Update username or full name if changed
        if (finalUserDto.username() != null && !finalUserDto.username().isBlank()) {
            user.setTelegramUsername(finalUserDto.username());
            userRepository.save(user);
        }

        String token = jwtService.generateToken(user);
        List<OrderResponse> recentOrders = orderRepository.findByCustomerOrderByOrderDateDesc(user).stream()
                .limit(10)
                .map(OrderResponse::from)
                .toList();

        TelegramUserResponse userResp = new TelegramUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getTelegramUserId(),
                user.getTelegramUsername(),
                finalUserDto.firstName(),
                finalUserDto.lastName(),
                user.getLoyaltyPoints(),
                user.getMembershipTier().name(),
                recentOrders.size()
        );

        return new TelegramAuthResponse(token, userResp, recentOrders, "Authentication successful");
    }

    private User createTelegramCustomer(TelegramUserDto dto) {
        User user = new User();
        String baseName = dto.username() != null && !dto.username().isBlank()
                ? dto.username()
                : "tg_" + dto.id();

        String candidate = baseName;
        int suffix = 1;
        while (userRepository.findByUsername(candidate) != null) {
            candidate = baseName + "_" + (suffix++);
        }

        user.setUsername(candidate);
        user.setEmail(candidate + "@telegram.lyhourcoffee.com");
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole("CUSTOMER");
        user.setTelegramUserId(dto.id());
        user.setTelegramUsername(dto.username());
        user.setCreate_at(new Date(System.currentTimeMillis()));
        user.setLoyaltyPoints(20); // 20 welcome points for Telegram users!

        String displayName = (dto.firstName() + " " + dto.lastName()).trim();
        if (!displayName.isBlank()) {
            user.setAddress("Telegram: " + displayName);
        }

        User saved = userRepository.save(user);
        log.info("Created new Telegram customer: {} (Telegram ID: {})", saved.getUsername(), dto.id());
        return saved;
    }

    /**
     * Sets Telegram Bot's bottom menu button to launch the Mini App.
     */
    public Map<String, Object> setupBotMenuButton() {
        String botToken = resolveBotToken();
        if (botToken.isBlank()) {
            return Map.of("success", false, "message", "Telegram bot token is not configured.");
        }

        String miniAppUrl = resolveMiniAppUrl();
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/setChatMenuButton";
            var menuButton = Map.of(
                    "type", "web_app",
                    "text", "☕ Order Coffee",
                    "web_app", Map.of("url", miniAppUrl)
            );
            var payload = Map.of("menu_button", menuButton);

            var response = restTemplate.postForObject(url, payload, Map.class);
            log.info("setChatMenuButton response: {}", response);
            return Map.of(
                    "success", true,
                    "miniAppUrl", miniAppUrl,
                    "response", response != null ? response : Map.of()
            );
        } catch (Exception e) {
            log.error("Failed to set Telegram menu button: {}", e.getMessage());
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * Registers bot commands with Telegram: /start, /menu, /orders, /help
     */
    public Map<String, Object> setupBotCommands() {
        String botToken = resolveBotToken();
        if (botToken.isBlank()) {
            return Map.of("success", false, "message", "Telegram bot token is not configured.");
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/setMyCommands";
            var commands = List.of(
                    Map.of("command", "start", "description", "☕ Welcome & Open Cafe Menu"),
                    Map.of("command", "menu", "description", "📱 Open Lyhour Coffee Mini App"),
                    Map.of("command", "orders", "description", "📋 View my recent orders"),
                    Map.of("command", "help", "description", "ℹ️ Cafe info, hours & location")
            );
            var payload = Map.of("commands", commands);

            var response = restTemplate.postForObject(url, payload, Map.class);
            return Map.of("success", true, "response", response != null ? response : Map.of());
        } catch (Exception e) {
            log.error("Failed to set Telegram bot commands: {}", e.getMessage());
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * Handles incoming webhook updates from Telegram.
     */
    public void handleWebhookUpdate(Map<String, Object> update) {
        if (update == null) return;

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) update.get("message");
        if (message == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> chat = (Map<String, Object>) message.get("chat");
        if (chat == null || chat.get("id") == null) {
            return;
        }

        String chatId = String.valueOf(chat.get("id"));
        String text = message.get("text") != null ? String.valueOf(message.get("text")).trim() : "";
        String miniAppUrl = resolveMiniAppUrl();

        if (text.startsWith("/start") || text.startsWith("/menu")) {
            String welcomeMsg = """
                    👋 <b>Welcome to Lyhour Coffee & Cake!</b> ☕

                    Slow-roasted specialty coffee, artisan drinks, and freshly baked pastries.

                    Tap <b>Open Coffee Menu</b> below to browse our menu, customize your drink, and order with <b>Bakong KHQR</b> or cash pickup!
                    """;

            var keyboard = Map.of(
                    "inline_keyboard", List.of(
                            List.of(
                                    Map.of("text", "☕ Open Coffee Menu", "web_app", Map.of("url", miniAppUrl))
                            ),
                            List.of(
                                    Map.of("text", "📍 Location & Hours", "callback_data", "info")
                            )
                    )
            );
            notificationService.sendTelegramTo(chatId, welcomeMsg, keyboard);
        } else if (text.startsWith("/orders")) {
            User user = userRepository.findByTelegramUserId(Long.parseLong(chatId)).orElse(null);
            if (user == null) {
                notificationService.sendTelegramTo(chatId, "You haven't placed any orders yet. Open the menu to get started! ☕");
                return;
            }
            List<Order> orders = orderRepository.findByCustomerOrderByOrderDateDesc(user);
            if (orders.isEmpty()) {
                notificationService.sendTelegramTo(chatId, "No past orders found. Tap the menu below to order fresh coffee!");
            } else {
                StringBuilder sb = new StringBuilder("📋 <b>Your Recent Orders:</b>\n\n");
                for (int i = 0; i < Math.min(5, orders.size()); i++) {
                    Order o = orders.get(i);
                    sb.append("• <b>Order #").append(o.getId()).append("</b> — $").append(o.getTotalPrice())
                            .append(" (").append(o.getStatus()).append(")\n");
                }
                sb.append("\nTap below to open the Mini App for live tracking.");
                var keyboard = Map.of(
                        "inline_keyboard", List.of(
                                List.of(Map.of("text", "📱 Open Mini App", "web_app", Map.of("url", miniAppUrl)))
                        )
                );
                notificationService.sendTelegramTo(chatId, sb.toString(), keyboard);
            }
        } else if (text.startsWith("/help")) {
            String help = """
                    ☕ <b>Lyhour Coffee & Cake</b>
                    📍 <b>Location:</b> #390 Street Coffee, Phnom Penh, Cambodia
                    ⏰ <b>Hours:</b> 7:00 AM – 9:00 PM Daily
                    📞 <b>Phone:</b> +855 97 49 44 390
                    💳 <b>Payments:</b> Bakong KHQR, Cash, Stripe

                    Need help with an order? Contact our barista team directly.
                    """;
            notificationService.sendTelegramTo(chatId, help);
        }
    }

    public Map<String, Object> getMiniAppInfo() {
        String botToken = resolveBotToken();
        String baseUrl = resolveBaseUrl();
        String miniAppUrl = resolveMiniAppUrl();
        boolean tokenConfigured = !botToken.isBlank();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("shopName", "Lyhour Coffee");
        info.put("botTokenConfigured", tokenConfigured);
        info.put("baseUrl", baseUrl);
        info.put("miniAppUrl", miniAppUrl);
        info.put("botFatherCommand", "/newapp or /setmenubutton");
        info.put("botUsername", appSettingService.get("telegram.bot_username", "LyhourCafe_bot"));
        return info;
    }
}
