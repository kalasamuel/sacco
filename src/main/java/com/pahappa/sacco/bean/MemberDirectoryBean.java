package com.pahappa.sacco.bean;

import com.pahappa.sacco.entity.Loan;
import com.pahappa.sacco.entity.Member;
import com.pahappa.sacco.exception.BusinessRuleViolationException;
import com.pahappa.sacco.exception.UnauthorizedException;
import com.pahappa.sacco.service.LoanService;
import com.pahappa.sacco.service.MemberService;
import com.pahappa.sacco.service.SavingsService;
import com.pahappa.sacco.util.FacesMessageUtil;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Named("memberDirectoryBean")
@ViewScoped
public class MemberDirectoryBean implements Serializable {

    @Inject
    private MemberService memberService;
    @Inject
    private SavingsService savingsService;
    @Inject
    private LoanService loanService;
    @Inject
    private CurrentUserBean currentUser;

    private List<Member> allMembers;
    private List<Member> filteredMembers;

    private String statusFilter = "ALL";      // ALL | ACTIVE | DEACTIVATED
    private String loanStatusFilter = "ALL";  // ALL | NONE | ACTIVE | OVERDUE | PENDING
    private String searchText;

    private Long memberIdPendingDeactivation;
    private boolean deactivationBlocked;

    @PostConstruct
    public void init() {
        allMembers = memberService.getAllMembers();
        applyFilters();
    }

    public void applyFilters() {
        filteredMembers = allMembers.stream()
                .filter(this::matchesStatus)
                .filter(this::matchesLoanStatus)
                .filter(this::matchesSearch)
                .collect(Collectors.toList());
    }

    private boolean matchesStatus(Member m) {
        switch (statusFilter) {
            case "ACTIVE": return Boolean.TRUE.equals(m.getActive());
            case "DEACTIVATED": return !Boolean.TRUE.equals(m.getActive());
            default: return true;
        }
    }

    private boolean matchesLoanStatus(Member m) {
        if ("ALL".equals(loanStatusFilter)) {
            return true;
        }
        List<Loan> loans = loanService.getLoansForMember(m.getId());
        if ("NONE".equals(loanStatusFilter)) {
            return loans.stream().noneMatch(Loan::isActiveOrOverdue);
        }
        return loans.stream().anyMatch(l -> l.getStatus().name().equals(loanStatusFilter));
    }

    private boolean matchesSearch(Member m) {
        if (searchText == null || searchText.isBlank()) {
            return true;
        }
        String q = searchText.toLowerCase();
        return m.getFullName().toLowerCase().contains(q)
                || m.getMembershipNumber().toLowerCase().contains(q)
                || m.getNationalId().toLowerCase().contains(q);
    }

    public BigDecimal balanceOf(Member m) {
        try {
            if (m == null || m.getId() == null) {
                return BigDecimal.ZERO;
            }
            return savingsService.getBalance(m.getId());
        } catch (BusinessRuleViolationException e) {
            // Member has no savings account yet — treat as zero balance for UI display.
            return BigDecimal.ZERO;
        }
    }

    public String loanStatusLabelOf(Member m) {
        return loanService.getLoansForMember(m.getId()).stream()
                .filter(Loan::isActiveOrOverdue)
                .findFirst()
                .map(l -> l.getStatus().toString())
                .orElse("None");
    }

    // Opens the deactivation confirmation dialog, pre-checking (advisory only) whether it will be blocked.
    public void prepareDeactivate(Long memberId) {
        this.memberIdPendingDeactivation = memberId;
        this.deactivationBlocked = memberService.hasBlockingActiveLoan(memberId);
    }

    public void confirmDeactivate() {
        try {
            memberService.deactivateMember(memberIdPendingDeactivation, currentUser.getUser());
            FacesMessageUtil.addInfo("Member deactivated.");
            init(); // reload full list so the status change is reflected
        } catch (UnauthorizedException e) {
            FacesMessageUtil.addError("Only an admin can deactivate a member.");
        } catch (BusinessRuleViolationException e) {
            FacesMessageUtil.addError(e.getMessage());
        }
    }

    public List<Member> getFilteredMembers() { return filteredMembers; }
    public String getStatusFilter() { return statusFilter; }
    public void setStatusFilter(String statusFilter) { this.statusFilter = statusFilter; }
    public String getLoanStatusFilter() { return loanStatusFilter; }
    public void setLoanStatusFilter(String loanStatusFilter) { this.loanStatusFilter = loanStatusFilter; }
    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }
    public Long getMemberIdPendingDeactivation() { return memberIdPendingDeactivation; }
    public boolean isDeactivationBlocked() { return deactivationBlocked; }
}
