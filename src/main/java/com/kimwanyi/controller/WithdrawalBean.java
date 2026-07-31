package com.kimwanyi.controller;

import com.kimwanyi.model.dto.forms.WithdrawalForm;
import com.kimwanyi.service.interfaces.SavingsService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("withdrawalBean")
@RequestScope
public class WithdrawalBean {

    private final SavingsService savingsService;

    private WithdrawalForm withdrawalForm = new WithdrawalForm();

    public WithdrawalBean(SavingsService savingsService) {
        this.savingsService = savingsService;
    }

    public WithdrawalForm getWithdrawalForm() {
        return withdrawalForm;
    }

    public void setWithdrawalForm(WithdrawalForm withdrawalForm) {
        this.withdrawalForm = withdrawalForm;
    }

    public String withdraw() {
        return null;
    }
}
