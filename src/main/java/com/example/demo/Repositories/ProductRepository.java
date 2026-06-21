package com.example.demo.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Models.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase(String name);

    long countByStockLessThanEqual(int stock);

    List<Product> findByStockLessThanEqualOrderByStockAsc(int stock);
}
