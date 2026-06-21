package com.example.demo.Service;

import com.example.demo.Models.Promotion;
import com.example.demo.Models.PromotionType;
import com.example.demo.Repositories.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.List;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public List<Promotion> findAll() { return promotionRepository.findAll(); }

    @Transactional
    public Promotion save(Promotion promotion) { return promotionRepository.save(promotion); }

    public BigDecimal calculatePromotionDiscount(BigDecimal subtotal) {
        LocalTime now = LocalTime.now();
        BigDecimal discount = BigDecimal.ZERO;
        for (Promotion promo : promotionRepository.findByActiveTrue()) {
            if (promo.getType() == PromotionType.HAPPY_HOUR) {
                if (promo.getStartTime() != null && promo.getEndTime() != null
                        && !now.isBefore(promo.getStartTime()) && !now.isAfter(promo.getEndTime())) {
                    discount = discount.add(subtotal.multiply(promo.getDiscountPercent())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                }
            }
        }
        return discount.min(subtotal);
    }
}
