package com.example.usermanagement.system.service;

import java.time.LocalDate;

public record CreditDomainQueryCriteria(
        String queryType,
        String keyword,
        String customerNo,
        String ratingLevel,
        String riskLevel,
        String productType,
        String status,
        String defaultLevel,
        String ownerOrgName,
        LocalDate startDate,
        LocalDate endDate) {
}
