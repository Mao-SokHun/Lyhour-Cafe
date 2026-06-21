package com.example.demo.Service;

import com.example.demo.Models.MembershipTier;
import com.example.demo.Models.User;
import com.example.demo.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class LoyaltyService {

    private final UserRepository userRepository;

    @Value("${app.loyalty.points-per-dollar:1}")
    private int pointsPerDollar;

    public LoyaltyService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public int calculatePointsEarned(BigDecimal totalPaid) {
        if (totalPaid == null) {
            return 0;
        }
        return totalPaid.intValue() * pointsPerDollar;
    }

    @Transactional
    public void awardPoints(User customer, int points) {
        if (customer == null || points <= 0 || !"CUSTOMER".equals(customer.getRole())) {
            return;
        }
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + points);
        updateMembershipTier(customer);
        userRepository.save(customer);
    }

    @Transactional
    public void recordSpending(User customer, BigDecimal amount) {
        if (customer == null || amount == null || !"CUSTOMER".equals(customer.getRole())) {
            return;
        }
        customer.setTotalSpending(customer.getTotalSpending().add(amount));
        updateMembershipTier(customer);
        userRepository.save(customer);
    }

    private void updateMembershipTier(User customer) {
        int pts = customer.getLoyaltyPoints();
        BigDecimal spent = customer.getTotalSpending();
        if (pts >= 500 || spent.compareTo(BigDecimal.valueOf(500)) >= 0) {
            customer.setMembershipTier(MembershipTier.VIP);
        } else if (pts >= 200 || spent.compareTo(BigDecimal.valueOf(200)) >= 0) {
            customer.setMembershipTier(MembershipTier.GOLD);
        } else if (pts >= 50 || spent.compareTo(BigDecimal.valueOf(50)) >= 0) {
            customer.setMembershipTier(MembershipTier.SILVER);
        } else {
            customer.setMembershipTier(MembershipTier.BRONZE);
        }
    }
}
