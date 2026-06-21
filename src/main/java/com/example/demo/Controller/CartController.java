package com.example.demo.Controller;

import com.example.demo.Service.KhqrPaymentService;
import com.example.demo.Service.StripePaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CartController {

    private final StripePaymentService stripePaymentService;
    private final KhqrPaymentService khqrPaymentService;

    public CartController(StripePaymentService stripePaymentService, KhqrPaymentService khqrPaymentService) {
        this.stripePaymentService = stripePaymentService;
        this.khqrPaymentService = khqrPaymentService;
    }

    @GetMapping("/cart")
    public String showCart(Model model) {
        model.addAttribute("stripeEnabled", stripePaymentService.isEnabled());
        model.addAttribute("khqrDemo", khqrPaymentService.isDemoMode());
        model.addAttribute("khqrEnabled", khqrPaymentService.isKhqrEnabled());
        model.addAttribute("bakongEnabled", khqrPaymentService.isBakongEnabled());
        model.addAttribute("abaEnabled", khqrPaymentService.isAbaEnabled());
        model.addAttribute("wingEnabled", khqrPaymentService.isWingEnabled());
        return "cart";
    }
}
