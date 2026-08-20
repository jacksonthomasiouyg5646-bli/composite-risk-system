package com.example.usermanagement.system;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.api.PageResult;
import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.system.service.RiskScoringRuleService;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/risks/scoring-rules")
public class RiskScoringRuleController {
    private final RiskScoringRuleService riskScoringRuleService;

    public RiskScoringRuleController(RiskScoringRuleService riskScoringRuleService) {
        this.riskScoringRuleService = riskScoringRuleService;
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return ApiResponse.ok(riskScoringRuleService.list(page, size, keyword));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(riskScoringRuleService.create(body));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(riskScoringRuleService.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        riskScoringRuleService.delete(id);
        return ApiResponse.ok(null);
    }
}
