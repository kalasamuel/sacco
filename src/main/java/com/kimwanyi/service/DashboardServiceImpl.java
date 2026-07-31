package com.kimwanyi.service;

import com.kimwanyi.service.interfaces.DashboardService;
import com.kimwanyi.service.interfaces.AuditLogService;

import java.math.BigDecimal;

import com.kimwanyi.dao.LoanDAO;
import com.kimwanyi.dao.MemberDAO;
import com.kimwanyi.dao.SavingsAccountDAO;
import com.kimwanyi.model.dto.DashboardSummaryDto;
import com.kimwanyi.model.enums.LoanStatus;
import com.kimwanyi.model.enums.MemberStatus;

import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final MemberDAO memberDAO;
    private final SavingsAccountDAO savingsAccountDAO;
    private final LoanDAO loanDAO;
    private final AuditLogService auditLogService;

    public DashboardServiceImpl(MemberDAO memberDAO, SavingsAccountDAO savingsAccountDAO, LoanDAO loanDAO, AuditLogService auditLogService) {
        this.memberDAO = memberDAO;
        this.savingsAccountDAO = savingsAccountDAO;
        this.loanDAO = loanDAO;
        this.auditLogService = auditLogService;
    }

    @Override
    public DashboardSummaryDto getSummary() {
        DashboardSummaryDto summary = new DashboardSummaryDto();

        try {
            summary.setTotalMembers(memberDAO.count());
            summary.setActiveMembers(memberDAO.countByStatus(MemberStatus.ACTIVE));
            summary.setInactiveMembers(memberDAO.countByStatus(MemberStatus.INACTIVE));
            summary.setSuspendedMembers(memberDAO.countByStatus(MemberStatus.SUSPENDED));
        } catch (Exception e) {
            // fallback defaults
        }

        try {
            BigDecimal totalSavings = savingsAccountDAO.sumAllBalances();
            summary.setTotalSavingsBalance(totalSavings != null ? totalSavings : java.math.BigDecimal.ZERO);
        } catch (Exception e) {
            summary.setTotalSavingsBalance(java.math.BigDecimal.ZERO);
        }
        
        try {
            summary.setPendingLoanCount(loanDAO.countByStatus(LoanStatus.PENDING));
            summary.setActiveLoanCount(loanDAO.countByStatus(LoanStatus.ACTIVE));
            BigDecimal totalLoans = loanDAO.sumOutstandingBalance();
            summary.setTotalOutstandingLoans(totalLoans != null ? totalLoans : java.math.BigDecimal.ZERO);
        } catch (Exception e) {
            summary.setTotalOutstandingLoans(java.math.BigDecimal.ZERO);
        }

        try {
            summary.setRecentActivity(auditLogService.findRecent(10));
        } catch (Exception e) {
            summary.setRecentActivity(java.util.Collections.emptyList());
        }

        // Pipeline extra stats
        try {
            long savers = savingsAccountDAO.findAll().stream()
                .filter(a -> a.getBalance() != null && a.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .count();
            summary.setMembersWithSavings(savers);
            summary.setFullyRepaidLoans(loanDAO.countByStatus(LoanStatus.FULLY_REPAID));
        } catch (Exception e) {
            summary.setMembersWithSavings(0);
            summary.setFullyRepaidLoans(0);
        }

        // Chart Data (Mocking past 6 months aggregate for now until transaction-level aggregates are built)
        java.time.YearMonth currentMonth = java.time.YearMonth.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM");
        
        java.util.List<String> labels = new java.util.ArrayList<>();
        java.util.List<BigDecimal> savingsData = new java.util.ArrayList<>();
        java.util.List<BigDecimal> loansData = new java.util.ArrayList<>();
        
        // We'll generate 6 months of data, trending towards the current real totals
        BigDecimal targetSavings = summary.getTotalSavingsBalance() != null ? summary.getTotalSavingsBalance() : BigDecimal.ZERO;
        BigDecimal targetLoans = summary.getTotalOutstandingLoans() != null ? summary.getTotalOutstandingLoans() : BigDecimal.ZERO;

        for (int i = 5; i >= 0; i--) {
            labels.add(currentMonth.minusMonths(i).format(formatter));
            // Simple linear curve for visualization
            double factor = (6 - i) / 6.0;
            savingsData.add(targetSavings.multiply(BigDecimal.valueOf(factor)));
            loansData.add(targetLoans.multiply(BigDecimal.valueOf(factor)));
        }

        summary.setChartLabels(labels);
        summary.setChartSavings(savingsData);
        summary.setChartLoans(loansData);

        return summary;
    }
}
