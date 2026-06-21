package com.example.demo.Service;

import com.example.demo.Models.Order;
import com.example.demo.Models.Reservation;
import com.example.demo.Models.User;
import com.example.demo.dto.NotificationStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final Optional<JavaMailSender> mailSender;
    private final AppSettingService appSettingService;

    @Value("${app.notifications.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.notifications.email.from:orders@lyhourcoffee.com}")
    private String fromEmail;

    @Value("${app.notifications.sms.enabled:false}")
    private boolean smsEnabled;

    public NotificationService(Optional<JavaMailSender> mailSender, AppSettingService appSettingService) {
        this.mailSender = mailSender;
        this.appSettingService = appSettingService;
    }

    public void sendOrderConfirmation(Order order) {
        String subject = "Order #" + order.getId() + " confirmed — Lyhour Coffee";
        String body = """
                Hi %s,

                Your order #%d has been received.
                Total: $%s
                Payment: %s (%s)
                Status: %s

                Thank you for ordering with Lyhour Coffee!
                """.formatted(
                order.getUsername(),
                order.getId(),
                order.getTotalPrice(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getStatus());

        log.info("ORDER NOTIFICATION: {}", body.replace("\n", " | "));

        if (emailEnabled && mailSender.isPresent() && order.getCustomer().getEmail() != null) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(order.getCustomer().getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.get().send(message);
        }
    }

    public void sendReservationConfirmation(Reservation reservation) {
        String body = """
                Hi %s,

                Your table reservation is %s.
                Date: %s at %s
                Guests: %d

                Lyhour Coffee
                """.formatted(
                reservation.getCustomerName(),
                reservation.getStatus(),
                reservation.getReservationDate(),
                reservation.getReservationTime(),
                reservation.getGuests());

        log.info("RESERVATION NOTIFICATION: {}", body.replace("\n", " | "));

        if (emailEnabled && mailSender.isPresent()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(reservation.getEmail());
            message.setSubject("Reservation — Lyhour Coffee");
            message.setText(body);
            mailSender.get().send(message);
        }

        if (reservation.getPhone() != null && !reservation.getPhone().isBlank()) {
            sendSms(reservation.getPhone(), "Lyhour Coffee: reservation " + reservation.getStatus()
                    + " for " + reservation.getReservationDate() + " at " + reservation.getReservationTime());
        }
    }

    public void sendSms(String phone, String message) {
        log.info("SMS to {}: {}", phone, message);
        if (smsEnabled) {
            log.warn("SMS enabled but no gateway configured yet. Message was logged only.");
        }
    }

    public void sendPasswordReset(User user, String token) {
        String link = "http://localhost:8080/reset-password?token=" + token;
        String body = "Hi " + user.getUsername() + ",\n\nReset your password:\n" + link + "\n\nLink expires in 1 hour.";
        log.info("PASSWORD RESET: {}", body.replace("\n", " | "));
        if (emailEnabled && mailSender.isPresent() && user.getEmail() != null) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Reset your Lyhour Coffee password");
            message.setText(body);
            mailSender.get().send(message);
        }
    }

    public void sendReceiptEmail(Order order) {
        if (!emailEnabled || mailSender.isEmpty() || order.getCustomer().getEmail() == null) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(order.getCustomer().getEmail());
        message.setSubject("Receipt — Order #" + order.getId());
        message.setText("Thank you! Order #" + order.getId() + " total: $" + order.getTotalPrice());
        mailSender.get().send(message);
    }

    @Value("${app.notifications.telegram.enabled:false}")
    private boolean telegramEnabled;

    @Value("${app.notifications.telegram.bot-token:}")
    private String telegramBotToken;

    @Value("${app.notifications.telegram.chat-id:}")
    private String telegramChatId;

    @Value("${app.notifications.push.enabled:false}")
    private boolean pushEnabled;

    @Value("${app.notifications.push.webhook-url:}")
    private String pushWebhookUrl;

    @Value("${app.notifications.push.fcm-server-key:}")
    private String fcmServerKey;

    public NotificationStatusResponse getStatus() {
        String chatId = resolveChatId();
        return new NotificationStatusResponse(
                telegramEnabled,
                !telegramBotToken.isBlank() && !chatId.isBlank(),
                pushEnabled,
                !fcmServerKey.isBlank(),
                !pushWebhookUrl.isBlank());
    }

    public String resolveChatId() {
        String fromDb = appSettingService.get("telegram.chat_id", "");
        if (!fromDb.isBlank()) {
            return fromDb;
        }
        return telegramChatId != null ? telegramChatId : "";
    }

    /**
     * After you send any message to @LyhourCafe_bot, call this to save your chat ID.
     */
    @SuppressWarnings("unchecked")
    public String discoverTelegramChatId() {
        if (telegramBotToken.isBlank()) {
            return null;
        }
        try {
            String url = "https://api.telegram.org/bot" + telegramBotToken + "/getUpdates";
            var response = new org.springframework.web.client.RestTemplate().getForObject(url, java.util.Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
                return null;
            }
            var results = (java.util.List<java.util.Map<String, Object>>) response.get("result");
            if (results == null || results.isEmpty()) {
                return null;
            }
            for (int i = results.size() - 1; i >= 0; i--) {
                var update = results.get(i);
                var message = (java.util.Map<String, Object>) update.get("message");
                if (message == null) {
                    message = (java.util.Map<String, Object>) update.get("channel_post");
                }
                if (message == null) {
                    continue;
                }
                var chat = (java.util.Map<String, Object>) message.get("chat");
                if (chat == null || chat.get("id") == null) {
                    continue;
                }
                String chatId = String.valueOf(chat.get("id"));
                appSettingService.save("telegram.chat_id", chatId);
                log.info("Telegram chat linked: {}", chatId);
                return chatId;
            }
        } catch (Exception e) {
            log.warn("Telegram getUpdates failed: {}", e.getMessage());
        }
        return null;
    }

    public void sendTelegram(String text) {
        log.info("TELEGRAM: {}", text);
        String chatId = resolveChatId();
        if (!telegramEnabled || telegramBotToken.isBlank() || chatId.isBlank()) {
            return;
        }
        try {
            String url = "https://api.telegram.org/bot" + telegramBotToken + "/sendMessage?chat_id="
                    + chatId + "&text=" + java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8);
            new org.springframework.web.client.RestTemplate().getForObject(url, String.class);
        } catch (Exception e) {
            log.warn("Telegram send failed: {}", e.getMessage());
        }
    }

    public void sendPush(String title, String body, String deviceToken) {
        log.info("PUSH [{}] {} → {}", title, body, deviceToken);
        if (!pushEnabled) {
            return;
        }
        if (deviceToken != null && !deviceToken.isBlank() && !fcmServerKey.isBlank()) {
            sendFcm(title, body, deviceToken);
            return;
        }
        if (pushWebhookUrl.isBlank()) {
            return;
        }
        try {
            var payload = java.util.Map.of(
                    "title", title,
                    "body", body,
                    "token", deviceToken != null ? deviceToken : "");
            new org.springframework.web.client.RestTemplate().postForObject(pushWebhookUrl, payload, String.class);
        } catch (Exception e) {
            log.warn("Push webhook failed: {}", e.getMessage());
        }
    }

    public void sendPushToUser(User user, String title, String body) {
        if (user == null || user.getPushToken() == null || user.getPushToken().isBlank()) {
            return;
        }
        sendPush(title, body, user.getPushToken());
    }

    private void sendFcm(String title, String body, String deviceToken) {
        try {
            var headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("Authorization", "key=" + fcmServerKey);
            var notification = java.util.Map.of("title", title, "body", body);
            var payload = java.util.Map.of("to", deviceToken, "notification", notification);
            var entity = new org.springframework.http.HttpEntity<>(payload, headers);
            new org.springframework.web.client.RestTemplate().postForObject(
                    "https://fcm.googleapis.com/fcm/send", entity, String.class);
        } catch (Exception e) {
            log.warn("FCM send failed: {}", e.getMessage());
        }
    }

    public void notifyStaffOrder(Order order) {
        sendTelegram("New order #" + order.getId() + " — $" + order.getTotalPrice() + " (" + order.getPaymentMethod() + ")");
        sendPush("New order", "#" + order.getId() + " $" + order.getTotalPrice(), null);
    }

    public void notifyCustomerOrderStatus(Order order) {
        if (order.getCustomer() == null) {
            return;
        }
        String title = "Order #" + order.getId();
        String body = "Status: " + order.getStatus();
        sendPushToUser(order.getCustomer(), title, body);
    }

    public boolean sendTestTelegram() {
        String chatId = resolveChatId();
        if (!telegramEnabled || telegramBotToken.isBlank() || chatId.isBlank()) {
            return false;
        }
        sendTelegram("Lyhour Coffee test — Telegram is working.");
        return true;
    }

    public boolean sendTestPush() {
        if (!pushEnabled) {
            return false;
        }
        if (!fcmServerKey.isBlank()) {
            log.info("FCM configured — register a mobile push token to receive test pushes.");
        }
        if (!pushWebhookUrl.isBlank()) {
            sendPush("Test push", "Lyhour Coffee notifications are working.", null);
            return true;
        }
        return !fcmServerKey.isBlank();
    }
}
