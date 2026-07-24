package com.pahappa.sacco.bean;

import com.pahappa.sacco.entity.Loan;
import com.pahappa.sacco.entity.LoanStatus;
import com.pahappa.sacco.entity.TransactionType;
import com.pahappa.sacco.service.LoanService;
import com.pahappa.sacco.service.MemberService;
import com.pahappa.sacco.service.SavingsService;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.pie.PieChartDataSet;
import org.primefaces.model.charts.pie.PieChartModel;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//Aggregates everything the admin dashboard needs into one place.
//answer to "how much money do we actually hold" and "who's overdue" without a week's delay
@Named("adminDashboardBean")
@ViewScoped
public class AdminDashboardBean implements Serializable {

    private static final int MONTHS_OF_HISTORY = 6;

    @Inject
    private MemberService memberService;
    @Inject
    private SavingsService savingsService;
    @Inject
    private LoanService loanService;
    @Inject
    private CurrentUserBean currentUser;

    private long activeMemberCount;
    private BigDecimal totalSavingsHeld;
    private Map<LoanStatus, Long> loanCounts;
    private List<Loan> overdueLoans;
    private BarChartModel cashFlowModel;
    private PieChartModel loanDistributionModel;

    @PostConstruct
    public void init() {
        activeMemberCount = memberService.getActiveMemberCount();
        totalSavingsHeld = savingsService.getTotalSavingsHeld(currentUser.getUser());
        loanCounts = loanService.getLoanStatusCounts(currentUser.getUser());
        overdueLoans = loanService.getLoansByStatus(LoanStatus.OVERDUE);
        buildCashFlowModel();
        buildLoanDistributionModel();
    }

 
    // Monthly Cash Inflow vs Outflow (blueprint requirement). Built from
    // TransactionDao.sumByTypeAndDateRange called once per
    // month per type over the trailing 6 months
    private void buildCashFlowModel() {
        List<String> monthLabels = new ArrayList<>();
        List<Number> deposits = new ArrayList<>();
        List<Number> withdrawals = new ArrayList<>();

        YearMonth current = YearMonth.now();
        for (int i = MONTHS_OF_HISTORY - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            LocalDateTime start = month.atDay(1).atStartOfDay();
            LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

            monthLabels.add(month.format(DateTimeFormatter.ofPattern("MMM yyyy")));
            deposits.add(savingsService.getTotalByTypeAndRange(TransactionType.DEPOSIT, start, end, currentUser.getUser()));
            withdrawals.add(savingsService.getTotalByTypeAndRange(TransactionType.WITHDRAWAL, start, end, currentUser.getUser()));
        }

        BarChartDataSet depositSet = new BarChartDataSet();
        depositSet.setLabel("Deposits");
        depositSet.setBackgroundColor("rgb(15, 122, 74)");   // success
        depositSet.setData(deposits);

        BarChartDataSet withdrawalSet = new BarChartDataSet();
        withdrawalSet.setLabel("Withdrawals");
        withdrawalSet.setBackgroundColor("rgb(178, 60, 31)"); // danger
        withdrawalSet.setData(withdrawals);

        ChartData data = new ChartData();
        data.addChartDataSet(depositSet);
        data.addChartDataSet(withdrawalSet);
        data.setLabels(monthLabels);

        cashFlowModel = new BarChartModel();
        cashFlowModel.setData(data);
    }

    // Loan Portfolio Distribution (blueprint requirement) — Active, Overdue, Closed (Repaid), Rejected, Pending.
    private void buildLoanDistributionModel() {
        List<Number> values = List.of(
                loanCounts.getOrDefault(LoanStatus.ACTIVE, 0L),
                loanCounts.getOrDefault(LoanStatus.OVERDUE, 0L),
                loanCounts.getOrDefault(LoanStatus.CLOSED, 0L),
                loanCounts.getOrDefault(LoanStatus.REJECTED, 0L),
                loanCounts.getOrDefault(LoanStatus.PENDING, 0L)
        );
        List<String> labels = List.of("Active", "Overdue", "Repaid (Closed)", "Rejected", "Pending");
        List<String> colors = List.of(
                "rgb(15, 122, 74)",   // Active — success green
                "rgb(178, 60, 31)",   // Overdue — danger red
                "rgb(30, 92, 72)",   // Repaid — primary-light
                "rgb(90, 106, 99)",   // Rejected — muted gray
                "rgb(184, 134, 11)"   // Pending — gold
        );

        PieChartDataSet dataSet = new PieChartDataSet();
        dataSet.setData(values);
        dataSet.setBackgroundColor(colors);

        ChartData data = new ChartData();
        data.addChartDataSet(dataSet);
        data.setLabels(labels);

        loanDistributionModel = new PieChartModel();
        loanDistributionModel.setData(data);
    }

    public long getActiveMemberCount() { return activeMemberCount; }
    public BigDecimal getTotalSavingsHeld() { return totalSavingsHeld; }
    public Map<LoanStatus, Long> getLoanCounts() { return loanCounts; }
    public List<Loan> getOverdueLoans() { return overdueLoans; }
    public BarChartModel getCashFlowModel() { return cashFlowModel; }
    public PieChartModel getLoanDistributionModel() { return loanDistributionModel; }

    public long getPendingCount() { return loanCounts.getOrDefault(LoanStatus.PENDING, 0L); }
    public long getActiveCount() { return loanCounts.getOrDefault(LoanStatus.ACTIVE, 0L); }
    public long getOverdueCount() { return loanCounts.getOrDefault(LoanStatus.OVERDUE, 0L); }
    public long getClosedCount() { return loanCounts.getOrDefault(LoanStatus.CLOSED, 0L); }
    public long getRejectedCount() { return loanCounts.getOrDefault(LoanStatus.REJECTED, 0L); }
}
