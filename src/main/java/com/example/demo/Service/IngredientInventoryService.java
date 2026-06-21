package com.example.demo.Service;

import com.example.demo.Models.*;
import com.example.demo.Repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class IngredientInventoryService {

    private final IngredientRepository ingredientRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeItemRepository recipeItemRepository;

    public IngredientInventoryService(
            IngredientRepository ingredientRepository,
            InventoryTransactionRepository transactionRepository,
            RecipeRepository recipeRepository,
            RecipeItemRepository recipeItemRepository) {
        this.ingredientRepository = ingredientRepository;
        this.transactionRepository = transactionRepository;
        this.recipeRepository = recipeRepository;
        this.recipeItemRepository = recipeItemRepository;
    }

    public List<Ingredient> findAll() {
        return ingredientRepository.findAllByOrderByNameAsc();
    }

    public List<Ingredient> findLowStock() {
        return ingredientRepository.findAll().stream().filter(Ingredient::isLowStock).toList();
    }

    @Transactional
    public Ingredient save(Ingredient ingredient) {
        return ingredientRepository.save(ingredient);
    }

    @Transactional
    public void adjustStock(Long ingredientId, BigDecimal change, InventoryTransactionType type, String note) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new IllegalArgumentException("Ingredient not found"));
        BigDecimal newQty = ingredient.getQuantity().add(change);
        if (newQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Insufficient ingredient stock: " + ingredient.getName());
        }
        ingredient.setQuantity(newQty);
        ingredientRepository.save(ingredient);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setIngredient(ingredient);
        tx.setType(type);
        tx.setQuantityChange(change);
        tx.setNote(note);
        transactionRepository.save(tx);
    }

    @Transactional
    public void deductForOrder(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            recipeRepository.findByProductId(item.getProduct().getId()).ifPresent(recipe -> {
                List<RecipeItem> items = recipeItemRepository.findByRecipeId(recipe.getId());
                for (RecipeItem ri : items) {
                    BigDecimal total = ri.getAmount().multiply(BigDecimal.valueOf(item.getQuantity()));
                    adjustStock(ri.getIngredient().getId(), total.negate(), InventoryTransactionType.SALE,
                            "Order #" + order.getId());
                }
            });
        }
    }

    public List<InventoryTransaction> recentTransactions() {
        return transactionRepository.findTop100ByOrderByCreatedAtDesc();
    }
}
