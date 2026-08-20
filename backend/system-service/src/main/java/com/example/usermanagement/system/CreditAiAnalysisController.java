package com.example.usermanagement.system;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.system.service.CreditAiAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequirePermission("risk:manage")
@RestController
@RequestMapping("/risks/ai-analysis")
public class CreditAiAnalysisController {
    private final CreditAiAnalysisService creditAiAnalysisService;

    public CreditAiAnalysisController(CreditAiAnalysisService creditAiAnalysisService) {
        this.creditAiAnalysisService = creditAiAnalysisService;
    }

    @GetMapping("/customer")
    public ApiResponse<Map<String, Object>> analyzeCustomer(
            @RequestParam("customer") String customer,
            @RequestParam(value = "includeExternal", defaultValue = "true") boolean includeExternal
    ) {
        return ApiResponse.ok(creditAiAnalysisService.analyzeCustomer(customer, includeExternal));
    }
}
