package com.example.usermanagement.system;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.api.PageResult;
import com.example.usermanagement.common.security.AuthContext;
import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.common.security.ServletAuthFilter;
import com.example.usermanagement.system.service.RiskGovernanceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequirePermission("risk:manage")
@RestController
@RequestMapping("/risks")
public class RiskGovernanceController {
    private final RiskGovernanceService riskGovernanceService;

    public RiskGovernanceController(RiskGovernanceService riskGovernanceService) {
        this.riskGovernanceService = riskGovernanceService;
    }

    @GetMapping("/data-governance")
    public ApiResponse<Map<String, Object>> dataGovernance() {
        return ApiResponse.ok(riskGovernanceService.getDataGovernanceOverview());
    }

    @PostMapping("/data-governance/snapshot")
    public ApiResponse<Map<String, Object>> captureDataQuality() {
        return ApiResponse.ok(riskGovernanceService.captureDataQuality());
    }

    @GetMapping("/model-governance")
    public ApiResponse<Map<String, Object>> modelGovernance() {
        return ApiResponse.ok(riskGovernanceService.getModelGovernanceOverview());
    }

    @GetMapping("/model-governance/versions/{versionId}")
    public ApiResponse<Map<String, Object>> getModelVersion(@PathVariable Long versionId) {
        return ApiResponse.ok(riskGovernanceService.getModelVersionDetail(versionId));
    }

    @PostMapping("/model-governance/versions")
    public ApiResponse<Map<String, Object>> createModelVersion(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(riskGovernanceService.createModelVersion(body, username(request)));
    }

    @PutMapping("/model-governance/versions/{versionId}/rules/{ruleCode}")
    public ApiResponse<Map<String, Object>> updateModelVersionRule(
            @PathVariable Long versionId,
            @PathVariable String ruleCode,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(riskGovernanceService.updateModelVersionRule(versionId, ruleCode, body, username(request)));
    }

    @PostMapping("/model-governance/versions/{versionId}/simulate")
    public ApiResponse<Map<String, Object>> simulateModelVersion(@PathVariable Long versionId, HttpServletRequest request) {
        return ApiResponse.ok(riskGovernanceService.simulateModelVersion(versionId, username(request)));
    }

    @PostMapping("/model-governance/versions/{versionId}/submit")
    public ApiResponse<Map<String, Object>> submitModelVersion(@PathVariable Long versionId, @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(riskGovernanceService.submitModelVersion(versionId, username(request), text(body, "comment")));
    }

    @PostMapping("/model-governance/versions/{versionId}/approve")
    public ApiResponse<Map<String, Object>> approveModelVersion(@PathVariable Long versionId, @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(riskGovernanceService.approveModelVersion(versionId, username(request), text(body, "comment")));
    }

    @PostMapping("/model-governance/versions/{versionId}/publish")
    public ApiResponse<Map<String, Object>> publishModelVersion(@PathVariable Long versionId, @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(riskGovernanceService.publishModelVersion(versionId, username(request), text(body, "comment")));
    }

    @PostMapping("/model-governance/versions/{versionId}/rollback")
    public ApiResponse<Map<String, Object>> rollbackModelVersion(@PathVariable Long versionId, @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(riskGovernanceService.rollbackModelVersion(versionId, username(request), text(body, "comment")));
    }

    @GetMapping("/alert-cases")
    public ApiResponse<PageResult<Map<String, Object>>> listAlertCases(
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(riskGovernanceService.listAlertCases(state, page, size));
    }

    @PostMapping("/alert-cases/refresh")
    public ApiResponse<Map<String, Object>> refreshAlertCases() {
        return ApiResponse.ok(riskGovernanceService.refreshAlertCases());
    }

    @RequirePermission("risk:treat")
    @PostMapping("/alert-cases/{customerNo}/start")
    public ApiResponse<Map<String, Object>> startAlertCase(@PathVariable String customerNo, HttpServletRequest request) {
        return ApiResponse.ok(riskGovernanceService.startAlertCase(customerNo, username(request)));
    }

    @RequirePermission("risk:treat")
    @PostMapping("/alert-cases/{customerNo}/close")
    public ApiResponse<Map<String, Object>> closeAlertCase(@PathVariable String customerNo, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(riskGovernanceService.closeAlertCase(customerNo, text(body, "comment"), username(request)));
    }

    @RequirePermission("risk:treat")
    @PostMapping("/alert-cases/batch/start")
    public ApiResponse<Map<String, Object>> batchStartAlertCases(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(riskGovernanceService.batchStartAlertCases(body, username(request)));
    }

    @RequirePermission("risk:treat")
    @PostMapping("/alert-cases/batch/close")
    public ApiResponse<Map<String, Object>> batchCloseAlertCases(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(riskGovernanceService.batchCloseAlertCases(body, username(request)));
    }

    @PostMapping("/stress-tests")
    public ApiResponse<Map<String, Object>> runStressTest(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(riskGovernanceService.runStressTest(body, username(request)));
    }

    @GetMapping("/relationship-graph")
    public ApiResponse<Map<String, Object>> relationshipGraph(@RequestParam String customerNo) {
        return ApiResponse.ok(riskGovernanceService.getRelationshipGraph(customerNo));
    }

    private String username(HttpServletRequest request) {
        Object context = request.getAttribute(ServletAuthFilter.AUTH_CONTEXT_ATTRIBUTE);
        if (context instanceof AuthContext authContext) {
            return authContext.username();
        }
        String forwardedUsername = request.getHeader("X-Auth-Username");
        return forwardedUsername == null || forwardedUsername.isBlank() ? "SYSTEM" : forwardedUsername.trim();
    }

    private String text(Map<String, Object> body, String key) {
        Object value = body == null ? null : body.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }
}
