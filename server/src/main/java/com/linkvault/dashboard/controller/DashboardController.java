package com.linkvault.dashboard.controller;

import com.linkvault.common.response.ApiResponse;
import com.linkvault.dashboard.dto.DashboardSummaryResponse;
import com.linkvault.dashboard.service.DashboardService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/dashboard/summary")
    public ApiResponse<DashboardSummaryResponse> summary() {
        return ApiResponse.success("Dashboard summary loaded", dashboardService.summary());
    }

    @GetMapping("/api/workspaces/{workspaceId}/dashboard/summary")
    public ApiResponse<DashboardSummaryResponse> summaryInWorkspace(@PathVariable UUID workspaceId) {
        return ApiResponse.success("Dashboard summary loaded", dashboardService.summary(workspaceId));
    }
}