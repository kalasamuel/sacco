package com.kimwanyi.strategy;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class LoanInterest {

    public BigDecimal calculateInterest(BigDecimal principal) {
        return principal.multiply(new BigDecimal("0.10"));
    }
}
