package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.RiskIntelligenceMapper;
import com.example.usermanagement.system.service.CompositeRiskDashboardService;
import com.example.usermanagement.system.service.RiskManagementReportService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RiskManagementReportServiceImpl implements RiskManagementReportService {
    private final CompositeRiskDashboardService compositeRiskDashboardService;
    private final RiskIntelligenceMapper mapper;

    public RiskManagementReportServiceImpl(
            CompositeRiskDashboardService compositeRiskDashboardService,
            RiskIntelligenceMapper mapper
    ) {
        this.compositeRiskDashboardService = compositeRiskDashboardService;
        this.mapper = mapper;
    }

    @Override
    public Map<String, Object> getReport() {
        Map<String, Object> dashboard = compositeRiskDashboardService.getOverview();
        List<Map<String, Object>> scorings = compositeRiskDashboardService.listCustomerScorings();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generated_at", LocalDateTime.now());
        report.put("organization", organizationReport(scorings));
        report.put("industry", dashboard.get("industry_distribution"));
        report.put("product", mapper.listProductReport());
        report.put("risk_migration", dashboard.get("risk_migration"));
        report.put("summary", dashboard.get("summary"));
        return report;
    }

    private List<Map<String, Object>> organizationReport(List<Map<String, Object>> scorings) {
        Map<String, Map<String, Object>> aggregates = new LinkedHashMap<>();
        for (Map<String, Object> row : scorings) {
            String organization = value(row.get("owner_org_name"), "未分配机构");
            Map<String, Object> aggregate = aggregates.computeIfAbsent(organization, key -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("owner_org_name", key);
                item.put("customer_count", 0);
                item.put("warning_customer_count", 0);
                item.put("high_risk_count", 0);
                item.put("warning_ead_amount", BigDecimal.ZERO);
                item.put("forecast_upgrade_count", 0);
                return item;
            });
            aggregate.put("customer_count", integer(aggregate.get("customer_count")) + 1);
            int score = integer(row.get("risk_score"));
            if (score >= 45) {
                aggregate.put("warning_customer_count", integer(aggregate.get("warning_customer_count")) + 1);
                aggregate.put("warning_ead_amount", decimal(aggregate.get("warning_ead_amount")).add(decimal(row.get("ead_amount_total"))));
            }
            if (score >= 65) {
                aggregate.put("high_risk_count", integer(aggregate.get("high_risk_count")) + 1);
            }
            if ("风险上迁".equals(row.get("forecast_change"))) {
                aggregate.put("forecast_upgrade_count", integer(aggregate.get("forecast_upgrade_count")) + 1);
            }
        }
        return aggregates.values().stream()
                .sorted(Comparator
                        .comparingInt((Map<String, Object> row) -> integer(row.get("high_risk_count"))).reversed()
                        .thenComparing(row -> decimal(row.get("warning_ead_amount")), Comparator.reverseOrder()))
                .toList();
    }

    private int integer(Object value) {
        return decimal(value).intValue();
    }

    private BigDecimal decimal(Object value) {
        try {
            return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value == null ? 0 : value));
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private String value(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }
}
