package com.pahappa.sacco.bean;

import com.pahappa.sacco.entity.Loan;
import com.pahappa.sacco.entity.LoanRepayment;
import com.pahappa.sacco.exception.BusinessRuleViolationException;
import com.pahappa.sacco.exception.UnauthorizedException;
import com.pahappa.sacco.service.LoanService;
import com.pahappa.sacco.util.FacesMessageUtil;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Named("loanRepaymentBean")
@ViewScoped
public class LoanRepaymentBean implements Serializable {

    @Inject
    private LoanService loanService;
    @Inject
    private CurrentUserBean currentUser;

    private Long selectedLoanId;
    private BigDecimal amount;
    private List<LoanRepayment> repaymentHistory;

    public void loadLoan(Long loanId) {
        this.selectedLoanId = loanId;
        this.repaymentHistory = loanService.getRepaymentHistory(loanId);
    }

    public void repay() {
        try {
            loanService.repay(selectedLoanId, amount, currentUser.getUser());
            FacesMessageUtil.addInfo("Repayment of " + amount + " recorded.");
            amount = null;
            loadLoan(selectedLoanId);
        } catch (UnauthorizedException e) {
            FacesMessageUtil.addError("You are not permitted to record loan repayments.");
        } catch (BusinessRuleViolationException e) {
            FacesMessageUtil.addError(e.getMessage());
        }
    }

    public Long getSelectedLoanId() { return selectedLoanId; }
    public void setSelectedLoanId(Long selectedLoanId) { this.selectedLoanId = selectedLoanId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public List<LoanRepayment> getRepaymentHistory() { return repaymentHistory; }
}
