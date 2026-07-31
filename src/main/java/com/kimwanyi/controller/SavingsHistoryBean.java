package com.kimwanyi.controller;

import jakarta.annotation.PostConstruct;
import java.util.List;
import com.kimwanyi.util.FacesMessageUtil;
import com.kimwanyi.dao.MemberDAO;
import com.kimwanyi.dao.SavingsAccountDAO;
import com.kimwanyi.model.Member;
import com.kimwanyi.model.SavingsAccount;
import com.kimwanyi.model.dto.SavingsAccountDto;
import com.kimwanyi.model.dto.SavingsTransactionDto;
import com.kimwanyi.service.interfaces.SavingsService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("savingsHistoryBean")
@RequestScope
public class SavingsHistoryBean {

    private final SavingsService savingsService;
    private final MemberDAO memberDAO;
    private final SavingsAccountDAO savingsAccountDAO;
    private final UserSessionBean userSessionBean;

    private SavingsAccountDto account;
    private List<SavingsTransactionDto> transactions = List.of();

    public SavingsHistoryBean(
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
        try {
            Member member = memberDAO.findByUserAccountId(userSessionBean.getLoggedInUser().getId())
                    .orElseThrow(() -> new IllegalStateException("No member found for the logged-in user"));

            SavingsAccount savingsAccount = savingsAccountDAO.findByMemberId(member.getId())
                    .orElseThrow(() -> new IllegalStateException("No savings account found for member"));

            account = savingsService.getAccountById(savingsAccount.getId());
            transactions = savingsService.getTransactionHistory(savingsAccount.getId());
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage("Failed to load savings history: " + e.getMessage());
        }
    }

    public SavingsAccountDto getAccount() {
        return account;
    }

    public List<SavingsTransactionDto> getTransactions() {
        return transactions;
    }

    public List<SavingsTransactionDto> getRecentTransactions() {
        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }
        return transactions.subList(0, Math.min(5, transactions.size()));
    }

    public boolean globalFilterFunction(Object value, Object filter, java.util.Locale locale) {
        String filterText = (filter == null) ? null : filter.toString().trim().toLowerCase();
        if (filterText == null || filterText.isEmpty()) {
            return true;
        }

        SavingsTransactionDto tx = (SavingsTransactionDto) value;

        boolean matchesRef = tx.getReference() != null && tx.getReference().toLowerCase().contains(filterText);
        boolean matchesType = tx.getTransactionType() != null && tx.getTransactionType().toLowerCase().contains(filterText);
        boolean matchesDesc = tx.getDescription() != null && tx.getDescription().toLowerCase().contains(filterText);
        boolean matchesAmt = tx.getAmount() != null && tx.getAmount().toPlainString().contains(filterText);

        return matchesRef || matchesType || matchesDesc || matchesAmt;
    }
}
