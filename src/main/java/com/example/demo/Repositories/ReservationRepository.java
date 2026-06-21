package com.example.demo.Repositories;

import com.example.demo.Models.Reservation;
import com.example.demo.Models.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByStatusOrderByReservationDateAscReservationTimeAsc(ReservationStatus status);
    List<Reservation> findAllByOrderByReservationDateDescReservationTimeDesc();
    List<Reservation> findByReservationDateGreaterThanEqualOrderByReservationDateAsc(LocalDate date);
    List<Reservation> findByPhoneOrderByReservationDateDescReservationTimeDesc(String phone);
}
