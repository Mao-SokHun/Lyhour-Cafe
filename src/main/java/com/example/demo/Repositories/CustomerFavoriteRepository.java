package com.example.demo.Repositories;

import com.example.demo.Models.CustomerFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerFavoriteRepository extends JpaRepository<CustomerFavorite, Long> {
    List<CustomerFavorite> findByCustomerId(Long customerId);
    boolean existsByCustomerIdAndProductId(Long customerId, Long productId);
    void deleteByCustomerIdAndProductId(Long customerId, Long productId);
}
