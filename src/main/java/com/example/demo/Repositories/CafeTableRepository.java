package com.example.demo.Repositories;

import com.example.demo.Models.CafeTable;
import com.example.demo.Models.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CafeTableRepository extends JpaRepository<CafeTable, Long> {
    List<CafeTable> findAllByOrderByTableNumberAsc();
    Optional<CafeTable> findByTableNumber(String tableNumber);
    Optional<CafeTable> findByQrCode(String qrCode);
    long countByStatus(TableStatus status);
}
