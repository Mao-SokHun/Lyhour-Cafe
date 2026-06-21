package com.example.demo.Controller;

import com.example.demo.Models.CafeTable;
import com.example.demo.Models.Product;
import com.example.demo.Repositories.ProductRepository;
import com.example.demo.Service.TableService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class QrOrderController {

    private final TableService tableService;
    private final ProductRepository productRepository;

    public QrOrderController(TableService tableService, ProductRepository productRepository) {
        this.tableService = tableService;
        this.productRepository = productRepository;
    }

    @GetMapping("/qr/{code}")
    @Transactional(readOnly = true)
    public String qrMenu(@PathVariable String code, Model model) {
        CafeTable table = tableService.findByQrCode(code.toUpperCase());
        List<Product> products = productRepository.findAll().stream()
                .filter(Product::isAvailable)
                .toList();
        model.addAttribute("table", table);
        model.addAttribute("products", products);
        model.addAttribute("qrMode", true);
        return "qr-menu";
    }
}
