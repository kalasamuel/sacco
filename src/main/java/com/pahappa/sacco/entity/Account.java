package com.pahappa.sacco.entity;

import com.pahappa.sacco.exception.InsufficientFundsException;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @NotNull
    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @NotNull
    @Column(name = "min_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal minBalance = new BigDecimal("20000.00");

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Account() {
    }

    public Account(Member member) {
        this.member = member;
        this.balance = BigDecimal.ZERO;
        this.minBalance = new BigDecimal("20000.00");
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive.");
        }
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive.");
        }
        BigDecimal resultingBalance = this.balance.subtract(amount);
        if (resultingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException(
                    "Withdrawal of " + amount + " exceeds available balance of " + this.balance);
        }
        if (resultingBalance.compareTo(this.minBalance) < 0) {
            throw new InsufficientFundsException(
                    "Withdrawal would breach the required minimum balance of " + this.minBalance);
        }
        this.balance = resultingBalance;
    }

    public BigDecimal maxLoanAmount() {
        return this.balance.multiply(BigDecimal.valueOf(3));
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getMinBalance() { return minBalance; }
    public void setMinBalance(BigDecimal minBalance) { this.minBalance = minBalance; }
    public Integer getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        Account account = (Account) o;
        return id != null && id.equals(account.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Account{id=" + id + ", balance=" + balance + '}';
    }
}
