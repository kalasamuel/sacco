package com.pahappa.sacco.entity;

import com.pahappa.sacco.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the invariants Account.credit()/debit() enforce directly
 * (Section 4) — these are pure unit tests against the entity in
 * isolation, no database or concurrency involved. The concurrency
 * guarantee itself is tested separately in
 * {@link com.pahappa.sacco.concurrency.ConcurrentWithdrawalTest}.
 */
class AccountTest {

    private Member newMember() {
        return new Member("MBR-001", "NIN-001", "Jane", "Doe", "0700000000", "jane@example.com");
    }

    @Test
    void creditIncreasesBalance() {
        Account account = new Account(newMember());
        account.credit(new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("1000.00"), account.getBalance());
    }

    @Test
    void debitDecreasesBalanceWhenSufficientFundsAboveMinimum() {
        Account account = new Account(newMember());
        account.credit(new BigDecimal("50000.00")); // above the 20,000 minimum
        account.debit(new BigDecimal("10000.00"));
        assertEquals(new BigDecimal("40000.00"), account.getBalance());
    }

    @Test
    void debitRejectsAmountExceedingBalance() {
        Account account = new Account(newMember());
        account.credit(new BigDecimal("30000.00"));
        assertThrows(InsufficientFundsException.class, () -> account.debit(new BigDecimal("50000.00")));
        // Balance must be unchanged after a rejected debit.
        assertEquals(new BigDecimal("30000.00"), account.getBalance());
    }

    @Test
    void debitRejectsAmountThatWouldBreachMinimumBalance() {
        Account account = new Account(newMember());
        account.credit(new BigDecimal("30000.00")); // 10,000 above the 20,000 floor
        // Withdrawing 15,000 would leave 15,000 — below the 20,000 minimum.
        assertThrows(InsufficientFundsException.class, () -> account.debit(new BigDecimal("15000.00")));
    }

    @Test
    void creditRejectsZeroOrNegativeAmount() {
        Account account = new Account(newMember());
        assertThrows(IllegalArgumentException.class, () -> account.credit(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> account.credit(new BigDecimal("-5.00")));
    }

    @Test
    void maxLoanAmountIsThreeTimesBalance() {
        Account account = new Account(newMember());
        account.credit(new BigDecimal("100000.00"));
        assertEquals(new BigDecimal("300000.00"), account.maxLoanAmount());
    }
}
