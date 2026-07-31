package com.kimwanyi.service.interfaces;

import java.util.List;

import com.kimwanyi.model.dto.SavingsAccountDto;
import com.kimwanyi.model.dto.SavingsTransactionDto;
import com.kimwanyi.model.dto.forms.DepositForm;
import com.kimwanyi.model.dto.forms.InternalTransferForm;
import com.kimwanyi.model.dto.forms.WithdrawalForm;

import java.time.YearMonth;

public interface SavingsService {

    SavingsTransactionDto deposit(DepositForm form);

    SavingsTransactionDto withdraw(WithdrawalForm form);

    SavingsAccountDto getAccountById(Long id);

    List<SavingsTransactionDto> getTransactionHistory(Long savingsAccountId);

    void transfer(InternalTransferForm form);
    int applyMonthlyInterest(YearMonth month);
    void postPreviousMonthInterest();
}
