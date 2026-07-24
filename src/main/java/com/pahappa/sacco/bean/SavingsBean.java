package com.pahappa.sacco.bean;

import com.pahappa.sacco.entity.Loan;
import com.pahappa.sacco.entity.Member;
import com.pahappa.sacco.entity.Transaction;
import com.pahappa.sacco.exception.BusinessRuleViolationException;
import com.pahappa.sacco.exception.InsufficientFundsException;
import com.pahappa.sacco.exception.UnauthorizedException;
import com.pahappa.sacco.service.LoanService;
import com.pahappa.sacco.service.MemberService;
import com.pahappa.sacco.service.SavingsService;
import com.pahappa.sacco.util.FacesMessageUtil;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Named("savingsBean")
@ViewScoped
public class SavingsBean implements Serializable {

    private static final BigDecimal MIN_BALANCE = new BigDecimal("20000.00"); // Account's default

    @Inject
    private SavingsService savingsService;
    @Inject
    private MemberService memberService;
    @Inject
    private LoanService loanService;
    @Inject
    private CurrentUserBean currentUser;

    private Long selectedMemberId;
    private Member selectedMember;
    private BigDecimal amount;
    private List<Transaction> statement;
    private BigDecimal currentBalance;
    private String activeLoanStatusLabel;

    //  "instant passbook receipt" from the blueprint.
    private BigDecimal receiptPreviousBalance;
    private Transaction receiptTransaction;

    public void loadMember(Long memberId) {
        this.selectedMemberId = memberId;
        this.selectedMember = memberService.findById(memberId);
        refresh();
        resolveActiveLoanStatus();
    }

    public void loadIfPresent() {
        if (selectedMemberId != null) {
            loadMember(selectedMemberId);
        }
    }

    public void deposit() {
        try {
            BigDecimal balanceBefore = currentBalance;
            Transaction tx = savingsService.deposit(selectedMemberId, amount, currentUser.getUser());
            showReceipt(balanceBefore, tx);
            FacesMessageUtil.addInfo("Deposit recorded successfully.");
            amount = null;
            refresh();
        } catch (UnauthorizedException e) {
            FacesMessageUtil.addError("You are not permitted to perform deposits.");
        } catch (BusinessRuleViolationException | InsufficientFundsException e) {
            FacesMessageUtil.addError(e.getMessage());
        }
    }

    public void withdraw() {
        try {
            BigDecimal balanceBefore = currentBalance;
            Transaction tx = savingsService.withdraw(selectedMemberId, amount, currentUser.getUser());
            showReceipt(balanceBefore, tx);
            FacesMessageUtil.addInfo("Withdrawal recorded successfully.");
            amount = null;
            refresh();
        } catch (UnauthorizedException e) {
            FacesMessageUtil.addError("You are not permitted to perform withdrawals.");
        } catch (InsufficientFundsException e) {
            FacesMessageUtil.addError(e.getMessage());
        } catch (BusinessRuleViolationException e) {
            FacesMessageUtil.addError(e.getMessage());
        }
    }

    private void showReceipt(BigDecimal balanceBefore, Transaction tx) {
        this.receiptPreviousBalance = balanceBefore;
        this.receiptTransaction = tx;
    }

    
    public BigDecimal getMaxWithdrawable() {
        if (currentBalance == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal max = currentBalance.subtract(MIN_BALANCE);
        return max.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : max;
    }

    private void resolveActiveLoanStatus() {
        List<Loan> loans = loanService.getLoansForMember(selectedMemberId);
        activeLoanStatusLabel = loans.stream()
                .filter(Loan::isActiveOrOverdue)
                .findFirst()
                .map(l -> l.getStatus() + " (" + l.getRemainingBalance() + " remaining)")
                .orElse("None");
    }

    private void refresh() {
        if (selectedMemberId == null) {
            return;
        }
        this.currentBalance = savingsService.getBalance(selectedMemberId);
        this.statement = savingsService.getStatement(selectedMemberId);
    }

    public Long getSelectedMemberId() { return selectedMemberId; }
    public void setSelectedMemberId(Long selectedMemberId) { this.selectedMemberId = selectedMemberId; }
    public Member getSelectedMember() { return selectedMember; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public List<Transaction> getStatement() { return statement; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public String getActiveLoanStatusLabel() { return activeLoanStatusLabel; }
    public BigDecimal getReceiptPreviousBalance() { return receiptPreviousBalance; }
    public Transaction getReceiptTransaction() { return receiptTransaction; }
}

