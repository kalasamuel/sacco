package com.kimwanyi.util;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.kimwanyi.dao.SavingsAccountDAO;

@Component
public class SavingsAccountNumberGenerator {

    private final SavingsAccountDAO savingsAccountDAO;

    public SavingsAccountNumberGenerator(SavingsAccountDAO savingsAccountDAO) {
        this.savingsAccountDAO = savingsAccountDAO;
    }

    public String generate() {
        int year = LocalDate.now().getYear();
        String prefix = "SAV-" + year + "-";

        long sequence = savingsAccountDAO.countByAccountNumberStartingWith(prefix) + 1;
        String candidate = prefix + String.format("%04d", sequence);

        while (savingsAccountDAO.existsByAccountNumber(candidate)) {
            sequence++;
            candidate = prefix + String.format("%04d", sequence);
        }

        return candidate;
    }
}
