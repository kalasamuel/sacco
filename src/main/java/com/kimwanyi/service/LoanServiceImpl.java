package com.kimwanyi.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.kimwanyi.dao.LoanRepaymentDAO;
import com.kimwanyi.dao.LoanDAO;
import com.kimwanyi.dao.MemberDAO;
import com.kimwanyi.dao.SavingsAccountDAO;
import com.kimwanyi.dao.UserAccountDAO;
import com.kimwanyi.model.Loan;
import com.kimwanyi.model.LoanRepayment;
import com.kimwanyi.model.Member;
import com.kimwanyi.model.SavingsAccount;
import com.kimwanyi.model.UserAccount;
import com.kimwanyi.model.dto.forms.LoanApplicationForm;
import com.kimwanyi.model.dto.forms.LoanDecisionForm;
import com.kimwanyi.model.dto.forms.LoanRepaymentForm;
import com.kimwanyi.model.enums.AuditAction;
import com.kimwanyi.model.enums.LoanStatus;
import com.kimwanyi.model.enums.NotificationType;
import com.kimwanyi.model.enums.Role;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import com.kimwanyi.service.interfaces.LoanService;
import com.kimwanyi.service.interfaces.AuditLogService;
import com.kimwanyi.service.interfaces.NotificationService;
import com.kimwanyi.strategy.LoanEligibility;
import com.kimwanyi.strategy.LoanInterest;

@Service
public class LoanServiceImpl implements LoanService {

    private final LoanDAO loanDAO;
    private final LoanRepaymentDAO loanRepaymentDAO;
    private final MemberDAO memberDAO;
    private final SavingsAccountDAO savingsAccountDAO;
    private final UserAccountDAO userAccountDAO;
    private final LoanEligibility loanEligibilityPolicy;
    private final LoanInterest loanInterestCalculator;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public LoanServiceImpl(
        LoanDAO loanDAO,
        LoanRepaymentDAO loanRepaymentDAO,
        MemberDAO memberDAO,
        SavingsAccountDAO savingsAccountDAO,
        UserAccountDAO userAccountDAO,
        LoanEligibility loanEligibilityPolicy,
        LoanInterest loanInterestCalculator,
        AuditLogService auditLogService,
        NotificationService notificationService
    ) {
        this.loanDAO = loanDAO;
        this.loanRepaymentDAO = loanRepaymentDAO;
        this.memberDAO = memberDAO;
        this.savingsAccountDAO = savingsAccountDAO;
        this.userAccountDAO = userAccountDAO;
        this.loanEligibilityPolicy = loanEligibilityPolicy;
        this.loanInterestCalculator = loanInterestCalculator;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public Loan applyLoan(LoanApplicationForm form) {
        Member member = memberDAO.findById(form.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        SavingsAccount savingsAccount = savingsAccountDAO.findByMemberId(member.getId())
                .orElseThrow(() -> new IllegalArgumentException("Savings account not found for member"));
        List<Loan> memberLoans = loanDAO.findByMemberId(member.getId());

        BigDecimal principal = form.getPrincipalAmount();
        loanEligibilityPolicy.verifyEligibility(member, savingsAccount, principal, memberLoans);

        BigDecimal interest = loanInterestCalculator.calculateInterest(principal);
        BigDecimal totalRepayable = principal.add(interest);

        Loan loan = new Loan();
        loan.setMember(member);
        loan.setPrincipal(principal);
        loan.setInterestRate(new BigDecimal("10.00"));
        loan.setInterestAmount(interest);
        loan.setTotalRepayable(totalRepayable);
        loan.setAmountRepaid(BigDecimal.ZERO);
        loan.setOutstandingBalance(totalRepayable);
        loan.setStatus(LoanStatus.PENDING);
        loan.setPurpose("General Loan");
        loan.setApplicationDate(LocalDate.now());

        Loan saved = loanDAO.save(loan);

        auditLogService.record(member.getUserAccount(), AuditAction.LOAN_APPLIED, "Loan", saved.getId(),
                "Applied for loan: UGX " + principal);

        notificationService.notifyAdmins(NotificationType.LOAN_APPLICATION, "New Loan Application",
                member.getUserAccount().getUsername() + " applied for UGX " + principal);

        return saved;
    }

    @Override
    @Transactional
    public Loan decideLoan(LoanDecisionForm form, Long adminUserId) {
        Loan loan = loanDAO.findById(form.getLoanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found"));

        UserAccount admin = userAccountDAO.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("Admin account not found"));

        if (admin.getRole() != Role.ADMIN || !admin.isEnabled()) {
            throw new SecurityException("Only an enabled administrator can approve or reject loans");
        }

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException("Loan is not in PENDING status");
        }

        loan.setDecisionDate(LocalDate.now());
        loan.setDecidedBy(admin);

        if (form.isApproved()) {
            loan.setStatus(LoanStatus.ACTIVE);
            loan.setDueDate(LocalDate.now().plusMonths(12));
            loanDAO.save(loan);

            auditLogService.record(admin, AuditAction.LOAN_APPROVED, "Loan", loan.getId(),
                    "Approved loan for member: " + loan.getMember().getUserAccount().getUsername());

            notificationService.notify(loan.getMember().getUserAccount(), NotificationType.LOAN_APPROVED,
                    "Loan Approved", "Your loan of UGX " + loan.getPrincipal() + " was approved.");
        } else {
            loan.setStatus(LoanStatus.REJECTED);
            loan.setRejectionReason(form.getRemarks());
            loanDAO.save(loan);

            auditLogService.record(admin, AuditAction.LOAN_REJECTED, "Loan", loan.getId(),
                    "Rejected loan. Reason: " + form.getRemarks());

            notificationService.notify(loan.getMember().getUserAccount(), NotificationType.LOAN_REJECTED,
                    "Loan Rejected", "Your loan application was rejected. Reason: " + form.getRemarks());
        }

        return loan;
    }

    @Override
    @Transactional
    public void repayLoan(LoanRepaymentForm form) {
        Loan loan = loanDAO.findById(form.getLoanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.OVERDUE) {
            throw new IllegalStateException("Loan is not active or overdue");
        }

        BigDecimal amount = form.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Repayment amount must be positive");
        }

        BigDecimal currentOutstanding = loan.getOutstandingBalance();
        if (amount.compareTo(currentOutstanding) > 0) {
            throw new IllegalArgumentException("Repayment amount exceeds outstanding balance");
        }

        BigDecimal newOutstanding = currentOutstanding.subtract(amount);
        BigDecimal newAmountRepaid = loan.getAmountRepaid().add(amount);

        loan.setOutstandingBalance(newOutstanding);
        loan.setAmountRepaid(newAmountRepaid);

        if (newOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.FULLY_REPAID);
        }

