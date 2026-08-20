package com.example.usermanagement.system;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.system.service.CompositeRiskDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequirePermission("risk:manage")
@RestController
@RequestMapping("/risks/composite-dashboard")
public class CompositeRiskDashboardController {
    private final CompositeRiskDashboardService compositeRiskDashboardService;

    public CompositeRiskDashboardController(CompositeRiskDashboardService compositeRiskDashboardService) {
        this.compositeRiskDashboardService = compositeRiskDashboardService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getOverview() {
        return ApiResponse.ok(compositeRiskDashboardService.getOverview());
    }

    @RequirePermission("risk:treat")
    @PostMapping("/alerts/{customerNo}/treatment")
    public ApiResponse<Map<String, Object>> createTreatment(@PathVariable String customerNo) {
        return ApiResponse.ok(compositeRiskDashboardService.createTreatment(customerNo));
    }
}
