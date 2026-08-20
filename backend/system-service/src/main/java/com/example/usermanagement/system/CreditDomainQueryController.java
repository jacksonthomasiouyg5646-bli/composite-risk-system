package com.example.usermanagement.system;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.api.PageResult;
import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.system.service.CreditDomainQueryCriteria;
import com.example.usermanagement.system.service.CreditDomainQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RequirePermission("risk:manage")
@RestController
@RequestMapping("/risks/credit-domain")
public class CreditDomainQueryController {
    private final CreditDomainQueryService creditDomainQueryService;

    public CreditDomainQueryController(CreditDomainQueryService creditDomainQueryService) {
        this.creditDomainQueryService = creditDomainQueryService;
    }

    @GetMapping("/query")
    public ApiResponse<PageResult<Map<String, Object>>> query(
            @RequestParam(value = "queryType", defaultValue = "customer") String queryType,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "customerNo", required = false) String customerNo,
            @RequestParam(value = "ratingLevel", required = false) String ratingLevel,
            @RequestParam(value = "riskLevel", required = false) String riskLevel,
            @RequestParam(value = "productType", required = false) String productType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "defaultLevel", required = false) String defaultLevel,
            @RequestParam(value = "ownerOrgName", required = false) String ownerOrgName,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        CreditDomainQueryCriteria criteria = new CreditDomainQueryCriteria(
                queryType,
                keyword,
                customerNo,
                ratingLevel,
                riskLevel,
                productType,
                status,
                defaultLevel,
                ownerOrgName,
                startDate,
                endDate);
        return ApiResponse.ok(creditDomainQueryService.query(criteria, page, size));
    }
}
