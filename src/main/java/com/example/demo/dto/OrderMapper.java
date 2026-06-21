package com.example.demo.dto;

import com.example.demo.Models.Branch;
import com.example.demo.Models.OrderItem;
import com.example.demo.Models.Reservation;

public final class OrderMapper {

    private OrderMapper() {}

    public static OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getName_product(),
                item.getQuantity(),
                item.getSize(),
                item.getNote(),
                item.getPriceAtTimeOfOrder());
    }

    public static BranchResponse toBranchResponse(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getName(),
                branch.getAddress(),
                branch.getPhone());
    }

    public static ReservationResponse toReservationResponse(Reservation r) {
        String branchName = r.getBranch() != null ? r.getBranch().getName() : null;
        return new ReservationResponse(
                r.getId(),
                r.getCustomerName(),
                r.getEmail(),
                r.getPhone(),
                r.getReservationDate(),
                r.getReservationTime(),
                r.getGuests(),
                r.getStatus().name(),
                branchName,
                r.getNote());
    }
}
