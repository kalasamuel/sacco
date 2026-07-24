package com.pahappa.sacco.service;

import com.pahappa.sacco.dao.AccountDao;
import com.pahappa.sacco.dao.LoanDao;
import com.pahappa.sacco.dao.LoanRepaymentDao;
import com.pahappa.sacco.dao.MemberDao;
import com.pahappa.sacco.entity.*;
import com.pahappa.sacco.exception.BusinessRuleViolationException;
import com.pahappa.sacco.security.RoleGuard;
import com.pahappa.sacco.service.interest.LoanInterestStrategy;
import com.pahappa.sacco.util.TransactionUtil;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LoanService {

    // Loan term in months.
    private static final int LOAN_TERM_MONTHS = 6;

    private final AccountDao accountDao;
    private final MemberDao memberDao;
    private final LoanDao loanDao;
    private final LoanRepaymentDao loanRepaymentDao;
    private final LoanInterestStrategy loanInterestStrategy;

    @Inject
    public LoanService(AccountDao accountDao, MemberDao memberDao, LoanDao loanDao,
                        LoanRepaymentDao loanRepaymentDao, LoanInterestStrategy loanInterestStrategy) {
        this.accountDao = accountDao;
        this.memberDao = memberDao;
        this.loanDao = loanDao;
        this.loanRepaymentDao = loanRepaymentDao;
        this.loanInterestStrategy = loanInterestStrategy;
    }

    protected LoanService() {
        this.accountDao = null;
        this.memberDao = null;
        this.loanDao = null;
        this.loanRepaymentDao = null;
        this.loanInterestStrategy = null;
    }

    /** applies for a loan on behalf of a member with eligibility checks (no in-flight loan, amount within 3x savings) within a single pessimistic-locked transaction. */
    public Loan applyForLoan(Long memberId, BigDecimal principal, User performedBy) {
        RoleGuard.require(performedBy, Role.MEMBER, Role.CASHIER, Role.LOAN_OFFICER, Role.ADMIN);

        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("Loan principal must be positive.");
        }

        Member member = memberDao.findById(memberId)
                .orElseThrow(() -> new BusinessRuleViolationException("Member not found."));
        if (!Boolean.TRUE.equals(member.getActive())) {
            throw new BusinessRuleViolationException("This member account is deactivated.");
        }

        return TransactionUtil.execute(em -> {

            Account account = accountDao.findByMemberIdForUpdate(memberId)
                    .orElseThrow(() -> new BusinessRuleViolationException("No savings account found for this member."));

            List<Loan> inFlight = loanDao.findInFlightLoansForMember(memberId);
            if (!inFlight.isEmpty()) {
                throw new BusinessRuleViolationException(
                        "This member already has an unresolved loan application or active loan.");
            }

            BigDecimal maxAllowed = account.maxLoanAmount(); // 3x savings balance
            if (principal.compareTo(maxAllowed) > 0) {
                throw new BusinessRuleViolationException(
                        "Requested amount " + principal + " exceeds the maximum allowed of " + maxAllowed
                                + " (3x current savings balance).");
            }

            BigDecimal interestAmount = loanInterestStrategy.calculateInterest(principal);
            BigDecimal totalDue = principal.add(interestAmount);
            BigDecimal effectiveRate = Loan.FLAT_INTEREST_RATE;

            Loan loan = new Loan(member, principal, effectiveRate, totalDue);
            return loanDao.save(loan);
        });
    }

    public Loan approveLoan(Long loanId, User performedBy) {
        RoleGuard.require(performedBy, Role.ADMIN); // only an admin can approve or reject

        return TransactionUtil.execute(em -> {
            Loan loan = loanDao.findByIdForUpdate(loanId)
                    .orElseThrow(() -> new BusinessRuleViolationException("Loan not found."));

            Account account = accountDao.findByMemberIdForUpdate(loan.getMember().getId())
                    .orElseThrow(() -> new BusinessRuleViolationException("Member's savings account not found."));
            BigDecimal currentMaxEligible = account.maxLoanAmount();
            if (loan.getPrincipal().compareTo(currentMaxEligible) > 0) {
                throw new BusinessRuleViolationException(
                        "This applicant's savings balance has changed since application — the requested amount ("
                                + loan.getPrincipal() + ") now exceeds their current 3x entitlement ("
                                + currentMaxEligible + "). Reject and ask them to reapply for an eligible amount.");
            }

            loan.transitionTo(LoanStatus.APPROVED);
            loan.setApprovedBy(performedBy);
            loan.setApprovedAt(java.time.LocalDateTime.now());
            loan.transitionTo(LoanStatus.ACTIVE);
            loan.setDueDate(LocalDate.now().plusMonths(LOAN_TERM_MONTHS));

            return loan;
        });
    }

    public Loan rejectLoan(Long loanId, String reason, User performedBy) {
        RoleGuard.require(performedBy, Role.ADMIN);

        return TransactionUtil.execute(em -> {
            Loan loan = loanDao.findByIdForUpdate(loanId)
                    .orElseThrow(() -> new BusinessRuleViolationException("Loan not found."));
            loan.reject(reason); // throws IllegalArgumentException if reason is blank
            return loan;
        });
    }

    public LoanRepayment repay(Long loanId, BigDecimal amount, User performedBy) {
        RoleGuard.require(performedBy, Role.CASHIER, Role.ADMIN);

        return TransactionUtil.execute(em -> {
            Loan loan = loanDao.findByIdForUpdate(loanId)
                    .orElseThrow(() -> new BusinessRuleViolationException("Loan not found."));

            if (!loan.isActiveOrOverdue()) {
                throw new BusinessRuleViolationException(
                        "Cannot repay a loan that is not currently active or overdue.");
            }

            loan.applyRepayment(amount);

            LoanRepayment repayment = new LoanRepayment(loan, performedBy, amount,
                    loan.getRemainingBalance(), generateReference());
            return loanRepaymentDao.save(repayment);
        });
    }

    public int markOverdueLoans(User performedBy) {
        RoleGuard.require(performedBy, Role.ADMIN);

        return TransactionUtil.execute(em -> {
            List<Loan> overdue = loanDao.findOverdue(LocalDate.now());
            for (Loan loan : overdue) {
                loan.transitionTo(LoanStatus.OVERDUE);
            }
            return overdue.size();
        });
    }

    public List<Loan> getLoansForMember(Long memberId) {
        return loanDao.findByMember(memberId);
    }

    public List<Loan> getLoansByStatus(LoanStatus status) {
        return loanDao.findByStatus(status);
    }

    public java.util.Map<LoanStatus, Long> getLoanStatusCounts(User performedBy) {
        RoleGuard.require(performedBy, Role.ADMIN);
        java.util.Map<LoanStatus, Long> counts = new java.util.EnumMap<>(LoanStatus.class);
        for (LoanStatus status : LoanStatus.values()) {
            counts.put(status, loanDao.countByStatus(status));
        }
        return counts;
    }

    public List<LoanRepayment> getRepaymentHistory(Long loanId) {
        return loanRepaymentDao.findByLoanOrderedDesc(loanId);
    }

    public EligibilitySnapshot checkEligibility(Loan loan) {
        BigDecimal currentBalance = accountDao.findByMemberId(loan.getMember().getId())
                .map(Account::getBalance)
                .orElse(BigDecimal.ZERO);
        BigDecimal maxEligible = currentBalance.multiply(BigDecimal.valueOf(3));
        boolean withinEntitlement = loan.getPrincipal().compareTo(maxEligible) <= 0;

        long otherInFlightCount = loanDao.findInFlightLoansForMember(loan.getMember().getId()).stream()
                .filter(l -> !l.getId().equals(loan.getId()))
                .count();
        boolean noOtherActiveLoan = otherInFlightCount == 0;

        return new EligibilitySnapshot(currentBalance, maxEligible, withinEntitlement, noOtherActiveLoan);
    }

    public static class EligibilitySnapshot {
        public final BigDecimal currentSavings;
        public final BigDecimal maxEntitlement;
        public final boolean withinEntitlement;
        public final boolean noOtherActiveLoan;

        public EligibilitySnapshot(BigDecimal currentSavings, BigDecimal maxEntitlement,
                                    boolean withinEntitlement, boolean noOtherActiveLoan) {
            this.currentSavings = currentSavings;
            this.maxEntitlement = maxEntitlement;
            this.withinEntitlement = withinEntitlement;
            this.noOtherActiveLoan = noOtherActiveLoan;
        }

        public boolean isAllPass() {
            return withinEntitlement && noOtherActiveLoan;
        }
    }

    private String generateReference() {
        return "LNPMT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
