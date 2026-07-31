package com.kimwanyi.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import com.kimwanyi.exception.ResourceNotFoundException;
import com.kimwanyi.dao.MemberDAO;
import com.kimwanyi.dao.SavingsAccountDAO;
import com.kimwanyi.dao.SavingsTransactionDAO;
import com.kimwanyi.model.Member;
import com.kimwanyi.model.SavingsAccount;
import com.kimwanyi.model.SavingsTransaction;
import com.kimwanyi.model.dto.AccountStatementDto;
import com.kimwanyi.model.dto.StatementEntryDto;
import com.kimwanyi.model.enums.TransactionType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kimwanyi.service.interfaces.StatementService;


@Service
public class StatementServiceImpl implements StatementService {
    private final MemberDAO memberDAO;
    private final SavingsAccountDAO accountDAO;
    private final SavingsTransactionDAO transactionDAO;

    public StatementServiceImpl(MemberDAO memberDAO, SavingsAccountDAO accountDAO,
                                SavingsTransactionDAO transactionDAO) {
        this.memberDAO = memberDAO;
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountStatementDto generateForMember(Long userAccountId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("A valid statement date range is required");
        }
        Member member = memberDAO.findByUserAccountId(userAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Member profile not found"));
        SavingsAccount account = accountDAO.findByMemberId(member.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found"));
        List<SavingsTransaction> transactions = transactionDAO
                .findStatementTransactions(account.getId(), fromDate.atStartOfDay(), toDate.atTime(LocalTime.MAX));

        AccountStatementDto statement = new AccountStatementDto();
        statement.setMemberName(member.getUserAccount().getFirstName() + " " + member.getUserAccount().getLastName());
        statement.setMembershipNumber(member.getMembershipNumber());
        statement.setAccountNumber(account.getAccountNumber());
        statement.setFromDate(fromDate);
        statement.setToDate(toDate);
        BigDecimal opening = transactionDAO
                .findTopBySavingsAccountIdAndCreatedAtBeforeOrderByCreatedAtDescIdDesc(account.getId(), fromDate.atStartOfDay())
                .map(SavingsTransaction::getBalanceAfter)
                .orElse(BigDecimal.ZERO);
        statement.setOpeningBalance(opening);
        statement.setClosingBalance(transactions.isEmpty() ? opening
                : transactions.get(transactions.size() - 1).getBalanceAfter());
        statement.setEntries(transactions.stream().map(this::toEntry).toList());
        return statement;
    }

    private StatementEntryDto toEntry(SavingsTransaction transaction) {
        StatementEntryDto entry = new StatementEntryDto();
        entry.setDate(transaction.getCreatedAt());
        entry.setReference(transaction.getReference());
        entry.setDescription(transaction.getDescription());
        entry.setType(transaction.getType().name());
        boolean debit = transaction.getType() == TransactionType.WITHDRAW
                || transaction.getType() == TransactionType.TRANSFER_OUT;
        if (debit) entry.setDebit(transaction.getAmount()); else entry.setCredit(transaction.getAmount());
        entry.setBalance(transaction.getBalanceAfter());
        return entry;
    }
}
