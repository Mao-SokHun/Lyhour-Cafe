package com.example.demo.Service;

import com.example.demo.Models.Product;
import com.example.demo.Repositories.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        if (product.getStock() < quantity) {
            throw new IllegalStateException("Insufficient stock for " + product.getName()
                    + ". Available: " + product.getStock());
        }
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }

    public long countLowStockItems() {
        return productRepository.countByStockLessThanEqual(10);
    }

    @Transactional
    public void restoreStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        product.setStock(product.getStock() + quantity);
        productRepository.save(product);
    }
}
