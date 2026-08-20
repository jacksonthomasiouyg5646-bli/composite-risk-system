package com.example.usermanagement.system.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CompositeRiskScoringEngine {
    private static final int BASE_SCORE = 10;

    public Map<String, Object> score(Map<String, Object> feature, List<Map<String, Object>> rules) {
        Map<String, Object> result = new LinkedHashMap<>(feature);
        int score = BASE_SCORE;
        Set<String> additiveMetrics = new HashSet<>();
        List<String> tags = new ArrayList<>();
        List<String> reasons = new ArrayList<>();

        for (Map<String, Object> rule : rules) {
            String metric = text(rule.get("metric_key"));
            BigDecimal value = decimal(feature.get(metric));
            if (value == null || !matches(value, decimal(rule.get("threshold_value")), text(rule.get("operator_type")))) {
                continue;
            }

            String effect = text(rule.get("effect_type"));
            if ("ADD".equals(effect) && !additiveMetrics.add(metric)) {
                continue;
            }

            int scoreValue = integer(rule.get("score_value"));
            if ("FLOOR".equals(effect)) {
                score = Math.max(score, scoreValue);
            } else {
                score += scoreValue;
            }

            String tag = text(rule.get("risk_tag"));
            if (!tag.isBlank() && !tags.contains(tag)) {
                tags.add(tag);
            }
            String reason = text(rule.get("reason_template"));
            if (!reason.isBlank() && !reasons.contains(reason)) {
                reasons.add(reason.replace("{value}", displayMetric(metric, value)));
            }
        }

        score = Math.min(score, 100);
        int forecastScore = Math.min(100, score + forecastBoost(feature, score));
        String riskLevel = riskLevel(score);
        String forecastLevel = riskLevel(forecastScore);
        result.put("risk_score", score);
        result.put("risk_level", riskLevel);
        result.put("priority", priority(score));
        result.put("alert_type", alertType(feature, riskLevel));
        result.put("risk_tags", tags);
        result.put("risk_reasons", reasons);
        result.put("risk_signals", reasons.isEmpty() ? "常规监测" : String.join("；", reasons));
        result.put("forecast_score", forecastScore);
        result.put("forecast_level", forecastLevel);
        result.put("forecast_change", levelRank(forecastLevel) > levelRank(riskLevel) ? "风险上迁" : "风险维持");
        return result;
    }

    private int forecastBoost(Map<String, Object> feature, int score) {
        if (score >= 85) {
            return 0;
        }
        int boost = 0;
        if (integer(feature.get("overdue_count")) > 0) {
            boost += 8;
        }
        if (decimalOrZero(feature.get("max_pd")).compareTo(new BigDecimal("0.050000")) >= 0) {
            boost += 6;
        }
        if (decimalOrZero(feature.get("utilization_rate")).compareTo(new BigDecimal("0.900000")) >= 0) {
            boost += 4;
        }
        BigDecimal coverage = decimal(feature.get("coverage_rate"));
        if (coverage != null && coverage.compareTo(new BigDecimal("0.500000")) < 0) {
            boost += 5;
        }
        return Math.min(boost, 20);
    }

    private boolean matches(BigDecimal value, BigDecimal threshold, String operator) {
        if (threshold == null) {
            return false;
        }
        int comparison = value.compareTo(threshold);
        return switch (operator) {
            case "GT" -> comparison > 0;
            case "GTE" -> comparison >= 0;
            case "LT" -> comparison < 0;
            case "LTE" -> comparison <= 0;
            case "EQ" -> comparison == 0;
            default -> false;
        };
    }

    private String alertType(Map<String, Object> feature, String level) {
        if (integer(feature.get("blacklist_flag")) == 1) return "名单风险";
        if (integer(feature.get("debt_default_count")) > 0) return "债项违约";
        if (integer(feature.get("max_overdue_days")) > 90) return "严重逾期";
        if (integer(feature.get("max_overdue_days")) > 0) return "逾期预警";
        if (integer(feature.get("rating_numeric")) >= 21) return "评级触发";
        if (decimalOrZero(feature.get("max_pd")).compareTo(new BigDecimal("0.100000")) >= 0) return "PD 高风险";
        if (decimalOrZero(feature.get("utilization_rate")).compareTo(new BigDecimal("0.900000")) >= 0) return "额度高占用";
        return "低风险".equals(level) ? "常规监测" : "组合预警";
    }

    private String priority(int score) {
        if (score >= 85) return "P1";
        if (score >= 65) return "P2";
        if (score >= 45) return "P3";
        return "P4";
    }

    private String riskLevel(int score) {
        if (score >= 85) return "极高风险";
        if (score >= 65) return "高风险";
        if (score >= 45) return "中风险";
        return "低风险";
    }

    private int levelRank(String level) {
        return switch (level) {
            case "极高风险" -> 4;
            case "高风险" -> 3;
            case "中风险" -> 2;
            default -> 1;
        };
    }

    private String displayMetric(String metric, BigDecimal value) {
        if (Set.of("max_pd", "utilization_rate", "coverage_rate").contains(metric)) {
            return value.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private BigDecimal decimal(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        try {
            return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal decimalOrZero(Object value) {
        BigDecimal decimal = decimal(value);
        return decimal == null ? BigDecimal.ZERO : decimal;
    }

    private int integer(Object value) {
        BigDecimal decimal = decimal(value);
        return decimal == null ? 0 : decimal.intValue();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
