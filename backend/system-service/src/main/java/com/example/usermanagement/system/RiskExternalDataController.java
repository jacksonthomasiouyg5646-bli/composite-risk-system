package com.example.usermanagement.system;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.system.service.RiskModelMonitoringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequirePermission("risk:manage")
@RestController
@RequestMapping("/risks/external-data")
public class RiskExternalDataController {
    private final RiskModelMonitoringService riskModelMonitoringService;

    public RiskExternalDataController(RiskModelMonitoringService riskModelMonitoringService) {
        this.riskModelMonitoringService = riskModelMonitoringService;
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        return ApiResponse.ok(riskModelMonitoringService.getExternalDataStatus());
    }
}
