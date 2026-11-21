package com.example.demo.Service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class KitchenNotificationService {

    private final Optional<SimpMessagingTemplate> messagingTemplate;

    public KitchenNotificationService(Optional<SimpMessagingTemplate> messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyNewOrder(Long orderId) {
        messagingTemplate.ifPresent(template -> template.convertAndSend("/topic/kitchen/orders", orderId));
    }
}
