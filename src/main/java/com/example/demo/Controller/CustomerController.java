package com.example.demo.Controller;

import com.example.demo.Models.Order;
import com.example.demo.Models.User;
import com.example.demo.Repositories.OrderRepository;
import com.example.demo.Repositories.UserRepository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrderRepository orderRepository;

    // Show Profile
    @GetMapping("/profile")
    public String showProfile(@RequestHeader(value = "Referer", required = false) String referer,
                            Authentication authentication, Model model) {
        String username = authentication.getName(); 
        User user = userRepository.findByUsername(username);
        model.addAttribute("user", user);

        String backUrl = "/index";
        if (referer != null && referer.contains("/cart")) {
            backUrl = "/cart";
        }
        model.addAttribute("backUrl", backUrl);

        return "customer-profile"; 
    }


    // Show Edit Form
    @GetMapping("/profile/edit")
    public String editProfileForm(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        model.addAttribute("user", user);
        return "customer-profile-edit";
    }

    // Handle Update
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("user") User updatedUser, Authentication authentication, Model model) {
        String username = authentication.getName();
        User existingUser = userRepository.findByUsername(username);

        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setEmail(updatedUser.getEmail());
        // existingUser.setRole(updatedUser.getRole());
        // existingUser.setPassword(updatedUser.getPassword());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(updatedUser.getPassword());
            existingUser.setPassword(encodedPassword);
        }

        userRepository.save(existingUser);
        model.addAttribute("user", existingUser);
        model.addAttribute("successMessage", "Profile updated successfully!");
        return "redirect:/customer/profile?success";
    }

    // Show Order History
    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public String showOrderHistory(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        List<Order> orders = orderRepository.findByCustomerOrderByOrderDateDesc(user);
        model.addAttribute("orders", orders);
        return "customer-orders";
    }

    // Show Order Details
    @GetMapping("/orders/{id}")
    @Transactional(readOnly = true)
    public String showOrderDetails(@PathVariable("id") Long orderId, Authentication authentication, Model model) {
        String username = authentication.getName();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid order Id:" + orderId));

        // Security check: Ensure the order belongs to the logged-in user
        if (!order.getCustomer().getUsername().equals(username)) {
            return "redirect:/customer/orders?error=auth";
        }

        model.addAttribute("order", order);
        return "customer-order-details";
    }     
}
