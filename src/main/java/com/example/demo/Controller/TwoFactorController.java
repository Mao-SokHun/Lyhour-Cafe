package com.example.demo.Controller;

import com.example.demo.Models.User;
import com.example.demo.Security.TwoFactorAuthenticationFilter;
import com.example.demo.Service.TwoFactorService;
import com.example.demo.Repositories.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final UserRepository userRepository;

    public TwoFactorController(TwoFactorService twoFactorService, UserRepository userRepository) {
        this.twoFactorService = twoFactorService;
        this.userRepository = userRepository;
    }

    @GetMapping("/2fa")
    public String verifyPage(@AuthenticationPrincipal User user) {
        if (user == null || !user.isTwoFactorEnabled()) {
            return "redirect:/index";
        }
        return "verify-2fa";
    }

    @PostMapping("/2fa/verify")
    public String verify(@AuthenticationPrincipal User user, @RequestParam String code, HttpSession session) {
        if (user == null || !twoFactorService.verifyCode(user.getTwoFactorSecret(), code)) {
            return "redirect:/2fa?error";
        }
        TwoFactorAuthenticationFilter.markVerified(session);
        return "redirect:/index";
    }

    @GetMapping("/account/2fa/setup")
    public String setupPage(@AuthenticationPrincipal User user, Model model) throws Exception {
        String secret = twoFactorService.generateSecret();
        model.addAttribute("secret", secret);
        model.addAttribute("qrDataUri", twoFactorService.buildQrDataUri(user.getUsername(), secret));
        model.addAttribute("enabled", user.isTwoFactorEnabled());
        return "setup-2fa";
    }

    @PostMapping("/account/2fa/enable")
    public String enable(@AuthenticationPrincipal User user, @RequestParam String secret, @RequestParam String code) {
        User fresh = userRepository.findById(user.getId()).orElseThrow();
        twoFactorService.enableTwoFactor(fresh, secret, code);
        return "redirect:/customer/profile?2fa=enabled";
    }

    @PostMapping("/account/2fa/disable")
    public String disable(@AuthenticationPrincipal User user) {
        twoFactorService.disableTwoFactor(userRepository.findById(user.getId()).orElseThrow());
        return "redirect:/customer/profile?2fa=disabled";
    }
}
