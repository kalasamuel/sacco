package com.pahappa.sacco.service;

import com.pahappa.sacco.dao.AccountDao;
import com.pahappa.sacco.dao.MemberDao;
import com.pahappa.sacco.dao.TransactionDao;
import com.pahappa.sacco.entity.*;
import com.pahappa.sacco.exception.BusinessRuleViolationException;
import com.pahappa.sacco.security.RoleGuard;
import com.pahappa.sacco.service.interest.SavingsInterestStrategy;
import com.pahappa.sacco.util.TransactionUtil;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

 // Savings module business logic: deposit, withdraw, view balance/history, monthly interest posting.
@ApplicationScoped
public class SavingsService {

    private final AccountDao accountDao;
    private final MemberDao memberDao;
    private final TransactionDao transactionDao;
    private final SavingsInterestStrategy savingsInterestStrategy;

    @Inject
    public SavingsService(AccountDao accountDao, MemberDao memberDao, TransactionDao transactionDao,
                           SavingsInterestStrategy savingsInterestStrategy) {
        this.accountDao = accountDao;
        this.memberDao = memberDao;
        this.transactionDao = transactionDao;
        this.savingsInterestStrategy = savingsInterestStrategy;
    }

    protected SavingsService() {
        this.accountDao = null;
        this.memberDao = null;
        this.transactionDao = null;
        this.savingsInterestStrategy = null;
    }

    public Transaction deposit(Long memberId, BigDecimal amount, User performedBy) {
        RoleGuard.require(performedBy, Role.CASHIER, Role.ADMIN);
        validateMemberActive(memberId);

        return TransactionUtil.execute(em -> {
            Account account = accountDao.findByMemberIdForUpdate(memberId)
                    .orElseThrow(() -> new BusinessRuleViolationException("No savings account found for this member."));

            account.credit(amount);

            Transaction tx = new Transaction(account, performedBy, TransactionType.DEPOSIT,
                    amount, account.getBalance(), generateReference());
            return transactionDao.save(tx);
        });
    }

    public Transaction withdraw(Long memberId, BigDecimal amount, User performedBy) {
        RoleGuard.require(performedBy, Role.CASHIER, Role.ADMIN);
        validateMemberActive(memberId);

        return TransactionUtil.execute(em -> {
            // The lock is acquired here, BEFORE the balance is read for the
            // debit check inside account.debit()
            // the exact line that closes the "cashier misread the balance" race condition.
            Account account = accountDao.findByMemberIdForUpdate(memberId)
                    .orElseThrow(() -> new BusinessRuleViolationException("No savings account found for this member."));

            account.debit(amount); // throws InsufficientFundsException on violation

            Transaction tx = new Transaction(account, performedBy, TransactionType.WITHDRAWAL,
                    amount, account.getBalance(), generateReference());
            return transactionDao.save(tx);
        });
    }

    //Posts one month's interest to a single account.
    public Transaction postMonthlyInterest(Long memberId, User performedBy) {
        RoleGuard.require(performedBy, Role.ADMIN);

        return TransactionUtil.execute(em -> {
            Account account = accountDao.findByMemberIdForUpdate(memberId)
                    .orElseThrow(() -> new BusinessRuleViolationException("No savings account found for this member."));

            BigDecimal interest = savingsInterestStrategy.calculateInterest(account.getBalance());
            if (interest.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }

            account.credit(interest);

            Transaction tx = new Transaction(account, performedBy, TransactionType.INTEREST,
                    interest, account.getBalance(), generateReference());
            return transactionDao.save(tx);
        });
    }

    public int postMonthlyInterestToAllAccounts(User performedBy) {
        RoleGuard.require(performedBy, Role.ADMIN);
        List<Member> activeMembers = memberDao.findAllActive();
        int postedCount = 0;
        for (Member member : activeMembers) {
            Transaction tx = postMonthlyInterest(member.getId(), performedBy);
            if (tx != null) {
                postedCount++;
            }
        }
        return postedCount;
    }

    public BigDecimal getBalance(Long memberId) {
        return accountDao.findByMemberId(memberId)
                .map(Account::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    //Informational-only maximum loan eligibility (3x savings balance),
     //for display on the loan application form BEFORE submission
    public BigDecimal getMaxLoanEligibility(Long memberId) {
        return accountDao.findByMemberId(memberId)
                .map(Account::maxLoanAmount)
                .orElse(BigDecimal.ZERO);
    }

    public List<Transaction> getStatement(Long memberId) {
        return accountDao.findByMemberId(memberId)
                .map(account -> transactionDao.findByAccountOrderedDesc(account.getId()))
                .orElseGet(java.util.Collections::emptyList);
    }

    //Total savings held across every account, right now — the direct
    //fix for "it takes us almost a week after month-end to know how
     // much money the SACCO actually holds."
    public BigDecimal getTotalSavingsHeld(User performedBy) {
        RoleGuard.require(performedBy, Role.ADMIN);
        return transactionDao.totalSavingsHeld();
    }

    //Monthly cash inflow/outflow figures for the admin dashboard chart
    public BigDecimal getTotalByTypeAndRange(TransactionType type, java.time.LocalDateTime from,
                                              java.time.LocalDateTime to, User performedBy) {
        RoleGuard.require(performedBy, Role.ADMIN);
        return transactionDao.sumByTypeAndDateRange(type, from, to);
    }

    //receipt/reference number for a ledger entry.
    private String generateReference() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    //Ensures the member exists and is active before allowing cashier actions.

    private void validateMemberActive(Long memberId) {
        if (memberId == null) {
            throw new BusinessRuleViolationException("Member id must be provided.");
        }
        Member member = memberDao.findById(memberId)
                .orElseThrow(() -> new BusinessRuleViolationException("Member not found."));
        if (!Boolean.TRUE.equals(member.getActive())) {
            throw new BusinessRuleViolationException("This member account is deactivated.");
        }
    }
}
