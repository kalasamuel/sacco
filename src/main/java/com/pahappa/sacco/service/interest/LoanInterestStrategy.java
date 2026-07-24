package com.pahappa.sacco.service.interest;

import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.math.RoundingMode;

@ApplicationScoped
public class LoanInterestStrategy implements InterestStrategy {

    private static final BigDecimal FLAT_RATE_PERCENT = new BigDecimal("10.00");

    @Override
    public BigDecimal calculateInterest(BigDecimal principal) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return principal.multiply(FLAT_RATE_PERCENT)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
