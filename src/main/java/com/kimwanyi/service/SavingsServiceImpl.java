package com.kimwanyi.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import com.kimwanyi.exception.ResourceNotFoundException;
import com.kimwanyi.util.converter.SavingsAccountConverter;
import com.kimwanyi.util.converter.SavingsTransactionConverter;
import com.kimwanyi.dao.SavingsAccountDAO;
import com.kimwanyi.dao.SavingsTransactionDAO;
import com.kimwanyi.model.SavingsAccount;
import com.kimwanyi.model.SavingsTransaction;
import com.kimwanyi.model.dto.SavingsAccountDto;
import com.kimwanyi.model.dto.SavingsTransactionDto;
import com.kimwanyi.model.dto.forms.DepositForm;
import com.kimwanyi.model.dto.forms.InternalTransferForm;
import com.kimwanyi.model.dto.forms.WithdrawalForm;
import com.kimwanyi.model.enums.NotificationType;
import com.kimwanyi.model.enums.TransactionType;
import com.kimwanyi.model.enums.MemberStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.YearMonth;

import com.kimwanyi.service.interfaces.SavingsService;
import com.kimwanyi.strategy.BalanceDefault;
import com.kimwanyi.strategy.SavingsInterest;
import com.kimwanyi.service.interfaces.NotificationService;
import com.kimwanyi.service.interfaces.AuditLogService;

@Service
public class SavingsServiceImpl implements SavingsService {

    private static final BigDecimal MINIMUM_BALANCE = new BigDecimal("20000.00");

    private final SavingsAccountDAO savingsAccountDAO;
    private final SavingsTransactionDAO savingsTransactionDAO;
    private final SavingsAccountConverter savingsAccountConverter;
    private final SavingsTransactionConverter savingsTransactionConverter;
    private final BalanceDefault withdrawalPolicy;
    private final NotificationService notificationService;
    private final SavingsInterest savingsInterestCalculator;
    private final AuditLogService auditLogService;

