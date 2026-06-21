package com.example.demo.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TaxService {

    @Value("${app.tax.rate:0.10}")
    private BigDecimal taxRate;

    public BigDecimal calculateTax(BigDecimal taxableAmount) {
        if (taxableAmount == null || taxableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return taxableAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTaxRatePercent() {
        return taxRate.multiply(BigDecimal.valueOf(100));
    }
}
