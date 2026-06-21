package com.example.demo.Controller;

import com.example.demo.Models.Product;
import com.example.demo.Repositories.ProductRepository;
import com.example.demo.dto.ProductResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ApiProductController {

    private final ProductRepository productRepository;

    public ApiProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<ProductResponse> list(@RequestParam(required = false) String category) {
        return productRepository.findAll().stream()
                .filter(p -> category == null || category.equalsIgnoreCase(p.getCategory()))
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        return toResponse(product);
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getCategory(),
                p.getImageUrl(),
                p.getStock(),
                p.isInStock(),
                p.getBranch() != null ? p.getBranch().getName() : null
        );
    }
}