    public SavingsServiceImpl(
            SavingsAccountDAO savingsAccountDAO,
            SavingsTransactionDAO savingsTransactionDAO,
            SavingsAccountConverter savingsAccountConverter,
            SavingsTransactionConverter savingsTransactionConverter,
            BalanceDefault withdrawalPolicy,
            NotificationService notificationService,
            SavingsInterest savingsInterestCalculator,
            AuditLogService auditLogService
    ) {
        this.savingsAccountDAO = savingsAccountDAO;
        this.savingsTransactionDAO = savingsTransactionDAO;
        this.savingsAccountConverter = savingsAccountConverter;
        this.savingsTransactionConverter = savingsTransactionConverter;
        this.withdrawalPolicy = withdrawalPolicy;
        this.notificationService = notificationService;
        this.savingsInterestCalculator = savingsInterestCalculator;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public SavingsTransactionDto deposit(DepositForm form) {
        if (form.getAmount() == null || form.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }
        if (form.getSavingsAccountId() == null) {
            throw new IllegalArgumentException("Savings account is required for a deposit");
        }

        SavingsAccount account = savingsAccountDAO.findById(form.getSavingsAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found"));

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(form.getAmount());

        account.setBalance(balanceAfter);
        savingsAccountDAO.save(account);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setSavingsAccount(account);
        tx.setReference("DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setType(TransactionType.DEPOSIT);
        tx.setAmount(form.getAmount());
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceAfter);
        tx.setDescription("Member deposit");
        tx.setCreatedAt(LocalDateTime.now());
        savingsTransactionDAO.save(tx);
        auditLogService.record(account.getMember() == null ? null : account.getMember().getUserAccount(),
                com.kimwanyi.model.enums.AuditAction.DEPOSIT_PROCESSED,
                "SavingsTransaction", tx.getId(), tx.getReference() + " UGX " + form.getAmount());

        notificationService.notify(account.getMember().getUserAccount(), NotificationType.DEPOSIT,
                "Deposit Received", "UGX " + form.getAmount() + " deposited. New balance: UGX " + balanceAfter + ".");

        return savingsTransactionConverter.toDto(tx);
    }

    @Override
    @Transactional
    public SavingsTransactionDto withdraw(WithdrawalForm form) {
        SavingsAccount account = savingsAccountDAO.findById(form.getSavingsAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found"));

        BigDecimal balanceBefore = account.getBalance();

        if (!withdrawalPolicy.isWithdrawalAllowed(balanceBefore, form.getAmount())) {
            throw new IllegalArgumentException(
                    "Withdrawal not allowed: amount must be positive and leave at least UGX 20,000 in the account.");
        }

        BigDecimal balanceAfter = balanceBefore.subtract(form.getAmount());

        account.setBalance(balanceAfter);
        savingsAccountDAO.save(account);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setSavingsAccount(account);
        tx.setReference("WTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setType(TransactionType.WITHDRAW);
        tx.setAmount(form.getAmount());
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceAfter);
        tx.setDescription(form.getDescription() != null && !form.getDescription().isBlank()
                ? form.getDescription().trim() : "Member withdrawal");
        tx.setCreatedAt(LocalDateTime.now());
        savingsTransactionDAO.save(tx);
        auditLogService.record(account.getMember() == null ? null : account.getMember().getUserAccount(),
                com.kimwanyi.model.enums.AuditAction.WITHDRAWAL_PROCESSED,
                "SavingsTransaction", tx.getId(), tx.getReference() + " UGX " + form.getAmount());

        notificationService.notify(account.getMember().getUserAccount(), NotificationType.WITHDRAWAL,
                "Withdrawal Processed", "UGX " + form.getAmount() + " withdrawn. New balance: UGX " + balanceAfter + ".");

        return savingsTransactionConverter.toDto(tx);
    }

    @Override
    public SavingsAccountDto getAccountById(Long id) {
        SavingsAccount account = savingsAccountDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found with ID: " + id));
        return savingsAccountConverter.toDto(account);
    }

    @Override
    public List<SavingsTransactionDto> getTransactionHistory(Long savingsAccountId) {
        return savingsTransactionDAO.findBySavingsAccountId(savingsAccountId).stream()
                .sorted(Comparator.comparing(com.kimwanyi.model.SavingsTransaction::getCreatedAt).reversed())
                .map(savingsTransactionConverter::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void transfer(InternalTransferForm form) {
        if (form.getAmount() == null || form.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        SavingsAccount fromAccount = savingsAccountDAO.findById(form.getFromAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Your savings account was not found"));

        SavingsAccount toAccount = savingsAccountDAO.findByAccountNumber(form.getToAccountNumber().trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Recipient account number not found: " + form.getToAccountNumber()));

        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new IllegalArgumentException("You cannot transfer to your own account");
        }
        if (fromAccount.getMember() == null || fromAccount.getMember().getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalArgumentException("Only an active member can make an internal transfer");
        }
        if (toAccount.getMember() == null || toAccount.getMember().getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalArgumentException("The recipient must be an active Kimwanyi SACCO member");
        }

        BigDecimal transferAmount = form.getAmount();
        BigDecimal fromBalanceAfter = fromAccount.getBalance().subtract(transferAmount);

        if (fromBalanceAfter.compareTo(MINIMUM_BALANCE) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient balance. Transfer would leave your account below the minimum balance of UGX 20,000. " +
                    "Available for transfer: UGX " + fromAccount.getBalance().subtract(MINIMUM_BALANCE).toPlainString()
            );
        }

        BigDecimal toBalanceAfter = toAccount.getBalance().add(transferAmount);
        String refPair = "TXF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String description = form.getDescription() != null && !form.getDescription().isBlank()
                ? form.getDescription().trim()
                : "Internal transfer";
        LocalDateTime now = LocalDateTime.now();

        // Debit sender
        fromAccount.setBalance(fromBalanceAfter);
        savingsAccountDAO.save(fromAccount);

        SavingsTransaction outTx = new SavingsTransaction();
        outTx.setSavingsAccount(fromAccount);
        outTx.setReference(refPair + "-OUT");
        outTx.setType(TransactionType.TRANSFER_OUT);
        outTx.setAmount(transferAmount);
        outTx.setBalanceBefore(fromAccount.getBalance().add(transferAmount));
        outTx.setBalanceAfter(fromBalanceAfter);
        outTx.setDescription(description + " → " + toAccount.getAccountNumber());
        savingsTransactionDAO.save(outTx);

        // Credit receiver
        toAccount.setBalance(toBalanceAfter);
        savingsAccountDAO.save(toAccount);

        SavingsTransaction inTx = new SavingsTransaction();
        inTx.setSavingsAccount(toAccount);
        inTx.setReference(refPair + "-IN");
        inTx.setType(TransactionType.TRANSFER_IN);
        inTx.setAmount(transferAmount);
        inTx.setBalanceBefore(toAccount.getBalance().subtract(transferAmount));
        inTx.setBalanceAfter(toBalanceAfter);
        inTx.setDescription(description + " ← " + fromAccount.getAccountNumber());
        savingsTransactionDAO.save(inTx);
        auditLogService.record(fromAccount.getMember() == null ? null : fromAccount.getMember().getUserAccount(),
                com.kimwanyi.model.enums.AuditAction.INTERNAL_TRANSFER_COMPLETED,
                "SavingsTransaction", outTx.getId(), refPair + " UGX " + transferAmount);
    }

    @Override
    @Transactional
    public int applyMonthlyInterest(YearMonth month) {
        if (month == null) throw new IllegalArgumentException("Interest month is required");
        int posted = 0;
        for (SavingsAccount account : savingsAccountDAO.findAllWithMember()) {
            String reference = "INT-" + month + "-" + account.getAccountNumber();
            if (savingsTransactionDAO.existsByReference(reference)) continue;
            BigDecimal interest = savingsInterestCalculator.calculateInterest(account.getBalance());
            if (interest.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal before = account.getBalance();
            BigDecimal after = before.add(interest);
            account.setBalance(after);
            savingsAccountDAO.save(account);
            SavingsTransaction transaction = new SavingsTransaction();
            transaction.setSavingsAccount(account);
            transaction.setReference(reference);
            transaction.setType(TransactionType.INTEREST);
            transaction.setAmount(interest);
            transaction.setBalanceBefore(before);
            transaction.setBalanceAfter(after);
            transaction.setDescription("Savings interest for " + month);
            savingsTransactionDAO.save(transaction);
            posted++;
        }
        return posted;
    }

    @Scheduled(cron = "0 5 0 1 * *")
    @Transactional
    public void postPreviousMonthInterest() {
        applyMonthlyInterest(YearMonth.now().minusMonths(1));
    }
}
