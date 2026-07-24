package com.pahappa.sacco.service.interest;

import java.math.BigDecimal;

public interface InterestStrategy {
    BigDecimal calculateInterest(BigDecimal principal);
}
