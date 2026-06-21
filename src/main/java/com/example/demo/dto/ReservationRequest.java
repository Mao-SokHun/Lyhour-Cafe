package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationRequest(
        String customerName,
        String email,
        String phone,
        LocalDate reservationDate,
        LocalTime reservationTime,
        int guests,
        String note,
        Long branchId
) {}