        loanDAO.save(loan);

        LoanRepayment repayment = new LoanRepayment();
        repayment.setLoan(loan);
        repayment.setAmount(amount);
        repayment.setBalanceBefore(currentOutstanding);
        repayment.setBalanceAfter(newOutstanding);
        repayment.setPaymentMethod("CASH");
        repayment.setReference("REP-" + System.currentTimeMillis());
        repayment.setPaymentDate(LocalDate.now());

        loanRepaymentDAO.save(repayment);

        auditLogService.record(loan.getMember().getUserAccount(), AuditAction.LOAN_REPAYMENT_RECORDED, "LoanRepayment", repayment.getId(),
                "Repaid UGX " + amount + " for Loan #" + loan.getId());

        notificationService.notify(loan.getMember().getUserAccount(), NotificationType.LOAN_REPAYMENT,
                "Repayment Recorded", "UGX " + amount + " repaid. Outstanding balance: UGX " + newOutstanding + ".");
    }

    @Override
    public List<Loan> getPendingLoans() {
        return loanDAO.findByStatus(LoanStatus.PENDING);
    }

    @Override
    public List<Loan> getLoansByMember(Long memberId) {
        return loanDAO.findByMemberId(memberId);
    }

    @Override
    public Loan getLoanById(Long loanId) {
        return loanDAO.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 15 0 * * *")
    public int markOverdueLoans() {
        List<Loan> overdue = loanDAO.findByStatusAndDueDateBefore(LoanStatus.ACTIVE, LocalDate.now());
        overdue.forEach(loan -> loan.setStatus(LoanStatus.OVERDUE));
        loanDAO.saveAll(overdue);
        return overdue.size();
    }
}
