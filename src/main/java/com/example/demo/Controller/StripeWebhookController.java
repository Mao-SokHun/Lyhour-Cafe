package com.example.demo.Controller;

import com.example.demo.Service.StripePaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.Config.StripeProperties;

@RestController
@RequestMapping("/api/v1/stripe")
public class StripeWebhookController {

    private final StripeProperties stripeProperties;
    private final StripePaymentService stripePaymentService;

    public StripeWebhookController(
            StripeProperties stripeProperties,
            StripePaymentService stripePaymentService) {
        this.stripeProperties = stripeProperties;
        this.stripePaymentService = stripePaymentService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {

        if (!stripePaymentService.isEnabled()) {
            return ResponseEntity.badRequest().body("Stripe not enabled");
        }
        if (stripeProperties.getWebhookSecret() == null || stripeProperties.getWebhookSecret().isBlank()) {
            return ResponseEntity.badRequest().body("Webhook secret not configured");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        String type = event.getType();
        if ("checkout.session.completed".equals(type)) {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);
            if (session != null && "paid".equals(session.getPaymentStatus())) {
                stripePaymentService.handleCheckoutCompleted(session.getId());
            }
        } else if ("checkout.session.expired".equals(type)) {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);
            if (session != null) {
                stripePaymentService.handleCheckoutExpired(session.getId());
            }
        }

        return ResponseEntity.ok("ok");
    }
}
