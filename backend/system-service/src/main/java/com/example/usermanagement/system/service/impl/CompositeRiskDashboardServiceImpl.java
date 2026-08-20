package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.CompositeRiskDashboardMapper;
import com.example.usermanagement.system.mapper.RiskScoringRuleMapper;
import com.example.usermanagement.system.service.CompositeRiskDashboardService;
import com.example.usermanagement.system.service.CompositeRiskScoringEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompositeRiskDashboardServiceImpl implements CompositeRiskDashboardService {
    private static final int ALERT_LIMIT = 20;
    private static final int INDUSTRY_LIMIT = 6;

    private final CompositeRiskDashboardMapper mapper;
    private final RiskScoringRuleMapper riskScoringRuleMapper;
    private final CompositeRiskScoringEngine scoringEngine;

    public CompositeRiskDashboardServiceImpl(
            CompositeRiskDashboardMapper mapper,
            RiskScoringRuleMapper riskScoringRuleMapper,
            CompositeRiskScoringEngine scoringEngine
    ) {
        this.mapper = mapper;
        this.riskScoringRuleMapper = riskScoringRuleMapper;
        this.scoringEngine = scoringEngine;
    }

    @Override
    public Map<String, Object> getOverview() {
        List<Map<String, Object>> rules = riskScoringRuleMapper.listEnabledRules();
        List<Map<String, Object>> scoredCustomers = scoreCustomers(mapper.listRiskFeatures(), rules);
        List<Map<String, Object>> alerts = scoredCustomers.stream()
                .filter(row -> asInt(row.get("risk_score")) >= 45)
                .limit(ALERT_LIMIT)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analysis_model", "COMPOSITE_SCORING_FORECAST_RULE_V2");
        result.put("generated_at", LocalDateTime.now());
        result.put("rule_count", rules.size());
        result.put("summary", buildSummary(scoredCustomers));
        result.put("alerts", alerts);
        result.put("warning_trend", mapper.listWarningTrend());
        result.put("industry_distribution", buildIndustryDistribution(scoredCustomers));
        result.put("risk_migration", buildRiskMigration(scoredCustomers));
        return result;
    }

    @Override
    public List<Map<String, Object>> listCustomerScorings() {
        return scoreCustomers(mapper.listRiskFeatures(), riskScoringRuleMapper.listEnabledRules());
    }

    @Override
    public Map<String, Object> getCustomerScoring(String customerNo) {
        Map<String, Object> feature = mapper.findRiskFeatureByCustomerNo(customerNo);
        if (feature == null) {
            throw new IllegalArgumentException("未找到客户风险数据");
        }
        return scoringEngine.score(feature, riskScoringRuleMapper.listEnabledRules());
    }

    @Override
    public Map<String, Object> getCustomerScoringExplanation(String customerNo) {
        String normalizedCustomerNo = customerNo == null ? "" : customerNo.trim();
        if (normalizedCustomerNo.isEmpty()) {
            throw new IllegalArgumentException("瀹㈡埛缂栧彿涓嶈兘涓虹┖");
        }
        Map<String, Object> feature = mapper.findRiskFeatureByCustomerNo(normalizedCustomerNo);
        if (feature == null) {
            throw new IllegalArgumentException("鏈壘鍒板鎴烽闄╂暟鎹?");
        }
        List<Map<String, Object>> rules = riskScoringRuleMapper.listEnabledRules();
        Map<String, Object> explanation = scoringEngine.explain(feature, rules);
        explanation.put("analysis_model", "COMPOSITE_SCORING_FORECAST_RULE_V2");
        explanation.put("generated_at", LocalDateTime.now());
        explanation.put("rule_count", rules.size());
        return explanation;
    }

    @Override
    @Transactional
    public Map<String, Object> createTreatment(String customerNo) {
        String normalizedCustomerNo = customerNo == null ? "" : customerNo.trim();
        if (normalizedCustomerNo.isEmpty()) {
            throw new IllegalArgumentException("客户编号不能为空");
        }

        Map<String, Object> alert = getCustomerScoring(normalizedCustomerNo);
        if (asInt(alert.get("risk_score")) < 45) {
            throw new IllegalArgumentException("该客户当前未达到组合预警处置阈值");
        }

        String riskCode = "CREDIT-" + normalizedCustomerNo;
        Map<String, Object> existingPlan = mapper.findTreatmentPlanByRiskCode(riskCode);
        if (existingPlan != null) {
            return result(false, riskCode, existingPlan);
        }

        if (mapper.findRiskRegisterByCode(riskCode) == null) {
            Map<String, Object> riskRegister = new LinkedHashMap<>();
            riskRegister.put("risk_code", riskCode);
            riskRegister.put("risk_name", "对公客户组合风险 - " + value(alert.get("customer_name"), normalizedCustomerNo));
            riskRegister.put("category", "信贷风险");
            riskRegister.put("level", registerLevel(asInt(alert.get("risk_score"))));
            riskRegister.put("owner_department", value(alert.get("owner_org_name"), "风险管理部"));
            riskRegister.put("responsible_person", value(alert.get("relationship_manager_name"), "待分配"));
            riskRegister.put("status", "监控中");
            riskRegister.put("identified_at", LocalDate.now());
            riskRegister.put("due_date", dueDate(asInt(alert.get("risk_score"))));
            riskRegister.put("description", "组合预警评分 " + asInt(alert.get("risk_score")) + "。风险信号：" + value(alert.get("risk_signals"), "待核验"));
            mapper.insertCompositeRiskRegister(riskRegister);
        }

        Map<String, Object> treatment = new LinkedHashMap<>();
        treatment.put("plan_code", "TRT-" + normalizedCustomerNo);
        treatment.put("risk_code", riskCode);
        treatment.put("action", "核验组合风险信号，复核现金流、还款来源、押品覆盖与存量合同，并形成处置结论。");
        treatment.put("owner", value(alert.get("relationship_manager_name"), "风险管理部"));
        treatment.put("due_date", dueDate(asInt(alert.get("risk_score"))));
        treatment.put("progress", 0);
        treatment.put("status", "未开始");
        mapper.insertCompositeTreatmentPlan(treatment);
        return result(true, riskCode, mapper.findTreatmentPlanByRiskCode(riskCode));
    }

    private List<Map<String, Object>> scoreCustomers(List<Map<String, Object>> features, List<Map<String, Object>> rules) {
        List<Map<String, Object>> scored = new ArrayList<>();
        for (Map<String, Object> feature : features) {
            scored.add(scoringEngine.score(feature, rules));
        }
        scored.sort(Comparator
                .comparingInt((Map<String, Object> row) -> asInt(row.get("risk_score"))).reversed()
                .thenComparing(row -> value(row.get("customer_no"), "")));
        return scored;
    }

    private Map<String, Object> buildSummary(List<Map<String, Object>> customers) {
        int extreme = 0;
        int high = 0;
        int warning = 0;
        int forecastHigh = 0;
        int forecastUpgrade = 0;
        BigDecimal warningEad = BigDecimal.ZERO;
        BigDecimal highRiskEad = BigDecimal.ZERO;
        Map<String, BigDecimal> industryEad = new LinkedHashMap<>();

        for (Map<String, Object> customer : customers) {
            int score = asInt(customer.get("risk_score"));
            int forecastScore = asInt(customer.get("forecast_score"));
            BigDecimal ead = decimal(customer.get("ead_amount_total"));
            if (score >= 85) extreme++;
            if (score >= 65 && score < 85) high++;
            if (score >= 45) {
                warning++;
                warningEad = warningEad.add(ead);
            }
            if (forecastScore >= 65) {
                forecastHigh++;
                highRiskEad = highRiskEad.add(ead);
                String industry = value(customer.get("industry_name"), "未分类");
                industryEad.merge(industry, ead, BigDecimal::add);
            }
            if ("风险上迁".equals(customer.get("forecast_change"))) {
                forecastUpgrade++;
            }
        }

        String topIndustry = "-";
        BigDecimal topIndustryEad = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : industryEad.entrySet()) {
            if (entry.getValue().compareTo(topIndustryEad) > 0) {
                topIndustry = entry.getKey();
                topIndustryEad = entry.getValue();
            }
        }
        BigDecimal concentrationRatio = highRiskEad.signum() == 0 ? BigDecimal.ZERO
                : topIndustryEad.divide(highRiskEad, 4, java.math.RoundingMode.HALF_UP);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("customer_total", customers.size());
        summary.put("extreme_risk_count", extreme);
        summary.put("high_risk_count", high);
        summary.put("warning_customer_count", warning);
        summary.put("warning_ead_amount", warningEad);
        summary.put("forecast_high_risk_count", forecastHigh);
        summary.put("forecast_upgrade_count", forecastUpgrade);
        summary.put("top_industry_name", topIndustry);
        summary.put("top_industry_concentration", concentrationRatio);
        summary.put("open_treatment_count", mapper.countOpenTreatmentPlans());
        return summary;
    }

    private List<Map<String, Object>> buildIndustryDistribution(List<Map<String, Object>> customers) {
        Map<String, Map<String, Object>> aggregates = new LinkedHashMap<>();
        for (Map<String, Object> customer : customers) {
            String industry = value(customer.get("industry_name"), "未分类");
            Map<String, Object> aggregate = aggregates.computeIfAbsent(industry, key -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("industry_name", key);
                value.put("customer_count", 0);
                value.put("high_risk_count", 0);
                value.put("high_risk_ead_amount", BigDecimal.ZERO);
                value.put("forecast_upgrade_count", 0);
                return value;
            });
            aggregate.put("customer_count", asInt(aggregate.get("customer_count")) + 1);
            if (asInt(customer.get("risk_score")) >= 65) {
                aggregate.put("high_risk_count", asInt(aggregate.get("high_risk_count")) + 1);
                aggregate.put("high_risk_ead_amount", decimal(aggregate.get("high_risk_ead_amount")).add(decimal(customer.get("ead_amount_total"))));
            }
            if ("风险上迁".equals(customer.get("forecast_change"))) {
                aggregate.put("forecast_upgrade_count", asInt(aggregate.get("forecast_upgrade_count")) + 1);
            }
        }
        return aggregates.values().stream()
                .sorted(Comparator
                        .comparingInt((Map<String, Object> row) -> asInt(row.get("high_risk_count"))).reversed()
                        .thenComparing(row -> decimal(row.get("high_risk_ead_amount")), Comparator.reverseOrder()))
                .limit(INDUSTRY_LIMIT)
                .toList();
    }

    private List<Map<String, Object>> buildRiskMigration(List<Map<String, Object>> customers) {
        String[] levels = {"极高风险", "高风险", "中风险", "低风险"};
        Map<String, Map<String, Object>> rows = new LinkedHashMap<>();
        for (String level : levels) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("risk_level", level);
            row.put("current_count", 0);
            row.put("forecast_count", 0);
            row.put("upgrade_count", 0);
            rows.put(level, row);
        }
        for (Map<String, Object> customer : customers) {
            Map<String, Object> current = rows.get(customer.get("risk_level"));
            Map<String, Object> forecast = rows.get(customer.get("forecast_level"));
            if (current != null) current.put("current_count", asInt(current.get("current_count")) + 1);
            if (forecast != null) forecast.put("forecast_count", asInt(forecast.get("forecast_count")) + 1);
            if ("风险上迁".equals(customer.get("forecast_change")) && forecast != null) {
                forecast.put("upgrade_count", asInt(forecast.get("upgrade_count")) + 1);
            }
        }
        return new ArrayList<>(rows.values());
    }

    private Map<String, Object> result(boolean created, String riskCode, Map<String, Object> treatment) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("risk_code", riskCode);
        result.put("treatment", treatment);
        return result;
    }

    private LocalDate dueDate(int score) {
        if (score >= 85) return LocalDate.now().plusDays(3);
        if (score >= 65) return LocalDate.now().plusDays(7);
        return LocalDate.now().plusDays(15);
    }

    private String registerLevel(int score) {
        if (score >= 85) return "重大";
        if (score >= 65) return "高";
        if (score >= 45) return "中";
        return "低";
    }

    private int asInt(Object value) {
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private BigDecimal decimal(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
        try {
            return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }

    private String value(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }
}
