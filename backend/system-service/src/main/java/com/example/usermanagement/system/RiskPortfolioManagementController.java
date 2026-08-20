package com.example.usermanagement.system;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.security.AuthContext;
import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.common.security.ServletAuthFilter;
import com.example.usermanagement.system.service.RiskPortfolioManagementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequirePermission("risk:manage")
@RestController
@RequestMapping("/risks/portfolio-management")
public class RiskPortfolioManagementController {
    private final RiskPortfolioManagementService service;
    public RiskPortfolioManagementController(RiskPortfolioManagementService service) { this.service = service; }
    @GetMapping public ApiResponse<Map<String,Object>> overview(HttpServletRequest request){return ApiResponse.ok(service.getOverview(username(request)));}
    @PostMapping("/limit-snapshots") public ApiResponse<Map<String,Object>> capture(HttpServletRequest request){return ApiResponse.ok(service.captureLimitSnapshot(username(request)));}
    @PutMapping("/limits/{id}") public ApiResponse<Map<String,Object>> update(@PathVariable Long id,@RequestBody Map<String,Object> body,HttpServletRequest request){return ApiResponse.ok(service.updateLimit(id,body,username(request)));}
    @PostMapping("/backtests") public ApiResponse<Map<String,Object>> backtest(HttpServletRequest request){return ApiResponse.ok(service.runBacktest(username(request)));}
    @PostMapping("/alert-effectiveness") public ApiResponse<Map<String,Object>> effectiveness(HttpServletRequest request){return ApiResponse.ok(service.runAlertEffectiveness(username(request)));}
    @PostMapping("/stress-tests") public ApiResponse<Map<String,Object>> stress(@RequestBody(required=false) Map<String,Object> body,HttpServletRequest request){String code=body==null?"MILD_DOWNTURN":String.valueOf(body.getOrDefault("scenario_code","MILD_DOWNTURN"));return ApiResponse.ok(service.runStressTest(code,username(request)));}
    @GetMapping("/groups/{groupCode}/members") public ApiResponse<Map<String,Object>> groupMembers(@PathVariable String groupCode){return ApiResponse.ok(service.getGroupMembers(groupCode));}
    @PutMapping("/workbench-preference") public ApiResponse<Map<String,Object>> preference(@RequestBody Map<String,Object> body,HttpServletRequest request){return ApiResponse.ok(service.saveWorkbenchPreference(body,username(request)));}
    private String username(HttpServletRequest request){Object context=request.getAttribute(ServletAuthFilter.AUTH_CONTEXT_ATTRIBUTE);if(context instanceof AuthContext auth)return auth.username();String value=request.getHeader("X-Auth-Username");return value==null||value.isBlank()?"SYSTEM":value.trim();}
}
