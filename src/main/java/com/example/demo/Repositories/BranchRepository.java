package com.example.demo.Repositories;

import com.example.demo.Models.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByActiveTrue();
    Optional<Branch> findByName(String name);
}
