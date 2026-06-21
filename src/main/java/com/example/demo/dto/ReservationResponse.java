package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationResponse(
        Long id,
        String customerName,
        String email,
        String phone,
        LocalDate reservationDate,
        LocalTime reservationTime,
        int guests,
        String status,
        String branchName,
        String note
) {}
