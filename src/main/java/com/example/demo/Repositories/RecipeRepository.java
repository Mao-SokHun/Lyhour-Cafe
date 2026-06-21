package com.example.demo.Repositories;

import com.example.demo.Models.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    Optional<Recipe> findByProductId(Long productId);
}
