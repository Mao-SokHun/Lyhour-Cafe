package com.example.demo.Controller;

import com.example.demo.Models.Reservation;
import com.example.demo.Service.ReservationService;
import com.example.demo.dto.OrderMapper;
import com.example.demo.dto.ReservationRequest;
import com.example.demo.dto.ReservationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reservations")
public class ApiReservationController {

    private final ReservationService reservationService;

    public ApiReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> list(@RequestParam String phone) {
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        List<ReservationResponse> results = reservationService.findByPhone(phone).stream()
                .map(OrderMapper::toReservationResponse)
                .toList();
        return ResponseEntity.ok(results);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody ReservationRequest request) {
        try {
            Reservation saved = reservationService.create(
                    request.customerName(),
                    request.email(),
                    request.phone(),
                    request.reservationDate(),
                    request.reservationTime(),
                    request.guests(),
                    request.note(),
                    request.branchId());
            return ResponseEntity.ok(Map.of(
                    "id", saved.getId(),
                    "status", saved.getStatus().name(),
                    "message", "Reservation submitted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
