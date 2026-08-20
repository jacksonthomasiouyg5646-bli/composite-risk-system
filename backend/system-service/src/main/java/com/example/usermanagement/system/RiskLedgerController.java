package com.example.usermanagement.system;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.api.PageResult;
import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.system.service.RiskLedgerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequirePermission("risk:manage")
@RestController
@RequestMapping("/risks/ledgers")
public class RiskLedgerController {
    private final RiskLedgerService riskLedgerService;

    public RiskLedgerController(RiskLedgerService riskLedgerService) {
        this.riskLedgerService = riskLedgerService;
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.ok(riskLedgerService.list(page, size, keyword));
    }
}
