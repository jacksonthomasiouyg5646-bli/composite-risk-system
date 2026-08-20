package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.config.ExternalRiskDataProperties;
import com.example.usermanagement.system.mapper.RiskIntelligenceMapper;
import com.example.usermanagement.system.service.CompositeRiskDashboardService;
import com.example.usermanagement.system.service.RiskModelMonitoringService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RiskModelMonitoringServiceImpl implements RiskModelMonitoringService {
    private final CompositeRiskDashboardService compositeRiskDashboardService;
    private final RiskIntelligenceMapper mapper;
    private final ExternalRiskDataProperties externalRiskDataProperties;

    public RiskModelMonitoringServiceImpl(
            CompositeRiskDashboardService compositeRiskDashboardService,
            RiskIntelligenceMapper mapper,
            ExternalRiskDataProperties externalRiskDataProperties
    ) {
        this.compositeRiskDashboardService = compositeRiskDashboardService;
        this.mapper = mapper;
        this.externalRiskDataProperties = externalRiskDataProperties;
    }

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> dashboard = compositeRiskDashboardService.getOverview();
        List<Map<String, Object>> scores = compositeRiskDashboardService.listCustomerScorings();
        Map<String, Object> current = buildCurrentMetrics(dashboard, scores);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("current", current);
        result.put("external_data", getExternalDataStatus());
        result.put("snapshots", mapper.listModelSnapshots(30));
        return result;
    }

    @Override
    public Map<String, Object> captureSnapshot() {
        Map<String, Object> dashboard = compositeRiskDashboardService.getOverview();
        List<Map<String, Object>> scores = compositeRiskDashboardService.listCustomerScorings();
        Map<String, Object> current = buildCurrentMetrics(dashboard, scores);
        Map<String, Object> external = map(getExternalDataStatus().get("today"));
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("snapshot_date", LocalDate.now());
        snapshot.put("model_name", current.get("model_name"));
        snapshot.put("rule_count", integer(current.get("rule_count")));
        snapshot.put("customer_total", integer(current.get("customer_total")));
        snapshot.put("average_risk_score", current.get("average_risk_score"));
        snapshot.put("extreme_risk_count", integer(current.get("extreme_risk_count")));
        snapshot.put("high_risk_count", integer(current.get("high_risk_count")));
        snapshot.put("warning_customer_count", integer(current.get("warning_customer_count")));
        snapshot.put("forecast_upgrade_count", integer(current.get("forecast_upgrade_count")));
        snapshot.put("external_query_count", integer(external.get("query_count_today")));
        snapshot.put("external_available_count", integer(external.get("available_count_today")));
        snapshot.put("external_unavailable_count", integer(external.get("unavailable_count_today")));
        mapper.upsertModelSnapshot(snapshot);
        return snapshot;
    }

    @Override
    public Map<String, Object> getExternalDataStatus() {
        Map<String, Object> latest = mapper.getLatestExternalDataAccess();
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("provider_name", externalRiskDataProperties.getProviderName());
        status.put("enabled", externalRiskDataProperties.isEnabled());
        status.put("today", mapper.getExternalDataStatusSummary());
        status.put("latest", latest);
        if (!externalRiskDataProperties.isEnabled()) {
            status.put("connection_status", "NOT_ENABLED");
        } else if (latest == null) {
            status.put("connection_status", "NO_REQUEST");
        } else {
            status.put("connection_status", latest.get("query_status"));
        }
        return status;
    }

    @Scheduled(cron = "${risk.model-monitoring.daily-cron:0 0 8 * * *}", zone = "Asia/Shanghai")
    public void scheduledSnapshot() {
        captureSnapshot();
    }

    private Map<String, Object> buildCurrentMetrics(Map<String, Object> dashboard, List<Map<String, Object>> scores) {
        Map<String, Object> summary = map(dashboard.get("summary"));
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> score : scores) {
            total = total.add(decimal(score.get("risk_score")));
        }
        BigDecimal average = scores.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(scores.size()), 4, RoundingMode.HALF_UP);
        Map<String, Object> current = new LinkedHashMap<>();
        current.put("model_name", dashboard.get("analysis_model"));
        current.put("rule_count", dashboard.get("rule_count"));
        current.put("customer_total", scores.size());
        current.put("average_risk_score", average);
        current.put("extreme_risk_count", summary.get("extreme_risk_count"));
        current.put("high_risk_count", summary.get("high_risk_count"));
        current.put("warning_customer_count", summary.get("warning_customer_count"));
        current.put("forecast_upgrade_count", summary.get("forecast_upgrade_count"));
        return current;
    }

    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> result = new LinkedHashMap<>();
            source.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return Map.of();
    }

    private BigDecimal decimal(Object value) {
        try {
            return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value == null ? 0 : value));
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private int integer(Object value) {
        return decimal(value).intValue();
    }
}
