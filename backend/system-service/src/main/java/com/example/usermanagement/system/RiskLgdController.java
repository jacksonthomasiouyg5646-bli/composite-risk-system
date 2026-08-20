package com.example.usermanagement.system;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.api.PageResult;
import com.example.usermanagement.common.security.AuthContext;
import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.common.security.ServletAuthFilter;
import com.example.usermanagement.system.service.RiskLgdService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RequirePermission("risk:manage")
@RestController
@RequestMapping("/risks/lgd")
public class RiskLgdController {
    private final RiskLgdService riskLgdService;

    public RiskLgdController(RiskLgdService riskLgdService) {
        this.riskLgdService = riskLgdService;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.ok(riskLgdService.getOverview());
    }

    @GetMapping("/ledger")
    public ApiResponse<PageResult<Map<String, Object>>> ledger(
            @RequestParam Map<String, String> requestParams,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Map<String, Object> filters = new LinkedHashMap<>(requestParams);
        filters.remove("page");
        filters.remove("size");
        return ApiResponse.ok(riskLgdService.listLedger(filters, page, size));
    }

    @GetMapping("/debts/{debtNo}")
    public ApiResponse<Map<String, Object>> debtDetail(@PathVariable String debtNo) {
        return ApiResponse.ok(riskLgdService.getDebtDetail(debtNo));
    }

    @GetMapping("/customers/{customerNo}")
    public ApiResponse<Map<String, Object>> customerLgd(@PathVariable String customerNo) {
        return ApiResponse.ok(riskLgdService.getCustomerLgd(customerNo));
    }

    @GetMapping("/governance")
    public ApiResponse<Map<String, Object>> governance() {
        return ApiResponse.ok(riskLgdService.getGovernance());
    }

    @PostMapping("/calculation-runs")
    public ApiResponse<Map<String, Object>> captureCalculation(HttpServletRequest request) {
        return ApiResponse.ok(riskLgdService.captureCalculationRun(username(request)));
    }

    @PostMapping("/stress-tests")
    public ApiResponse<Map<String, Object>> stressTest(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(riskLgdService.runStressTest(body, username(request)));
    }

    private String username(HttpServletRequest request) {
        Object context = request.getAttribute(ServletAuthFilter.AUTH_CONTEXT_ATTRIBUTE);
        if (context instanceof AuthContext authContext) return authContext.username();
        String forwardedUsername = request.getHeader("X-Auth-Username");
        return forwardedUsername == null || forwardedUsername.isBlank() ? "SYSTEM" : forwardedUsername.trim();
    }
}
