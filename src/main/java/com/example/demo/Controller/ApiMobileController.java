package com.example.demo.Controller;

import com.example.demo.Models.User;
import com.example.demo.Repositories.OrderRepository;
import com.example.demo.Repositories.ProductRepository;
import com.example.demo.Repositories.UserRepository;
import com.example.demo.Service.NotificationService;
import com.example.demo.dto.ProductResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mobile")
public class ApiMobileController {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ApiMobileController(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        var status = notificationService.getStatus();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("shop", "Lyhour Coffee");
        data.put("apiVersion", 1);
        data.put("pushEnabled", status.pushEnabled());
        data.put("fcmConfigured", status.fcmConfigured());
        return data;
    }

    @GetMapping("/home")
    public Map<String, Object> home(@AuthenticationPrincipal User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("shop", "Lyhour Coffee");
        data.put("menu", productRepository.findAll().stream()
                .filter(p -> p.isAvailable())
                .map(p -> new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                        p.getCategory(), p.getImageUrl(), p.getStock(), p.isInStock(),
                        p.getBranch() != null ? p.getBranch().getName() : null))
                .toList());
        if (user != null) {
            data.put("loyaltyPoints", user.getLoyaltyPoints());
            data.put("membershipTier", user.getMembershipTier());
            data.put("orderCount", orderRepository.findByCustomerOrderByOrderDateDesc(user).size());
        }
        return data;
    }

    @PostMapping("/push-token")
    @Transactional
    public Map<String, String> registerPush(@AuthenticationPrincipal User user, @RequestParam String token) {
        if (user == null) {
            return Map.of("status", "unauthorized");
        }
        User fresh = userRepository.findById(user.getId()).orElseThrow();
        fresh.setPushToken(token);
        userRepository.save(fresh);
        notificationService.sendPushToUser(fresh, "Lyhour Coffee", "Push notifications enabled.");
        return Map.of("status", "registered");
    }
}
