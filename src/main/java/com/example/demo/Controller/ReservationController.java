package com.example.demo.Controller;

import com.example.demo.Models.Branch;
import com.example.demo.Models.ReservationStatus;
import com.example.demo.Repositories.BranchRepository;
import com.example.demo.Service.ReservationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
public class ReservationController {

    private final ReservationService reservationService;
    private final BranchRepository branchRepository;

    public ReservationController(ReservationService reservationService, BranchRepository branchRepository) {
        this.reservationService = reservationService;
        this.branchRepository = branchRepository;
    }

    @GetMapping("/reservations")
    public String form(Model model) {
        model.addAttribute("branches", branchRepository.findByActiveTrue());
        return "reservations";
    }

    @PostMapping("/reservations")
    public String submit(
            @RequestParam String customerName,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam LocalDate reservationDate,
            @RequestParam LocalTime reservationTime,
            @RequestParam int guests,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) Long branchId,
            RedirectAttributes redirectAttributes) {
        try {
            reservationService.create(customerName, email, phone, reservationDate, reservationTime, guests, note, branchId);
            redirectAttributes.addFlashAttribute("success", true);
            return "redirect:/reservations?success";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reservations";
        }
    }
}
