package com.pahappa.sacco.entity;

import com.pahappa.sacco.exception.BusinessRuleViolationException;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "loans")
public class Loan implements Serializable {

    private static final Map<LoanStatus, Set<LoanStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(LoanStatus.class);
    static {
        ALLOWED_TRANSITIONS.put(LoanStatus.PENDING, EnumSet.of(LoanStatus.APPROVED, LoanStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(LoanStatus.APPROVED, EnumSet.of(LoanStatus.ACTIVE));
        ALLOWED_TRANSITIONS.put(LoanStatus.ACTIVE, EnumSet.of(LoanStatus.OVERDUE, LoanStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(LoanStatus.OVERDUE, EnumSet.of(LoanStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(LoanStatus.REJECTED, EnumSet.noneOf(LoanStatus.class));
        ALLOWED_TRANSITIONS.put(LoanStatus.CLOSED, EnumSet.noneOf(LoanStatus.class));
    }

    public static final BigDecimal FLAT_INTEREST_RATE = new BigDecimal("10.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @NotNull
    @Column(name = "principal", nullable = false, precision = 15, scale = 2)
    private BigDecimal principal;

    @NotNull
    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @NotNull
    @Column(name = "total_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDue;

    @NotNull
    @Column(name = "amount_repaid", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountRepaid = BigDecimal.ZERO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LoanStatus status = LoanStatus.PENDING;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    protected Loan() {
    }

    public Loan(Member member, BigDecimal principal, BigDecimal interestRatePercent, BigDecimal totalDue) {
        this.member = member;
        this.principal = principal;
        this.interestRate = interestRatePercent;
        this.totalDue = totalDue;
        this.amountRepaid = BigDecimal.ZERO;
        this.status = LoanStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        this.appliedAt = LocalDateTime.now();
    }

    public void transitionTo(LoanStatus newStatus) {
        Set<LoanStatus> allowed = ALLOWED_TRANSITIONS.get(this.status);
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new BusinessRuleViolationException(
                    "Cannot transition loan from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
    }

    public void applyRepayment(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Repayment amount must be positive.");
        }
        BigDecimal remaining = getRemainingBalance();
        if (amount.compareTo(remaining) > 0) {
            throw new BusinessRuleViolationException(
                    "Repayment of " + amount + " exceeds remaining loan balance of " + remaining);
        }
        this.amountRepaid = this.amountRepaid.add(amount);
        if (getRemainingBalance().compareTo(BigDecimal.ZERO) == 0) {
            transitionTo(LoanStatus.CLOSED);
        }
    }

    public void reject(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required.");
        }
        transitionTo(LoanStatus.REJECTED);
        this.rejectionReason = reason;
    }

    public String getRejectionReason() { return rejectionReason; }

    public BigDecimal getRemainingBalance() {
        return this.totalDue.subtract(this.amountRepaid);
    }

    public boolean isActiveOrOverdue() {
        return this.status == LoanStatus.ACTIVE || this.status == LoanStatus.OVERDUE;
    }


    public Long getId() { return id; }
    public Member getMember() { return member; }
    public BigDecimal getPrincipal() { return principal; }
    public BigDecimal getInterestRate() { return interestRate; }
    public BigDecimal getTotalDue() { return totalDue; }
    public BigDecimal getAmountRepaid() { return amountRepaid; }
    public LoanStatus getStatus() { return status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Integer getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Loan)) return false;
        Loan loan = (Loan) o;
        return id != null && id.equals(loan.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Loan{id=" + id + ", status=" + status + ", totalDue=" + totalDue + '}';
    }
}
