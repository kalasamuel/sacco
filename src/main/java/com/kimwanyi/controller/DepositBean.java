package com.kimwanyi.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;

import java.math.BigDecimal;
import com.kimwanyi.util.FacesMessageUtil;
import com.kimwanyi.dao.MemberDAO;
import com.kimwanyi.dao.SavingsAccountDAO;
import com.kimwanyi.model.Member;
import com.kimwanyi.model.SavingsAccount;
import com.kimwanyi.model.dto.forms.DepositForm;
import com.kimwanyi.service.interfaces.SavingsService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component("depositBean")
@SessionScoped
public class DepositBean {

    private final SavingsService savingsService;
    private final MemberDAO memberDAO;
    private final SavingsAccountDAO savingsAccountDAO;
    private final UserSessionBean userSessionBean;
    private final InternalTransferBean internalTransferBean;

    private Long accountId;
    private BigDecimal amount;

    public DepositBean(
            SavingsService savingsService,
            MemberDAO memberDAO,
            SavingsAccountDAO savingsAccountDAO,
            UserSessionBean userSessionBean,
            InternalTransferBean internalTransferBean
    ) {
        this.savingsService = savingsService;
        this.memberDAO = memberDAO;
        this.savingsAccountDAO = savingsAccountDAO;
        this.userSessionBean = userSessionBean;
        this.internalTransferBean = internalTransferBean;
    }

    @PostConstruct
    public void init() {
        try {
            if (userSessionBean.getLoggedInUser() == null) return;
            Member member = memberDAO.findByUserAccountId(userSessionBean.getLoggedInUser().getId()).orElse(null);
            if (member == null) return;
            SavingsAccount account = savingsAccountDAO.findByMemberId(member.getId()).orElse(null);
            if (account != null) {
                this.accountId = account.getId();
            }
        } catch (Exception ignored) {
        }
    }

    public void deposit() {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            FacesMessageUtil.addErrorMessage("Please enter a valid deposit amount.");
            return;
        }

        try {
            resolveAccountId();
            if (accountId == null) {
                FacesMessageUtil.addErrorMessage("Your savings account could not be found. Please contact the SACCO administrator.");
                return;
            }

            DepositForm form = new DepositForm();
            form.setSavingsAccountId(accountId);
            form.setAmount(amount);

            savingsService.deposit(form);

            // Refresh other beans that depend on the balance
            internalTransferBean.refreshBalance();

            FacesMessageUtil.addInfoMessage("Deposit of UGX " + amount.toPlainString() + " was successful!");
            amount = null; // Clear amount for next time
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
        }
    }

    private void resolveAccountId() {
        if (userSessionBean.getLoggedInUser() == null) {
            accountId = null;
            return;
        }

        Member member = memberDAO.findByUserAccountId(userSessionBean.getLoggedInUser().getId())
                .orElse(null);
        if (member == null) {
            accountId = null;
            return;
        }

        accountId = savingsAccountDAO.findByMemberId(member.getId())
                .map(SavingsAccount::getId)
                .orElse(null);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
