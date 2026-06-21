package com.example.demo.Controller;

import com.example.demo.Models.*;
import com.example.demo.Repositories.BranchRepository;
import com.example.demo.Repositories.OrderRepository;
import com.example.demo.Repositories.UserRepository;
import com.example.demo.Service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminEnterpriseController {

    private final IngredientInventoryService ingredientInventoryService;
    private final SupplierService supplierService;
    private final PromotionService promotionService;
    private final EmployeeService employeeService;
    private final AccountingService accountingService;
    private final AppSettingService appSettingService;
    private final ReportExportService reportExportService;
    private final BranchRepository branchRepository;
    private final TableService tableService;
    private final NotificationService notificationService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PdfExportService pdfExportService;
    private final BackupService backupService;

    public AdminEnterpriseController(
            IngredientInventoryService ingredientInventoryService,
            SupplierService supplierService,
            PromotionService promotionService,
            EmployeeService employeeService,
            AccountingService accountingService,
            AppSettingService appSettingService,
            ReportExportService reportExportService,
            BranchRepository branchRepository,
            TableService tableService,
            NotificationService notificationService,
            OrderRepository orderRepository,
            UserRepository userRepository,
            PdfExportService pdfExportService,
            BackupService backupService) {
        this.ingredientInventoryService = ingredientInventoryService;
        this.supplierService = supplierService;
        this.promotionService = promotionService;
        this.employeeService = employeeService;
        this.accountingService = accountingService;
        this.appSettingService = appSettingService;
        this.reportExportService = reportExportService;
        this.branchRepository = branchRepository;
        this.tableService = tableService;
        this.notificationService = notificationService;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.pdfExportService = pdfExportService;
        this.backupService = backupService;
    }

    @GetMapping("/ingredients")
    @Transactional(readOnly = true)
    public String ingredients(Model model) {
        model.addAttribute("ingredients", ingredientInventoryService.findAll());
        model.addAttribute("ingredient", new Ingredient());
        model.addAttribute("transactions", ingredientInventoryService.recentTransactions());
        return "list-ingredients";
    }

    @PostMapping("/ingredients/save")
    public String saveIngredient(@ModelAttribute Ingredient ingredient) {
        ingredientInventoryService.save(ingredient);
        return "redirect:/admin/ingredients?saved";
    }

    @PostMapping("/ingredients/{id}/adjust")
    public String adjustIngredient(@PathVariable Long id, @RequestParam BigDecimal amount,
                                   @RequestParam InventoryTransactionType type) {
        ingredientInventoryService.adjustStock(id, amount, type, "Manual adjustment");
        return "redirect:/admin/ingredients?updated";
    }

    @GetMapping("/suppliers")
    @Transactional(readOnly = true)
    public String suppliers(Model model) {
        model.addAttribute("suppliers", supplierService.findAll());
        model.addAttribute("supplier", new Supplier());
        model.addAttribute("orders", supplierService.findAllOrders());
        model.addAttribute("ingredients", ingredientInventoryService.findAll());
        return "list-suppliers";
    }

    @PostMapping("/suppliers/save")
    public String saveSupplier(@ModelAttribute Supplier supplier) {
        supplierService.save(supplier);
        return "redirect:/admin/suppliers?saved";
    }

    @PostMapping("/suppliers/orders/receive")
    public String receivePo(@RequestParam Long orderId) {
        supplierService.receiveOrder(orderId);
        return "redirect:/admin/suppliers?received";
    }

    @GetMapping("/promotions")
    public String promotions(Model model) {
        model.addAttribute("promotions", promotionService.findAll());
        model.addAttribute("promotion", new Promotion());
        return "list-promotions";
    }

    @PostMapping("/promotions/save")
    public String savePromotion(@ModelAttribute Promotion promotion) {
        promotionService.save(promotion);
        return "redirect:/admin/promotions?saved";
    }

    @GetMapping("/employees")
    @Transactional(readOnly = true)
    public String employees(Model model) {
        model.addAttribute("profiles", employeeService.findAllProfiles());
        model.addAttribute("staff", employeeService.findStaffUsers());
        model.addAttribute("attendance", employeeService.recentAttendance());
        model.addAttribute("branches", branchRepository.findAll());
        return "list-employees";
    }

    @PostMapping("/employees/profile/save")
    public String saveEmployeeProfile(@ModelAttribute EmployeeProfile profile, @RequestParam Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        profile.setUser(user);
        employeeService.saveProfile(profile);
        return "redirect:/admin/employees?saved";
    }

    @PostMapping("/employees/clock-in")
    public String clockIn(@RequestParam Long userId) {
        employeeService.clockIn(userId);
        return "redirect:/admin/employees?clocked";
    }

    @PostMapping("/employees/clock-out")
    public String clockOut(@RequestParam Long userId) {
        employeeService.clockOut(userId);
        return "redirect:/admin/employees?clocked";
    }

    @GetMapping("/accounting")
    public String accounting(Model model) {
        model.addAttribute("expenses", accountingService.findAllExpenses());
        model.addAttribute("expense", new Expense());
        model.addAttribute("monthRevenue", accountingService.getMonthRevenue());
        model.addAttribute("monthExpenses", accountingService.getMonthExpenses());
        model.addAttribute("monthProfit", accountingService.getMonthProfit());
        model.addAttribute("categories", ExpenseCategory.values());
        return "accounting";
    }

    @PostMapping("/accounting/expense/save")
    public String saveExpense(@ModelAttribute Expense expense) {
        accountingService.saveExpense(expense);
        return "redirect:/admin/accounting?saved";
    }

    @GetMapping("/branches")
    public String branches(Model model) {
        model.addAttribute("branches", branchRepository.findAll());
        model.addAttribute("branch", new Branch());
        return "list-branches";
    }

    @PostMapping("/branches/save")
    public String saveBranch(@ModelAttribute Branch branch) {
        branchRepository.save(branch);
        return "redirect:/admin/branches?saved";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("settings", appSettingService.findAll());
        model.addAttribute("notificationStatus", notificationService.getStatus());
        model.addAttribute("telegramChatId", notificationService.resolveChatId());
        return "admin-settings";
    }

    @PostMapping("/settings/save")
    public String saveSetting(@RequestParam String key, @RequestParam String value) {
        appSettingService.save(key, value);
        return "redirect:/admin/settings?saved";
    }

    @GetMapping("/table-layout")
    @Transactional(readOnly = true)
    public String tableLayout(Model model) {
        model.addAttribute("tables", tableService.findAll());
        return "table-layout";
    }

    @PostMapping("/tables/merge")
    public String mergeTables(@RequestParam Long targetId, @RequestParam Long sourceId) {
        tableService.mergeTables(targetId, sourceId);
        return "redirect:/admin/table-layout?merged";
    }

    @PostMapping("/tables/split")
    public String splitTable(@RequestParam Long tableId) {
        tableService.splitTable(tableId);
        return "redirect:/admin/table-layout?split";
    }

    @PostMapping("/tables/transfer")
    public String transferTable(@RequestParam Long fromId, @RequestParam Long toId) {
        tableService.transferTable(fromId, toId);
        return "redirect:/admin/table-layout?transferred";
    }

    @GetMapping("/reports/export/sales")
    public void exportSales(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales.csv");
        response.getOutputStream().write(reportExportService.exportSalesCsv());
    }

    @GetMapping("/reports/export/summary")
    public void exportSummary(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=summary.csv");
        response.getOutputStream().write(reportExportService.exportSummaryCsv());
    }

    @GetMapping("/reports/export/sales.pdf")
    public void exportSalesPdf(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales.pdf");
        response.getOutputStream().write(pdfExportService.exportSalesPdf());
    }

    @GetMapping("/reports/export/summary.pdf")
    public void exportSummaryPdf(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=summary.pdf");
        response.getOutputStream().write(pdfExportService.exportSummaryPdf());
    }

    @GetMapping("/orders/{id}/receipt.pdf")
    public void receiptPdf(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Order order = orderRepository.findById(id).orElseThrow();
        response.setContentType("application/pdf");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt-" + id + ".pdf");
        response.getOutputStream().write(pdfExportService.exportReceiptPdf(order));
    }

    @PostMapping("/backup/run")
    public String runBackup() throws IOException {
        backupService.runBackup();
        return "redirect:/admin/settings?backup=ok";
    }

    @PostMapping("/notifications/connect-telegram")
    public String connectTelegram() {
        String chatId = notificationService.discoverTelegramChatId();
        return "redirect:/admin/settings?" + (chatId != null ? "telegram=linked" : "telegram=nomessage");
    }

    @PostMapping("/notifications/test-telegram")
    public String testTelegram() {
        boolean ok = notificationService.sendTestTelegram();
        return "redirect:/admin/settings?" + (ok ? "telegram=ok" : "telegram=fail");
    }

    @PostMapping("/notifications/test-push")
    public String testPush() {
        boolean ok = notificationService.sendTestPush();
        return "redirect:/admin/settings?" + (ok ? "push=ok" : "push=fail");
    }

    @GetMapping("/orders/{id}/email-receipt")
    public String emailReceipt(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElseThrow();
        notificationService.sendReceiptEmail(order);
        return "redirect:/admin/orders/" + id + "/receipt?emailed";
    }

    @GetMapping("/api/chart-data")
    @ResponseBody
    public java.util.Map<String, Object> chartData() {
        return java.util.Map.of(
                "dailyRevenue", reportExportService.getDailyRevenueChart(),
                "topCustomers", reportExportService.getTopCustomers(),
                "worstSellers", reportExportService.getWorstSellers());
    }
}
