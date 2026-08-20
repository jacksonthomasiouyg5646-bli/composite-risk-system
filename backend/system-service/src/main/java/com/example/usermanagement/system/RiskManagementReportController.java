package com.example.usermanagement.system;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.system.service.RiskManagementReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequirePermission("risk:manage")
@RestController
@RequestMapping("/risks/management-reports")
public class RiskManagementReportController {
    private final RiskManagementReportService riskManagementReportService;

    public RiskManagementReportController(RiskManagementReportService riskManagementReportService) {
        this.riskManagementReportService = riskManagementReportService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getReport() {
        return ApiResponse.ok(riskManagementReportService.getReport());
    }
}
