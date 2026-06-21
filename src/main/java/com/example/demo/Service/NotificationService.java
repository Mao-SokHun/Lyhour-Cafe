package com.example.demo.Service;

import com.example.demo.Models.Order;
import com.example.demo.Models.Reservation;
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

    @Value("${app.notifications.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.notifications.email.from:orders@lyhourcoffee.com}")
    private String fromEmail;

    @Value("${app.notifications.sms.enabled:false}")
    private boolean smsEnabled;

    public NotificationService(Optional<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
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
            // Hook for Twilio or local SMS gateway — configure when ready
            log.warn("SMS enabled but no gateway configured yet. Message was logged only.");
        }
    }
}
