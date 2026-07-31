package com.kimwanyi.controller;

import com.kimwanyi.dao.SavingsAccountDAO;

import jakarta.annotation.PostConstruct;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import com.kimwanyi.util.FacesMessageUtil;
import com.kimwanyi.model.SavingsAccount;
import com.kimwanyi.model.dto.forms.DepositForm;
import com.kimwanyi.model.dto.forms.WithdrawalForm;
import com.kimwanyi.service.interfaces.SavingsService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component("adminSavingsBean")
@SessionScope
public class AdminSavingsBean implements Serializable {

    private final SavingsAccountDAO savingsAccountDAO;
    private final SavingsService savingsService;

    private List<SavingsAccount> accounts;
    private SavingsAccount selectedAccount;
    
    private BigDecimal amount;
    private String description;
    
    // Type of transaction currently being modal-ed ("DEPOSIT" or "WITHDRAW")
    private String transactionType;

    public AdminSavingsBean(SavingsAccountDAO savingsAccountDAO, SavingsService savingsService) {
        this.savingsAccountDAO = savingsAccountDAO;
        this.savingsService = savingsService;
    }

    @PostConstruct
    public void init() {
        refreshAccounts();
    }

    public void refreshAccounts() {
        this.accounts = savingsAccountDAO.findAllWithMember();
    }

    public void prepareTransaction(SavingsAccount account, String type) {
        this.selectedAccount = account;
        this.transactionType = type;
        this.amount = null;
        this.description = null;
    }

    public void processTransaction() {
        if (selectedAccount == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            FacesMessageUtil.addErrorMessage("Invalid amount specified.");
            return;
        }

        try {
            if ("DEPOSIT".equals(transactionType)) {
                DepositForm form = new DepositForm();
                form.setSavingsAccountId(selectedAccount.getId());
                form.setAmount(amount);
                form.setDescription(description != null && !description.isBlank() ? description : "Admin Cash Deposit");
                savingsService.deposit(form);
                FacesMessageUtil.addInfoMessage("Successfully deposited UGX " + amount);
            } else if ("WITHDRAW".equals(transactionType)) {
                WithdrawalForm form = new WithdrawalForm();
                form.setSavingsAccountId(selectedAccount.getId());
                form.setAmount(amount);
                form.setDescription(description != null && !description.isBlank() ? description : "Admin Cash Withdrawal");
                savingsService.withdraw(form);
                FacesMessageUtil.addInfoMessage("Successfully withdrawn UGX " + amount);
            }
            refreshAccounts();
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
        }
    }

    // Getters and Setters
    public List<SavingsAccount> getAccounts() { return accounts; }
    public SavingsAccount getSelectedAccount() { return selectedAccount; }
    public void setSelectedAccount(SavingsAccount selectedAccount) { this.selectedAccount = selectedAccount; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
}
