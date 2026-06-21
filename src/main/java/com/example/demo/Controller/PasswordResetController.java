package com.example.demo.Controller;

import com.example.demo.Service.PasswordResetService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestReset(@RequestParam String email) {
        passwordResetService.requestReset(email);
        return "redirect:/signin?resetSent";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam String token, org.springframework.ui.Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token, @RequestParam String password) {
        passwordResetService.resetPassword(token, password);
        return "redirect:/signin?resetOk";
    }
}
