package com.pahappa.sacco.service.interest;

import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.math.RoundingMode;

@ApplicationScoped
public class SavingsInterestStrategy implements InterestStrategy {

    private static final BigDecimal ANNUAL_RATE_PERCENT = new BigDecimal("5.00");
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    @Override
    public BigDecimal calculateInterest(BigDecimal principal) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal monthlyRate = ANNUAL_RATE_PERCENT
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(MONTHS_PER_YEAR, 10, RoundingMode.HALF_UP);
        return principal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
    }
}
