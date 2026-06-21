package com.example.demo.Repositories;

import com.example.demo.Models.EmployeeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {
    Optional<EmployeeProfile> findByUserId(Long userId);
    List<EmployeeProfile> findAllByOrderByUser_UsernameAsc();
}
