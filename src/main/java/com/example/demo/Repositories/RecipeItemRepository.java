package com.example.demo.Repositories;

import com.example.demo.Models.RecipeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecipeItemRepository extends JpaRepository<RecipeItem, Long> {
    List<RecipeItem> findByRecipeId(Long recipeId);
}
