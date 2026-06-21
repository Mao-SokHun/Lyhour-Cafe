package com.example.demo.Controller;

import com.example.demo.Models.CartItemDto;
import com.example.demo.Models.Coupon;
import com.example.demo.Models.CafeTable;
import com.example.demo.Models.Order;
import com.example.demo.Models.OrderSource;
import com.example.demo.Models.OrderStatus;
import com.example.demo.Models.Product;
import com.example.demo.Models.TableStatus;
import com.example.demo.Models.User;
import com.example.demo.Models.CheckoutRequest;
import com.example.demo.Repositories.OrderRepository;
import com.example.demo.Repositories.ProductRepository;
import com.example.demo.Repositories.UserRepository;
import com.example.demo.Models.ReservationStatus;
import com.example.demo.Service.AuditLogService;
import com.example.demo.Service.CouponService;
import com.example.demo.Service.DashboardService;
import com.example.demo.Service.OrderService;
import com.example.demo.Service.QrCodeService;
import com.example.demo.Service.ReservationService;
import com.example.demo.Service.TableService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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

    @Autowired
    private TableService tableService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private QrCodeService qrCodeService;

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
        model.addAttribute("tables", tableService.findAll());
        return "pos";
    }

    @PostMapping("/pos/checkout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> posCheckout(@RequestBody CheckoutRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cart is empty"));
        }

        User customer = request.getCustomerId() != null
                ? userRepository.findById(request.getCustomerId()).orElse(null)
                : userRepository.findByUsername("walkin");

        if (customer == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Customer not found"));
        }

        try {
            com.example.demo.Models.FulfillmentType type = request.getTableId() != null
                    ? com.example.demo.Models.FulfillmentType.DINE_IN
                    : com.example.demo.Models.FulfillmentType.PICKUP;
            Order order = orderService.createOrderFull(
                    customer,
                    request.getItems(),
                    OrderSource.IN_SHOP,
                    com.example.demo.Models.PaymentMethod.CASH,
                    request.getCouponCode(),
                    request.getTableId(),
                    type,
                    null);
            return ResponseEntity.ok(Map.of(
                    "message", "Sale completed",
                    "orderId", order.getId(),
                    "redirectUrl", "/admin/orders/" + order.getId() + "/receipt"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/kitchen")
    @Transactional(readOnly = true)
    public String kitchen(Model model) {
        model.addAttribute("orders", orderService.findKitchenOrders());
        model.addAttribute("statuses", List.of(OrderStatus.PENDING, OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.SERVED));
        return "kitchen";
    }

    @GetMapping("/kitchen/orders")
    @ResponseBody
    @Transactional(readOnly = true)
    public List<Map<String, Object>> kitchenOrdersApi() {
        return orderService.findKitchenOrders().stream().map(o -> Map.<String, Object>of(
                "id", o.getId(),
                "status", o.getStatus().name(),
                "username", o.getUsername(),
                "total", o.getTotalPrice(),
                "items", o.getOrderItems().stream()
                        .map(i -> i.getQuantity() + "x " + i.getName_product() + (i.getSize() != null ? " (" + i.getSize() + ")" : ""))
                        .toList()
        )).toList();
    }

    @PostMapping("/kitchen/orders/{id}/status")
    public String updateKitchenStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        orderService.updateStatus(id, status);
        return "redirect:/admin/kitchen?updated";
    }

    @GetMapping("/tables")
    @Transactional(readOnly = true)
    public String tables(Model model) {
        model.addAttribute("tables", tableService.findAll());
        model.addAttribute("statuses", TableStatus.values());
        return "list-tables";
    }

    @PostMapping("/tables/save")
    public String saveTable(@ModelAttribute CafeTable table) {
        tableService.save(table);
        return "redirect:/admin/tables?saved";
    }

    @PostMapping("/tables/{id}/status")
    public String updateTableStatus(@PathVariable Long id, @RequestParam TableStatus status) {
        tableService.updateStatus(id, status);
        return "redirect:/admin/tables?updated";
    }

    @GetMapping("/tables/{id}/qr.png")
    public void tableQrPng(
            @PathVariable Long id,
            HttpServletRequest request,
            HttpServletResponse response) throws java.io.IOException {
        CafeTable table = tableService.findById(id);
        String base = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443
                ? "" : ":" + request.getServerPort());
        String menuUrl = base + "/qr/" + table.getQrCode();
        byte[] png = qrCodeService.generatePng(menuUrl, 320);
        response.setContentType("image/png");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=3600");
        response.getOutputStream().write(png);
    }

    @GetMapping("/tables/{id}/print-qr")
    @Transactional(readOnly = true)
    public String printTableQr(@PathVariable Long id, HttpServletRequest request, Model model) {
        CafeTable table = tableService.findById(id);
        String base = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443
                ? "" : ":" + request.getServerPort());
        model.addAttribute("table", table);
        model.addAttribute("menuUrl", base + "/qr/" + table.getQrCode());
        return "table-qr-print";
    }

    @GetMapping("/coupons")
    public String coupons(Model model) {
        model.addAttribute("coupons", couponService.findAll());
        model.addAttribute("coupon", new Coupon());
        return "list-coupons";
    }

    @PostMapping("/coupons/save")
    public String saveCoupon(@ModelAttribute Coupon coupon) {
        couponService.save(coupon);
        return "redirect:/admin/coupons?saved";
    }

    @GetMapping("/coupons/delete")
    public String deleteCoupon(@RequestParam Long id) {
        couponService.delete(id);
        return "redirect:/admin/coupons?deleted";
    }

    @GetMapping("/audit-logs")
    public String auditLogs(Model model) {
        model.addAttribute("logs", auditLogService.recent());
        return "audit-logs";
    }

    @GetMapping("/orders/{id}/receipt")
    @Transactional(readOnly = true)
    public String receipt(@PathVariable Long id, Model model) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid order Id:" + id));
        model.addAttribute("order", order);
        return "receipt";
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
        model.addAttribute("weekRevenue", dashboardService.getWeekRevenue());
        model.addAttribute("monthRevenue", dashboardService.getMonthRevenue());
        model.addAttribute("bestSellers", dashboardService.getBestSellingProducts());
        model.addAttribute("availableTables", dashboardService.getAvailableTables());
        model.addAttribute("recentOrders", orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate")).stream().limit(10).toList());
        return "reports";
    }
}
