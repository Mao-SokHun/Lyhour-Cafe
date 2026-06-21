package com.example.demo.Controller;

import com.example.demo.Service.StripePaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CartController {

    private final StripePaymentService stripePaymentService;

    public CartController(StripePaymentService stripePaymentService) {
        this.stripePaymentService = stripePaymentService;
    }

    @GetMapping("/cart")
    public String showCart(Model model) {
        model.addAttribute("stripeEnabled", stripePaymentService.isEnabled());
        return "cart";
    }
}
