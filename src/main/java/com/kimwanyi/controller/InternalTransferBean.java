package com.kimwanyi.controller;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import com.kimwanyi.util.FacesMessageUtil;
import com.kimwanyi.dao.MemberDAO;
import com.kimwanyi.dao.SavingsAccountDAO;
import com.kimwanyi.model.Member;
import com.kimwanyi.model.SavingsAccount;
import com.kimwanyi.model.dto.forms.InternalTransferForm;
import com.kimwanyi.service.interfaces.SavingsService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component("internalTransferBean")
@SessionScope
public class InternalTransferBean {

    private final SavingsService savingsService;
    private final MemberDAO memberDAO;
    private final SavingsAccountDAO savingsAccountDAO;
    private final UserSessionBean userSessionBean;

    private Long myAccountId;
    private BigDecimal myBalance = BigDecimal.ZERO;
    private String toAccountNumber;
    private BigDecimal amount;
    private String description;

    public InternalTransferBean(
            SavingsService savingsService,
            MemberDAO memberDAO,
            SavingsAccountDAO savingsAccountDAO,
            UserSessionBean userSessionBean
    ) {
        this.savingsService = savingsService;
        this.memberDAO = memberDAO;
        this.savingsAccountDAO = savingsAccountDAO;
        this.userSessionBean = userSessionBean;
    }

    @PostConstruct
    public void init() {
        refreshBalance();
    }

    public void refreshBalance() {
        try {
            if (userSessionBean.getLoggedInUser() == null) return;
            Member member = memberDAO.findByUserAccountId(userSessionBean.getLoggedInUser().getId()).orElse(null);
            if (member == null) return;
            SavingsAccount account = savingsAccountDAO.findByMemberId(member.getId()).orElse(null);
            if (account == null) return;
            myAccountId = account.getId();
            myBalance = account.getBalance();
        } catch (Exception ignored) {
        }
    }

    /** Called by the transfer dialog submit button (AJAX). */
    public void transfer() {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            FacesMessageUtil.addErrorMessage("Please enter a valid transfer amount greater than zero.");
            return;
        }
        if (toAccountNumber == null || toAccountNumber.isBlank()) {
            FacesMessageUtil.addErrorMessage("Recipient account number is required.");
            return;
        }

        try {
            InternalTransferForm form = new InternalTransferForm();
            form.setFromAccountId(myAccountId);
            form.setToAccountNumber(toAccountNumber);
            form.setAmount(amount);
            form.setDescription(description);

            savingsService.transfer(form);
            refreshBalance();

            // Clear form after success
            toAccountNumber = null;
            amount = null;
            description = null;

            FacesMessageUtil.addInfoMessage("Transfer of UGX " + form.getAmount().toPlainString() + " to " + form.getToAccountNumber() + " was successful.");
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
        }
    }

    public BigDecimal getMaxTransferable() {
        BigDecimal minimum = new BigDecimal("20000.00");
        BigDecimal max = myBalance.subtract(minimum);
        return max.compareTo(BigDecimal.ZERO) > 0 ? max : BigDecimal.ZERO;
    }

    // Getters / setters
    public Long getMyAccountId() { return myAccountId; }
    public BigDecimal getMyBalance() { return myBalance; }

    public String getToAccountNumber() { return toAccountNumber; }
    public void setToAccountNumber(String toAccountNumber) { this.toAccountNumber = toAccountNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
