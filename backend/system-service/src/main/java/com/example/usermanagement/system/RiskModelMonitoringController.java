package com.example.usermanagement.system;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.system.service.RiskModelMonitoringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequirePermission("risk:manage")
@RestController
@RequestMapping("/risks/model-monitoring")
public class RiskModelMonitoringController {
    private final RiskModelMonitoringService riskModelMonitoringService;

    public RiskModelMonitoringController(RiskModelMonitoringService riskModelMonitoringService) {
        this.riskModelMonitoringService = riskModelMonitoringService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getOverview() {
        return ApiResponse.ok(riskModelMonitoringService.getOverview());
    }

    @PostMapping("/snapshot")
    public ApiResponse<Map<String, Object>> captureSnapshot() {
        return ApiResponse.ok(riskModelMonitoringService.captureSnapshot());
    }
}
