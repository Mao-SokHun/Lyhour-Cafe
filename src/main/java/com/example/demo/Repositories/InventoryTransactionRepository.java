package com.example.demo.Repositories;

import com.example.demo.Models.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findTop100ByOrderByCreatedAtDesc();
}
