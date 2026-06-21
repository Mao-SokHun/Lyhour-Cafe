package com.example.demo.Service;

import com.example.demo.Models.Branch;
import com.example.demo.Models.Reservation;
import com.example.demo.Models.ReservationStatus;
import com.example.demo.Repositories.BranchRepository;
import com.example.demo.Repositories.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BranchRepository branchRepository;
    private final NotificationService notificationService;

    public ReservationService(
            ReservationRepository reservationRepository,
            BranchRepository branchRepository,
            NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.branchRepository = branchRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Reservation create(
            String customerName,
            String email,
            String phone,
            LocalDate date,
            LocalTime time,
            int guests,
            String note,
            Long branchId) {
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Reservation date must be today or later");
        }
        if (guests < 1 || guests > 20) {
            throw new IllegalArgumentException("Guests must be between 1 and 20");
        }

        Reservation reservation = new Reservation();
        reservation.setCustomerName(customerName);
        reservation.setEmail(email);
        reservation.setPhone(phone);
        reservation.setReservationDate(date);
        reservation.setReservationTime(time);
        reservation.setGuests(guests);
        reservation.setNote(note);
        reservation.setStatus(ReservationStatus.PENDING);

        Branch branch = branchId != null
                ? branchRepository.findById(branchId).orElse(getDefaultBranch())
                : getDefaultBranch();
        reservation.setBranch(branch);

        Reservation saved = reservationRepository.save(reservation);
        notificationService.sendReservationConfirmation(saved);
        return saved;
    }

    @Transactional
    public Reservation updateStatus(Long id, ReservationStatus status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));
        reservation.setStatus(status);
        Reservation saved = reservationRepository.save(reservation);
        notificationService.sendReservationConfirmation(saved);
        return saved;
    }

    public List<Reservation> findUpcoming() {
        return reservationRepository.findByReservationDateGreaterThanEqualOrderByReservationDateAsc(LocalDate.now());
    }

    public List<Reservation> findAll() {
        return reservationRepository.findAllByOrderByReservationDateDescReservationTimeDesc();
    }

    public List<Reservation> findByPhone(String phone) {
        return reservationRepository.findByPhoneOrderByReservationDateDescReservationTimeDesc(phone.trim());
    }

    public Branch getDefaultBranch() {
        return branchRepository.findByName("Lyhour Coffee — Main")
                .orElse(branchRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("No branch configured")));
    }
}
