package com.pahappa.sacco.bean;

import com.pahappa.sacco.entity.Loan;
import com.pahappa.sacco.entity.LoanStatus;
import com.pahappa.sacco.exception.BusinessRuleViolationException;
import com.pahappa.sacco.exception.UnauthorizedException;
import com.pahappa.sacco.service.LoanService;
import com.pahappa.sacco.util.FacesMessageUtil;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

//  Backs the admin loan approval screen (/app/admin/). 
// fix for "when a loan officer is away, nobody else can
//  tell what stage an application is at" — pendingLoans and overdueLoans
//  are live queries
@Named("loanReviewBean")
@ViewScoped
public class LoanReviewBean implements Serializable {

    @Inject
    private LoanService loanService;
    @Inject
    private CurrentUserBean currentUser;

    private List<Loan> pendingLoans;
    private List<Loan> overdueLoans;
    private String rejectionReason;
    private Long loanIdPendingRejection;

    public void loadPending() {
        pendingLoans = loanService.getLoansByStatus(LoanStatus.PENDING);
    }

    public void loadOverdue() {
        overdueLoans = loanService.getLoansByStatus(LoanStatus.OVERDUE);
    }

    public void approve(Long loanId) {
        try {
            loanService.approveLoan(loanId, currentUser.getUser());
            FacesMessageUtil.addInfo("Loan approved and activated.");
            loadPending();
        } catch (UnauthorizedException e) {
            FacesMessageUtil.addError("Only an admin can approve loan applications.");
        } catch (BusinessRuleViolationException e) {
            FacesMessageUtil.addError(e.getMessage());
        }
    }

    //Called when the admin clicks "Reject" on a row — opens the shared reason-prompt dialog for that specific loan.
    public void prepareReject(Long loanId) {
        this.loanIdPendingRejection = loanId;
        this.rejectionReason = null;
    }

    public void confirmReject() {
        try {
            loanService.rejectLoan(loanIdPendingRejection, rejectionReason, currentUser.getUser());
            FacesMessageUtil.addInfo("Loan application rejected.");
            rejectionReason = null;
            loanIdPendingRejection = null;
            loadPending();
        } catch (UnauthorizedException e) {
            FacesMessageUtil.addError("Only an admin can reject loan applications.");
        } catch (IllegalArgumentException e) {
            FacesMessageUtil.addError("A rejection reason is required.");
        } catch (BusinessRuleViolationException e) {
            FacesMessageUtil.addError(e.getMessage());
        }
    }

    //Manually triggered sweep
    public void runOverdueSweep() {
        try {
            int count = loanService.markOverdueLoans(currentUser.getUser());
            FacesMessageUtil.addInfo(count + " loan(s) marked overdue.");
            loadOverdue();
        } catch (UnauthorizedException e) {
            FacesMessageUtil.addError("Only an admin can run the overdue sweep.");
        }
    }

    //Live eligibility snapshot for the Loan Desk validation panel (blueprint requirement)
    public LoanService.EligibilitySnapshot eligibilityOf(Loan loan) {
        return loanService.checkEligibility(loan);
    }

    //Repayment Ledger Sub-View (blueprint requirement) - schedule compliance / repayment history for any loan.
    private java.util.List<com.pahappa.sacco.entity.LoanRepayment> repaymentLedger;
    private Loan loanBeingViewed;

    public void viewRepaymentLedger(Loan loan) {
        this.loanBeingViewed = loan;
        this.repaymentLedger = loanService.getRepaymentHistory(loan.getId());
    }

    public java.util.List<com.pahappa.sacco.entity.LoanRepayment> getRepaymentLedger() { return repaymentLedger; }
    public Loan getLoanBeingViewed() { return loanBeingViewed; }

    public List<Loan> getPendingLoans() { return pendingLoans; }
    public List<Loan> getOverdueLoans() { return overdueLoans; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Long getLoanIdPendingRejection() { return loanIdPendingRejection; }
}
