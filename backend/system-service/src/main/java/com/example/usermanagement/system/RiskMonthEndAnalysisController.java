package com.example.usermanagement.system;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.security.AuthContext;
import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.common.security.ServletAuthFilter;
import com.example.usermanagement.system.service.RiskMonthEndAnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RequirePermission("risk:manage")
@RestController
@RequestMapping("/risks/month-end-analysis")
public class RiskMonthEndAnalysisController {
    private final RiskMonthEndAnalysisService service;

    public RiskMonthEndAnalysisController(RiskMonthEndAnalysisService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> analysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate currentMonth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseMonth,
            @RequestParam(defaultValue = "INDUSTRY") String dimension) {
        return ApiResponse.ok(service.getAnalysis(currentMonth, baseMonth, dimension));
    }

    @PostMapping("/batches")
    public ApiResponse<Map<String, Object>> capture(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        LocalDate monthEnd = null;
        if (body != null && body.get("month_end_date") != null && !String.valueOf(body.get("month_end_date")).isBlank()) {
            monthEnd = LocalDate.parse(String.valueOf(body.get("month_end_date")).substring(0, 10));
        }
        return ApiResponse.ok(service.captureMonthEnd(monthEnd, username(request), body == null ? Map.of() : body));
    }

    @GetMapping("/changes/drilldown")
    public ApiResponse<Map<String, Object>> drilldown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate currentMonth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseMonth,
            @RequestParam(defaultValue = "INDUSTRY") String level,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String customerNo,
            @RequestParam(required = false) String contractNo,
            @RequestParam(required = false) String changeType) {
        return ApiResponse.ok(service.getChangeDrilldown(currentMonth, baseMonth, level, industry, customerNo, contractNo, changeType));
    }

    @PatchMapping("/quality-issues/{issueId}")
    public ApiResponse<Map<String, Object>> updateIssue(@PathVariable Long issueId,
                                                        @RequestBody Map<String, Object> body,
                                                        HttpServletRequest request) {
        return ApiResponse.ok(service.updateQualityIssue(issueId, body, username(request)));
    }

    private String username(HttpServletRequest request) {
        Object context = request.getAttribute(ServletAuthFilter.AUTH_CONTEXT_ATTRIBUTE);
        if (context instanceof AuthContext auth) return auth.username();
        String value = request.getHeader("X-Auth-Username");
        return value == null || value.isBlank() ? "SYSTEM" : value.trim();
    }
}
