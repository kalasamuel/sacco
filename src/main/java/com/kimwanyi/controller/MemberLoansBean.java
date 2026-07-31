package com.kimwanyi.controller;

import jakarta.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import com.kimwanyi.util.FacesMessageUtil;
import com.kimwanyi.dao.MemberDAO;
import com.kimwanyi.model.Loan;
import com.kimwanyi.model.Member;
import com.kimwanyi.service.interfaces.LoanService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("memberLoansBean")
@RequestScope
public class MemberLoansBean {

    private final LoanService loanService;
    private final MemberDAO memberDAO;
    private final UserSessionBean userSessionBean;

    private List<Loan> loans = List.of();

    public MemberLoansBean(LoanService loanService, MemberDAO memberDAO, UserSessionBean userSessionBean) {
        this.loanService = loanService;
        this.memberDAO = memberDAO;
        this.userSessionBean = userSessionBean;
    }

    @PostConstruct
    public void init() {
        try {
            Member member = memberDAO.findByUserAccountId(userSessionBean.getLoggedInUser().getId())
                    .orElseThrow(() -> new IllegalStateException("No member found for the logged-in user"));

            loans = loanService.getLoansByMember(member.getId()).stream()
                    .sorted(Comparator.comparing(Loan::getApplicationDate).reversed())
                    .toList();
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage("Failed to load your loans: " + e.getMessage());
        }
    }

    public String badgeClass(Loan loan) {
        return switch (loan.getStatus()) {
            case PENDING -> "badge-yellow";
            case ACTIVE -> "badge-blue";
            case FULLY_REPAID -> "badge-green";
            case REJECTED, OVERDUE -> "badge-red";
            case APPROVED -> "badge-gray";
        };
    }

    public List<Loan> getLoans() {
        return loans;
    }
}
