package com.example.usermanagement.system;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.system.service.CreditDefaultTrendService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RequirePermission("risk:manage")
@RestController
@RequestMapping("/risks/default-trends")
public class CreditDefaultTrendController {
    private final CreditDefaultTrendService creditDefaultTrendService;

    public CreditDefaultTrendController(CreditDefaultTrendService creditDefaultTrendService) {
        this.creditDefaultTrendService = creditDefaultTrendService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> trend(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "defaultLevel", required = false) String defaultLevel) {
        return ApiResponse.ok(creditDefaultTrendService.getTrend(startDate, endDate, keyword, defaultLevel));
    }
}
