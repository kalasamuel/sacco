package com.pahappa.sacco.bean;

import com.pahappa.sacco.entity.Loan;
import com.pahappa.sacco.entity.Role;
import com.pahappa.sacco.exception.BusinessRuleViolationException;
import com.pahappa.sacco.exception.UnauthorizedException;
import com.pahappa.sacco.service.LoanService;
import com.pahappa.sacco.service.SavingsService;
import com.pahappa.sacco.util.FacesMessageUtil;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Named("loanApplicationBean")
@ViewScoped
public class LoanApplicationBean implements Serializable {

    @Inject
    private LoanService loanService;
    @Inject
    private SavingsService savingsService;
    @Inject
    private CurrentUserBean currentUser;

    private Long targetMemberId;
    private BigDecimal principal;
    private BigDecimal maxEligible;
    private List<Loan> loanHistory;

    public void applyForLoan() {
        try {
            Long memberId = resolveMemberId();
            loanService.applyForLoan(memberId, principal, currentUser.getUser());
            FacesMessageUtil.addInfo("Loan application submitted successfully and is pending review.");
            principal = null;
            loadHistory(memberId);
        } catch (UnauthorizedException e) {
            FacesMessageUtil.addError("You are not permitted to apply for a loan.");
        } catch (BusinessRuleViolationException e) {
            FacesMessageUtil.addError(e.getMessage());
        }
    }

    public void loadHistory(Long memberId) {
        this.targetMemberId = memberId;
        this.loanHistory = loanService.getLoansForMember(memberId);
        this.maxEligible = savingsService.getMaxLoanEligibility(memberId);
    }

    private Long resolveMemberId() {
        if (currentUser.getRole() == Role.MEMBER) {
            return currentUser.getUser().getMemberProfile().getId();
        }
        return targetMemberId;
    }

    public Long getTargetMemberId() { return targetMemberId; }
    public void setTargetMemberId(Long targetMemberId) { this.targetMemberId = targetMemberId; }
    public BigDecimal getPrincipal() { return principal; }
    public void setPrincipal(BigDecimal principal) { this.principal = principal; }
    public BigDecimal getMaxEligible() { return maxEligible; }
    public List<Loan> getLoanHistory() { return loanHistory; }
}
