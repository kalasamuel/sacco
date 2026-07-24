package com.pahappa.sacco.entity;

import com.pahappa.sacco.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that Loan.transitionTo (Section 4/8) only permits the legal
 * transitions of the state machine diagrammed in Section 8, and rejects
 * everything else — the direct guarantee behind "nobody can tell what
 * stage an application is at" no longer being possible.
 */
class LoanStateMachineTest {

    private Loan newLoan() {
        Member member = new Member("MBR-002", "NIN-002", "John", "Smith", "0711111111", "john@example.com");
        return new Loan(member, new BigDecimal("100000.00"), new BigDecimal("10.00"), new BigDecimal("110000.00"));
    }

    @Test
    void newLoanStartsPending() {
        assertEquals(LoanStatus.PENDING, newLoan().getStatus());
    }

    @Test
    void pendingCanTransitionToApproved() {
        Loan loan = newLoan();
        loan.transitionTo(LoanStatus.APPROVED);
        assertEquals(LoanStatus.APPROVED, loan.getStatus());
    }

    @Test
    void pendingCanTransitionToRejected() {
        Loan loan = newLoan();
        loan.transitionTo(LoanStatus.REJECTED);
        assertEquals(LoanStatus.REJECTED, loan.getStatus());
    }

    @Test
    void pendingCannotTransitionDirectlyToActive() {
        Loan loan = newLoan();
        assertThrows(BusinessRuleViolationException.class, () -> loan.transitionTo(LoanStatus.ACTIVE));
    }

    @Test
    void pendingCannotTransitionDirectlyToClosed() {
        Loan loan = newLoan();
        assertThrows(BusinessRuleViolationException.class, () -> loan.transitionTo(LoanStatus.CLOSED));
    }

    @Test
    void rejectedIsTerminal() {
        Loan loan = newLoan();
        loan.transitionTo(LoanStatus.REJECTED);
        assertThrows(BusinessRuleViolationException.class, () -> loan.transitionTo(LoanStatus.APPROVED));
        assertThrows(BusinessRuleViolationException.class, () -> loan.transitionTo(LoanStatus.ACTIVE));
    }

    @Test
    void fullHappyPathToClosedViaFullRepayment() {
        Loan loan = newLoan();
        loan.transitionTo(LoanStatus.APPROVED);
        loan.transitionTo(LoanStatus.ACTIVE);
        loan.applyRepayment(new BigDecimal("110000.00")); // pays off totalDue exactly
        assertEquals(LoanStatus.CLOSED, loan.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), loan.getRemainingBalance());
    }

    @Test
    void repaymentExceedingRemainingBalanceIsRejected() {
        Loan loan = newLoan();
        loan.transitionTo(LoanStatus.APPROVED);
        loan.transitionTo(LoanStatus.ACTIVE);
        assertThrows(BusinessRuleViolationException.class,
                () -> loan.applyRepayment(new BigDecimal("999999.00")));
    }

    @Test
    void activeCanBecomeOverdueThenClosed() {
        Loan loan = newLoan();
        loan.transitionTo(LoanStatus.APPROVED);
        loan.transitionTo(LoanStatus.ACTIVE);
        loan.transitionTo(LoanStatus.OVERDUE);
        loan.applyRepayment(new BigDecimal("110000.00"));
        assertEquals(LoanStatus.CLOSED, loan.getStatus());
    }
}
