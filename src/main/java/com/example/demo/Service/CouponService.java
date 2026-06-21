package com.example.demo.Service;

import com.example.demo.Models.Coupon;
import com.example.demo.Models.CouponType;
import com.example.demo.Repositories.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final AuditLogService auditLogService;

    public CouponService(CouponRepository couponRepository, AuditLogService auditLogService) {
        this.couponRepository = couponRepository;
        this.auditLogService = auditLogService;
    }

    public List<Coupon> findAll() {
        return couponRepository.findAll();
    }

    public Coupon findById(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + id));
    }

    @Transactional
    public Coupon save(Coupon coupon) {
        if (coupon.getCode() != null) {
            coupon.setCode(coupon.getCode().trim().toUpperCase());
        }
        Coupon saved = couponRepository.save(coupon);
        auditLogService.log("COUPON_SAVE", "Coupon " + saved.getCode());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Coupon coupon = findById(id);
        couponRepository.delete(coupon);
        auditLogService.log("COUPON_DELETE", "Coupon " + coupon.getCode());
    }

    public BigDecimal calculateDiscount(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            return BigDecimal.ZERO;
        }
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon code"));

        validateCoupon(coupon);

        if (coupon.getType() == CouponType.PERCENT) {
            return subtotal.multiply(coupon.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return coupon.getValue().min(subtotal);
    }

    @Transactional
    public void markUsed(String code) {
        if (code == null || code.isBlank()) {
            return;
        }
        couponRepository.findByCodeIgnoreCase(code.trim()).ifPresent(coupon -> {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        });
    }

    private void validateCoupon(Coupon coupon) {
        if (!coupon.isActive()) {
            throw new IllegalArgumentException("Coupon is inactive");
        }
        LocalDate today = LocalDate.now();
        if (coupon.getValidFrom() != null && today.isBefore(coupon.getValidFrom())) {
            throw new IllegalArgumentException("Coupon is not valid yet");
        }
        if (coupon.getValidUntil() != null && today.isAfter(coupon.getValidUntil())) {
            throw new IllegalArgumentException("Coupon has expired");
        }
        if (coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw new IllegalArgumentException("Coupon usage limit reached");
        }
    }
}
