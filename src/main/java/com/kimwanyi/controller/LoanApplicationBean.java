package com.kimwanyi.controller;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import com.kimwanyi.util.FacesMessageUtil;
import com.kimwanyi.dao.MemberDAO;
import com.kimwanyi.dao.SavingsAccountDAO;
import com.kimwanyi.model.Loan;
import com.kimwanyi.model.Member;
import com.kimwanyi.model.SavingsAccount;
import com.kimwanyi.model.dto.forms.LoanApplicationForm;
import com.kimwanyi.model.enums.LoanStatus;
import com.kimwanyi.service.interfaces.LoanService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("loanApplicationBean")
@RequestScope
public class LoanApplicationBean {

    private final LoanService loanService;
    private final MemberDAO memberDAO;
    private final SavingsAccountDAO savingsAccountDAO;
    private final UserSessionBean userSessionBean;

    private Long memberId;
    private BigDecimal maxEligibleAmount = BigDecimal.ZERO;
    private boolean hasActiveLoan;
    private String latestLoanStatus;

    private BigDecimal principalAmount;
    private Integer termMonths;

    public LoanApplicationBean(LoanService loanService, MemberDAO memberDAO, SavingsAccountDAO savingsAccountDAO, UserSessionBean userSessionBean) {
        this.loanService = loanService;
        this.memberDAO = memberDAO;
        this.savingsAccountDAO = savingsAccountDAO;
        this.userSessionBean = userSessionBean;
    }

    @PostConstruct
    public void init() {
        Member member = memberDAO.findByUserAccountId(userSessionBean.getLoggedInUser().getId())
                .orElseThrow(() -> new IllegalStateException("No member found for the logged-in user"));
        memberId = member.getId();

        SavingsAccount savingsAccount = savingsAccountDAO.findByMemberId(memberId).orElse(null);
        BigDecimal balance = savingsAccount != null ? savingsAccount.getBalance() : BigDecimal.ZERO;
        maxEligibleAmount = balance.multiply(new BigDecimal("3"));

        List<Loan> memberLoans = loanService.getLoansByMember(memberId);
        Loan activeLoan = memberLoans.stream().filter(loan ->
                loan.getStatus() == LoanStatus.PENDING || loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.OVERDUE)
                .findFirst().orElse(null);

        if (activeLoan != null) {
            hasActiveLoan = true;
            latestLoanStatus = activeLoan.getStatus().name();
        } else {
            hasActiveLoan = false;
        }
    }

    public String apply() {
        try {
            LoanApplicationForm form = new LoanApplicationForm();
            form.setMemberId(memberId);
            form.setPrincipalAmount(principalAmount);
            form.setTermMonths(termMonths);

            loanService.applyLoan(form);

            FacesMessageUtil.addInfoMessage("Loan application submitted successfully");
            return "/loans/applications?faces-redirect=true";
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
            return null;
        }
    }

    public BigDecimal getMaxEligibleAmount() {
        return maxEligibleAmount;
    }

    public boolean isHasActiveLoan() {
        return hasActiveLoan;
    }

    public String getLatestLoanStatus() {
        return latestLoanStatus;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public Integer getTermMonths() {
        return termMonths;
    }

    public void setTermMonths(Integer termMonths) {
        this.termMonths = termMonths;
    }
}
