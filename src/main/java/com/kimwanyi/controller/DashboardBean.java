package com.kimwanyi.controller;

import com.kimwanyi.model.dto.DashboardSummaryDto;
import com.kimwanyi.service.interfaces.DashboardService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import jakarta.annotation.PostConstruct;

@Component("dashboardBean")
@RequestScope
public class DashboardBean {

    private final DashboardService dashboardService;
    
    private DashboardSummaryDto summary;

    public DashboardBean(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PostConstruct
    public void init() {
        this.summary = dashboardService.getSummary();
    }

    public DashboardSummaryDto getSummary() {
        return summary;
    }
}
