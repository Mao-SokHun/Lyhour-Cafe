package com.example.demo.Repositories;

import com.example.demo.Models.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    List<Ingredient> findAllByOrderByNameAsc();
    List<Ingredient> findByQuantityLessThanEqualOrderByQuantityAsc(java.math.BigDecimal threshold);
}
