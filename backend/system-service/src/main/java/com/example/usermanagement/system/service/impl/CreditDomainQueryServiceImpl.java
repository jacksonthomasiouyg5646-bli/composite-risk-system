package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.common.api.PageResult;
import com.example.usermanagement.system.mapper.CreditDomainQueryMapper;
import com.example.usermanagement.system.service.CreditDomainQueryCriteria;
import com.example.usermanagement.system.service.CreditDomainQueryService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CreditDomainQueryServiceImpl implements CreditDomainQueryService {
    private static final Set<String> DEFAULT_LEVELS = Set.of("A", "B", "C");

    private final CreditDomainQueryMapper mapper;

    public CreditDomainQueryServiceImpl(CreditDomainQueryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PageResult<Map<String, Object>> query(CreditDomainQueryCriteria criteria, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        CreditDomainQueryCriteria normalized = normalize(criteria);

        List<Map<String, Object>> rows;
        long total;
        switch (normalized.queryType()) {
            case "customer" -> {
                rows = mapper.listCustomerView(normalized, safeSize, offset);
                total = mapper.countCustomerView(normalized);
            }
            case "limit" -> {
                rows = mapper.listLimitView(normalized, safeSize, offset);
                total = mapper.countLimitView(normalized);
            }
            case "applicationContract" -> {
                rows = mapper.listApplicationContractView(normalized, safeSize, offset);
                total = mapper.countApplicationContractView(normalized);
            }
            case "debtExposure" -> {
                rows = mapper.listDebtExposureView(normalized, safeSize, offset);
                total = mapper.countDebtExposureView(normalized);
            }
            case "defaultOverdue" -> {
                rows = mapper.listDefaultOverdueView(normalized, safeSize, offset);
                total = mapper.countDefaultOverdueView(normalized);
            }
            default -> throw new IllegalArgumentException("unsupported queryType: " + normalized.queryType());
        }

        return new PageResult<>(rows, total, safePage, safeSize);
    }

    private CreditDomainQueryCriteria normalize(CreditDomainQueryCriteria criteria) {
        String queryType = normalizeQueryType(criteria.queryType());
        LocalDate startDate = criteria.startDate();
        LocalDate endDate = criteria.endDate();
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        return new CreditDomainQueryCriteria(
                queryType,
                trim(criteria.keyword()),
                trim(criteria.customerNo()),
                trim(criteria.ratingLevel()),
                upper(trim(criteria.riskLevel())),
                trim(criteria.productType()),
                upper(trim(criteria.status())),
                normalizeDefaultLevel(criteria.defaultLevel()),
                trim(criteria.ownerOrgName()),
                startDate,
                endDate);
    }

    private String normalizeQueryType(String queryType) {
        String normalized = trim(queryType);
        if (normalized == null) {
            return "customer";
        }
        return switch (normalized) {
            case "customer", "limit", "applicationContract", "debtExposure", "defaultOverdue" -> normalized;
            default -> "customer";
        };
    }

    private String normalizeDefaultLevel(String defaultLevel) {
        String normalized = upper(trim(defaultLevel));
        return normalized != null && DEFAULT_LEVELS.contains(normalized) ? normalized : null;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String upper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }
}
