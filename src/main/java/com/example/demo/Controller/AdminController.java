package com.example.demo.Controller;

import com.example.demo.Models.CartItemDto;
import com.example.demo.Models.Order;
import com.example.demo.Models.OrderSource;
import com.example.demo.Models.OrderStatus;
import com.example.demo.Models.Product;
import com.example.demo.Models.User;
import com.example.demo.Repositories.OrderRepository;
import com.example.demo.Repositories.ProductRepository;
import com.example.demo.Repositories.UserRepository;
import com.example.demo.Models.ReservationStatus;
import com.example.demo.Service.DashboardService;
import com.example.demo.Service.OrderService;
import com.example.demo.Service.ReservationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrderService orderService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ReservationService reservationService;

    @GetMapping("/profile")
    public String showProfile(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        model.addAttribute("user", user);
        return "admin-profile";
    }

    @GetMapping("/profile/edit")
    public String editProfileForm(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        model.addAttribute("user", user);
        return "admin-profile-edit";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("user") User updatedUser, Authentication authentication) {
        String username = authentication.getName();
        User existingUser = userRepository.findByUsername(username);

        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setEmail(updatedUser.getEmail());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        userRepository.save(existingUser);
        return "redirect:/admin/profile?success";
    }

    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public String listAllOrders(@RequestParam(value = "status", required = false) String status, Model model) {
        List<Order> orders;
        if (status != null && !status.isBlank()) {
            orders = orderRepository.findByStatusOrderByOrderDateAsc(OrderStatus.valueOf(status.toUpperCase()));
            model.addAttribute("filterStatus", status.toUpperCase());
        } else {
            orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate"));
        }
        model.addAttribute("orders", orders);
        model.addAttribute("statuses", OrderStatus.values());
        return "list-order";
    }

    @GetMapping("/orders/{id}")
    @Transactional(readOnly = true)
    public String viewOrderDetails(@PathVariable("id") Long orderId, Model model) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid order Id:" + orderId));
        model.addAttribute("order", order);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("isAdminView", true);
        return "customer-order-details";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(
            @PathVariable("id") Long orderId,
            @RequestParam("status") OrderStatus status) {
        orderService.updateStatus(orderId, status);
        return "redirect:/admin/orders/" + orderId + "?updated";
    }

    @GetMapping("/pos")
    public String pointOfSale(Model model) {
        List<Product> products = productRepository.findAll();
        List<User> customers = userRepository.findAll().stream()
                .filter(user -> "CUSTOMER".equals(user.getRole()))
                .toList();
        model.addAttribute("products", products);
        model.addAttribute("customers", customers);
        return "pos";
    }

    @PostMapping("/pos/checkout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> posCheckout(
            @RequestBody List<CartItemDto> cartItems,
            @RequestParam(value = "customerId", required = false) Long customerId) {
        if (cartItems == null || cartItems.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cart is empty"));
        }

        User customer = customerId != null
                ? userRepository.findById(customerId).orElse(null)
                : userRepository.findByUsername("walkin");

        if (customer == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Customer not found"));
        }

        try {
            Order order = orderService.createOrder(customer, cartItems, OrderSource.IN_SHOP);
            return ResponseEntity.ok(Map.of(
                    "message", "Sale completed",
                    "orderId", order.getId(),
                    "redirectUrl", "/admin/orders/" + order.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/reservations")
    @Transactional(readOnly = true)
    public String listReservations(Model model) {
        model.addAttribute("reservations", reservationService.findAll());
        model.addAttribute("statuses", ReservationStatus.values());
        return "list-reservations";
    }

    @PostMapping("/reservations/{id}/status")
    public String updateReservationStatus(
            @PathVariable Long id,
            @RequestParam ReservationStatus status) {
        reservationService.updateStatus(id, status);
        return "redirect:/admin/reservations?updated";
    }

    @GetMapping("/reports")
    @Transactional(readOnly = true)
    public String reports(Model model) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now().minusDays(6).atStartOfDay();

        model.addAttribute("todayOrders", orderRepository.countOrdersSince(startOfToday));
        model.addAttribute("weekOrders", orderRepository.countOrdersSince(startOfWeek));
        model.addAttribute("todayRevenue", dashboardService.getTodayRevenue());
        model.addAttribute("totalRevenue", dashboardService.getTotalRevenue());
        model.addAttribute("pendingOrders", dashboardService.getPendingOrders());
        model.addAttribute("preparingOrders", dashboardService.getPreparingOrders());
        model.addAttribute("completedToday", orderRepository.sumTotalPriceByStatusSince(OrderStatus.COMPLETED, startOfToday));
        model.addAttribute("recentOrders", orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate")).stream().limit(10).toList());
        return "reports";
    }
}
