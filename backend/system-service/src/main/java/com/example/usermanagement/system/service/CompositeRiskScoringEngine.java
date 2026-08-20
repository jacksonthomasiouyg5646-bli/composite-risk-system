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

    public Map<String, Object> explain(Map<String, Object> feature, List<Map<String, Object>> rules) {
        Map<String, Object> result = score(feature, rules);
        int score = BASE_SCORE;
        Set<String> additiveMetrics = new HashSet<>();
        List<Map<String, Object>> factors = new ArrayList<>();

        for (Map<String, Object> rule : rules) {
            String metric = text(rule.get("metric_key"));
            BigDecimal metricValue = decimal(feature.get(metric));
            BigDecimal threshold = decimal(rule.get("threshold_value"));
            String operator = text(rule.get("operator_type"));
            String effect = text(rule.get("effect_type"));
            boolean hit = metricValue != null && matches(metricValue, threshold, operator);
            boolean duplicateAdditive = hit && "ADD".equals(effect) && !additiveMetrics.add(metric);
            int scoreValue = integer(rule.get("score_value"));
            int contribution = 0;
            if (hit && !duplicateAdditive) {
                if ("FLOOR".equals(effect)) {
                    contribution = Math.max(0, scoreValue - score);
                    score = Math.max(score, scoreValue);
                } else {
                    contribution = scoreValue;
                    score += scoreValue;
                }
                score = Math.min(score, 100);
            }

            Map<String, Object> factor = new LinkedHashMap<>();
            factor.put("rule_code", rule.get("rule_code"));
            factor.put("rule_name", rule.get("rule_name"));
            factor.put("risk_tag", rule.get("risk_tag"));
            factor.put("metric_key", metric);
            factor.put("metric_value", metricValue);
            factor.put("metric_display", displayMetricValue(metric, metricValue));
            factor.put("operator_type", operator);
            factor.put("operator_label", operatorLabel(operator));
            factor.put("threshold_value", threshold);
            factor.put("threshold_display", displayMetricValue(metric, threshold));
            factor.put("effect_type", effect);
            factor.put("score_value", scoreValue);
            factor.put("hit", hit);
            factor.put("ignored", duplicateAdditive);
            factor.put("contribution", contribution);
            factor.put("running_score", score);
            factor.put("reason", explainRuleReason(rule, metric, metricValue, threshold, hit, duplicateAdditive));
            factors.add(factor);
        }

        int riskScore = integer(result.get("risk_score"));
        result.put("base_score", BASE_SCORE);
        result.put("factors", factors);
        result.put("forecast_boost", integer(result.get("forecast_score")) - riskScore);
        result.put("forecast_factors", forecastFactors(feature, riskScore));
        result.put("formula_notes", List.of(
                "基准分为 " + BASE_SCORE + " 分，命中 ADD 规则累加分值，同一指标 ADD 规则只取首次命中，避免重复加分。",
                "FLOOR 规则用于重大风险兜底：命中后将当前分数至少抬升到规则分值。",
                "30 日预测分 = 当前风险评分 + 预测加分，预测加分最高 20 分，总分封顶 100 分。"
        ));
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

    private List<Map<String, Object>> forecastFactors(Map<String, Object> feature, int score) {
        if (score >= 85) {
            return List.of(forecastFactor("极高风险封顶", "risk_score", score, ">= 85", 0, true));
        }
        List<Map<String, Object>> factors = new ArrayList<>();
        factors.add(forecastFactor("存在逾期记录", "overdue_count", integer(feature.get("overdue_count")), "> 0", 8, integer(feature.get("overdue_count")) > 0));
        BigDecimal maxPd = decimalOrZero(feature.get("max_pd"));
        factors.add(forecastFactor("PD 不低于 5%", "max_pd", displayMetricValue("max_pd", maxPd), ">= 5.00%", 6, maxPd.compareTo(new BigDecimal("0.050000")) >= 0));
        BigDecimal utilizationRate = decimalOrZero(feature.get("utilization_rate"));
        factors.add(forecastFactor("额度使用率不低于 90%", "utilization_rate", displayMetricValue("utilization_rate", utilizationRate), ">= 90.00%", 4, utilizationRate.compareTo(new BigDecimal("0.900000")) >= 0));
        BigDecimal coverageRate = decimal(feature.get("coverage_rate"));
        factors.add(forecastFactor("押品覆盖率低于 50%", "coverage_rate", displayMetricValue("coverage_rate", coverageRate), "< 50.00%", 5, coverageRate != null && coverageRate.compareTo(new BigDecimal("0.500000")) < 0));
        return factors;
    }

    private Map<String, Object> forecastFactor(String name, String metric, Object value, String threshold, int scoreValue, boolean hit) {
        Map<String, Object> factor = new LinkedHashMap<>();
        factor.put("factor_name", name);
        factor.put("metric_key", metric);
        factor.put("metric_value", value);
        factor.put("threshold", threshold);
        factor.put("score_value", scoreValue);
        factor.put("hit", hit);
        factor.put("contribution", hit ? scoreValue : 0);
        return factor;
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

    private String displayMetricValue(String metric, BigDecimal value) {
        if (value == null) return "-";
        return displayMetric(metric, value);
    }

    private String operatorLabel(String operator) {
        return switch (operator) {
            case "GT" -> ">";
            case "GTE" -> ">=";
            case "LT" -> "<";
            case "LTE" -> "<=";
            case "EQ" -> "=";
            default -> operator;
        };
    }

    private String explainRuleReason(Map<String, Object> rule, String metric, BigDecimal metricValue, BigDecimal threshold, boolean hit, boolean duplicateAdditive) {
        if (duplicateAdditive) {
            return "已命中，但同一指标 ADD 规则已计分，本规则仅作为证据展示。";
        }
        String actual = displayMetricValue(metric, metricValue);
        String expected = displayMetricValue(metric, threshold);
        String description = text(rule.get("reason_template"));
        if (hit && !description.isBlank()) {
            return description.replace("{value}", actual);
        }
        return hit ? "命中：当前值 " + actual + " " + operatorLabel(text(rule.get("operator_type"))) + " 阈值 " + expected
                : "未命中：当前值 " + actual + "，阈值 " + operatorLabel(text(rule.get("operator_type"))) + " " + expected;
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
