package com.example.demo.Repositories;

import com.example.demo.Models.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findFirstByUserIdAndClockOutIsNullOrderByClockInDesc(Long userId);
    List<Attendance> findTop50ByOrderByClockInDesc();
}
