package com.example.demo.Service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class KitchenNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public KitchenNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyNewOrder(Long orderId) {
        messagingTemplate.convertAndSend("/topic/kitchen/orders", orderId);
    }
}
