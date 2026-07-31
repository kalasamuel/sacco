package com.kimwanyi.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//Member dashboard recent activity feed.
public class RecentActivity {

    public enum Category { SAVINGS, LOAN }

    private final LocalDateTime timestamp;
    private final String title;
    private final String subtitle;
    private final BigDecimal amount;
    private final boolean isCredit;
    private final Category category;

    public RecentActivity(LocalDateTime timestamp, String title, String subtitle,
                        BigDecimal amount, boolean isCredit, Category category) {
        this.timestamp = timestamp;
        this.title = title;
        this.subtitle = subtitle;
        this.amount = amount;
        this.isCredit = isCredit;
        this.category = category;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public BigDecimal getAmount() { return amount; }
    public boolean isCredit() { return isCredit; }
    public Category getCategory() { return category; }

    public String getAmountSign() { return isCredit ? "+" : "-"; }
    public String getAmountColor() { return isCredit ? "#16a34a" : "#dc2626"; }
    public String getCategoryIcon() {
        return category == Category.LOAN ? "fa-solid fa-file-invoice-dollar" : "fa-solid fa-piggy-bank";
    }
    public String getCategoryColor() {
        return category == Category.LOAN ? "#f97316" : "#7c3aed";
    }
    /** Pre-computed background for the icon circle — avoids EL hex-concat issues */
    public String getCategoryBg() {
        return category == Category.LOAN ? "rgba(249,115,22,0.10)" : "rgba(124,58,237,0.10)";
    }
}
