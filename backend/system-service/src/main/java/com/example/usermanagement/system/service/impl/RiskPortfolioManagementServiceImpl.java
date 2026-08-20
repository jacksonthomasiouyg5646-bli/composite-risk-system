package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.RiskPortfolioMapper;
import com.example.usermanagement.system.service.CompositeRiskDashboardService;
import com.example.usermanagement.system.service.RiskPortfolioManagementService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RiskPortfolioManagementServiceImpl implements RiskPortfolioManagementService {
    private final RiskPortfolioMapper mapper;
    private final CompositeRiskDashboardService compositeRiskDashboardService;
    private final ObjectMapper objectMapper;

    public RiskPortfolioManagementServiceImpl(RiskPortfolioMapper mapper, CompositeRiskDashboardService compositeRiskDashboardService, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.compositeRiskDashboardService = compositeRiskDashboardService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Map<String, Object> getOverview(String username) {
        ensureBaselineLimits();
        Map<String, Object> summary = mapper.getPortfolioSummary();
        List<Map<String, Object>> industries = mapper.listIndustryConcentration();
        List<Map<String, Object>> products = mapper.listProductConcentration();
        List<Map<String, Object>> organizations = mapper.listOrganizationConcentration();
        List<Map<String, Object>> limits = evaluateLimits(summary, industries, products, organizations, false, "SYSTEM");
        applyLimitForecast(limits, mapper.listLimitForecast(), summary);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", portfolioSummary(summary, industries, limits));
        result.put("industry_concentration", industries);
        result.put("product_concentration", products);
        result.put("organization_concentration", organizations);
        result.put("top_customers", mapper.listTopCustomerConcentration(10));
        result.put("limit_monitor", limits);
        result.put("latest_backtest", mapper.listLatestBacktestRuns());
        result.put("latest_effectiveness", mapper.listLatestAlertEffectiveness());
        result.put("stress_scenarios", mapper.listStressScenarios());
        result.put("latest_stress_results", mapper.listLatestStressResults());
        result.put("group_risk", mapper.listGroupRiskOverview());
        result.put("model_lifecycle", mapper.listModelLifecycle());
        result.put("model_lifecycle_summary", mapper.getModelLifecycleSummary());
        result.put("effectiveness_metrics", mapper.getAlertEffectivenessMetrics());
        result.put("workbench_preference", mapper.getWorkbenchPreference(operator(username)));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> captureLimitSnapshot(String operator) {
        ensureBaselineLimits();
        Map<String, Object> summary = mapper.getPortfolioSummary();
        List<Map<String, Object>> rows = evaluateLimits(summary, mapper.listIndustryConcentration(), mapper.listProductConcentration(), mapper.listOrganizationConcentration(), true, operator);
        applyLimitForecast(rows, mapper.listLimitForecast(), summary);
        return Map.of("captured_count", rows.size(), "limit_monitor", rows, "summary", limitSummary(rows), "data_date", value(mapper.getLatestPortfolioDataDate(), LocalDate.now().toString()));
    }

    @Override
    @Transactional
    public Map<String, Object> updateLimit(Long id, Map<String, Object> body, String operator) {
        Map<String, Object> current = mapper.listPortfolioLimits().stream().filter(item -> asLong(item.get("id")).equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到组合限额"));
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("limit_amount", positive(body == null ? null : body.get("limit_amount"), decimal(current.get("limit_amount"))));
        BigDecimal ratio = decimal(body == null ? null : body.get("warning_ratio"));
        if (ratio.signum() <= 0) ratio = decimal(current.get("warning_ratio"));
        if (ratio.compareTo(BigDecimal.ONE) > 0) throw new IllegalArgumentException("预警比例不能超过 100%");
        normalized.put("warning_ratio", ratio);
        normalized.put("owner", value(body == null ? null : body.get("owner"), value(current.get("owner"), operator(operator))));
        normalized.put("status", value(body == null ? null : body.get("status"), value(current.get("status"), "ENABLED")).toUpperCase());
        normalized.put("expiry_date", body == null ? current.get("expiry_date") : body.getOrDefault("expiry_date", current.get("expiry_date")));
        mapper.updatePortfolioLimit(id, normalized);
        return captureLimitSnapshot(operator);
    }

    @Override
    @Transactional
    public Map<String, Object> runBacktest(String operator) {
        String runCode = "BT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        LocalDate asOfDate = parseDate(mapper.getLatestPortfolioDataDate());
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.addAll(tagSegments(mapper.listProductBacktestSegments(), "PRODUCT"));
        rows.addAll(tagSegments(mapper.listIndustryBacktestSegments(), "INDUSTRY"));
        for (Map<String, Object> row : rows) {
            BigDecimal expectedPd = decimal(row.get("expected_pd"));
            BigDecimal observedPd = decimal(row.get("observed_default_rate"));
            BigDecimal expectedLgd = decimal(row.get("expected_lgd"));
            BigDecimal observedLgd = decimal(row.get("observed_lgd"));
            BigDecimal predictedEad = decimal(row.get("predicted_default_ead"));
            BigDecimal observedEad = decimal(row.get("observed_default_ead"));
            BigDecimal pdBias = observedPd.subtract(expectedPd).setScale(6, RoundingMode.HALF_UP);
            BigDecimal lgdBias = observedLgd.subtract(expectedLgd).setScale(6, RoundingMode.HALF_UP);
            BigDecimal eadBias = predictedEad.signum() == 0 ? BigDecimal.ZERO : observedEad.subtract(predictedEad).divide(predictedEad, 6, RoundingMode.HALF_UP);
            String status = calibrationStatus(pdBias, lgdBias, eadBias);
            row.put("run_code", runCode); row.put("as_of_date", asOfDate); row.put("pd_bias", pdBias); row.put("lgd_bias", lgdBias); row.put("ead_bias", eadBias);
            row.put("calibration_status", status); row.put("recommendation", calibrationRecommendation(status, pdBias, lgdBias, eadBias)); row.put("run_by", operator(operator));
            mapper.insertBacktestRun(row);
        }
        return Map.of("run_code", runCode, "segment_count", rows.size(), "results", mapper.listLatestBacktestRuns());
    }

    @Override
    @Transactional
    public Map<String, Object> runAlertEffectiveness(String operator) {
        LocalDate evaluationDate = LocalDate.now();
        int count = 0;
        for (Map<String, Object> base : mapper.listClosedAlertEffectivenessBases()) {
            Map<String, Object> current = compositeRiskDashboardService.getCustomerScoring(value(base.get("customer_no"), ""));
            int baseline = integer(base.get("baseline_risk_score"));
            int currentScore = integer(current.get("risk_score"));
            int days = (int) Math.max(0, ChronoUnit.DAYS.between(parseDateTime(base.get("closed_at")).toLocalDate(), evaluationDate));
            int overdue = integer(current.get("overdue_count"));
            int defaults = integer(current.get("debt_default_count"));
            String status = effectivenessStatus(days, baseline, currentScore, overdue, defaults);
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("alert_case_id", asLong(base.get("alert_case_id"))); snapshot.put("evaluation_date", evaluationDate); snapshot.put("evaluation_window_days", 30);
            snapshot.put("closed_at", base.get("closed_at")); snapshot.put("baseline_risk_score", baseline); snapshot.put("current_risk_score", currentScore); snapshot.put("score_delta", currentScore - baseline);
            snapshot.put("current_pd", decimal(current.get("max_pd"))); snapshot.put("current_lgd_downturn", decimal(base.get("current_lgd_downturn")));
            snapshot.put("overdue_count", overdue); snapshot.put("debt_default_count", defaults); snapshot.put("effectiveness_status", status);
            snapshot.put("conclusion", effectivenessConclusion(days, status, baseline, currentScore, overdue, defaults)); snapshot.put("evaluated_by", operator(operator));
            mapper.upsertAlertEffectiveness(snapshot); count++;
        }
        List<Map<String, Object>> results = mapper.listLatestAlertEffectiveness();
        return Map.of("evaluated_count", count, "summary", effectivenessSummary(results), "results", results);
    }

    @Override
    @Transactional
    public Map<String, Object> runStressTest(String scenarioCode, String operator) {
        Map<String, Object> scenario = mapper.findStressScenario(value(scenarioCode, "MILD_DOWNTURN"));
        if (scenario == null) throw new IllegalArgumentException("未找到可用压力情景");
        String runCode = "ST-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        BigDecimal pdMultiplier = decimal(scenario.get("pd_multiplier"));
        BigDecimal lgdAddon = decimal(scenario.get("lgd_addon"));
        BigDecimal eadMultiplier = decimal(scenario.get("ead_multiplier"));
        BigDecimal haircut = decimal(scenario.get("collateral_haircut"));
        BigDecimal totalIncremental = BigDecimal.ZERO;
        int breachCount = 0;
        for (Map<String, Object> base : mapper.listStressSegments()) {
            BigDecimal baseEad = decimal(base.get("base_ead"));
            BigDecimal stressedEad = baseEad.multiply(eadMultiplier).setScale(2, RoundingMode.HALF_UP);
            BigDecimal stressedPd = decimal(base.get("weighted_pd")).multiply(pdMultiplier).min(BigDecimal.ONE).setScale(6, RoundingMode.HALF_UP);
            BigDecimal stressedLgd = decimal(base.get("weighted_lgd")).add(lgdAddon).min(BigDecimal.ONE).setScale(6, RoundingMode.HALF_UP);
            BigDecimal stressedEl = stressedEad.multiply(stressedPd).multiply(stressedLgd).setScale(2, RoundingMode.HALF_UP);
            BigDecimal incremental = stressedEl.subtract(decimal(base.get("base_el"))).setScale(2, RoundingMode.HALF_UP);
            BigDecimal stressedCollateral = decimal(base.get("collateral_value")).multiply(BigDecimal.ONE.subtract(haircut)).setScale(2, RoundingMode.HALF_UP);
            boolean breach = mapper.listPortfolioLimits().stream().anyMatch(limit -> "INDUSTRY".equals(limit.get("dimension_type"))
                    && value(base.get("segment_code"), "").equals(value(limit.get("scope_code"), "")) && "EAD".equals(limit.get("metric_code"))
                    && stressedEad.compareTo(decimal(limit.get("limit_amount"))) > 0);
            Map<String, Object> row = new LinkedHashMap<>(base);
            row.put("run_code", runCode); row.put("scenario_code", scenario.get("scenario_code")); row.put("as_of_date", parseDate(mapper.getLatestPortfolioDataDate()));
            row.put("stressed_ead", stressedEad); row.put("stressed_el", stressedEl); row.put("incremental_loss", incremental);
            row.put("stressed_pd", stressedPd); row.put("stressed_lgd", stressedLgd); row.put("stressed_collateral", stressedCollateral);
            row.put("limit_breach_flag", breach ? 1 : 0); row.put("run_by", operator(operator)); mapper.insertStressResult(row);
            totalIncremental = totalIncremental.add(incremental); if (breach) breachCount++;
        }
        return Map.of("run_code", runCode, "scenario", scenario, "incremental_loss", totalIncremental,
                "breach_count", breachCount, "results", mapper.listLatestStressResults());
    }

    @Override
    public Map<String, Object> getGroupMembers(String groupCode) {
        return Map.of("group_code", groupCode, "members", mapper.listGroupMembers(groupCode));
    }

    @Override
    @Transactional
    public Map<String, Object> saveWorkbenchPreference(Map<String, Object> body, String username) {
        Map<String, Object> preference = new LinkedHashMap<>();
        preference.put("username", operator(username));
        preference.put("workspace_role", value(body == null ? null : body.get("workspace_role"), "RISK_MANAGER"));
        preference.put("default_tab", value(body == null ? null : body.get("default_tab"), "limits"));
        preference.put("density_mode", value(body == null ? null : body.get("density_mode"), "COMFORTABLE"));
        Object modules = body == null ? null : body.get("visible_modules");
        preference.put("visible_modules_json", json(modules == null ? List.of("limits", "forecast", "stress", "groups", "backtest", "effectiveness", "lifecycle") : modules));
        preference.put("updated_by", operator(username));
        mapper.upsertWorkbenchPreference(preference);
        return mapper.getWorkbenchPreference(operator(username));
    }

    @Scheduled(cron = "${risk.portfolio.daily-cron:0 20 8 * * *}", zone = "Asia/Shanghai")
    @Transactional
    public void scheduledPortfolioMonitoring() {
        captureLimitSnapshot("SYSTEM");
        runBacktest("SYSTEM");
        runAlertEffectiveness("SYSTEM");
    }

    private void ensureBaselineLimits() {
        List<Map<String, Object>> current = mapper.listPortfolioLimits();
        ensureDimensionLimits(current, mapper.listIndustryConcentration(), "INDUSTRY");
        ensureDimensionLimits(current, mapper.listProductConcentration(), "PRODUCT");
        ensureDimensionLimits(current, mapper.listOrganizationConcentration(), "ORGANIZATION");
    }

    private void ensureDimensionLimits(List<Map<String, Object>> current, List<Map<String, Object>> dimensions, String type) {
        for (Map<String, Object> dimension : dimensions) {
            for (String metric : List.of("EAD", "EL_DOWN")) {
                boolean exists = current.stream().anyMatch(limit -> type.equals(limit.get("dimension_type")) && value(dimension.get("scope_code"), "").equals(value(limit.get("scope_code"), "")) && metric.equals(limit.get("metric_code")));
                BigDecimal actual = "EAD".equals(metric) ? decimal(dimension.get("ead_amount")) : decimal(dimension.get("el_downturn_amount"));
                if (exists || actual.signum() == 0) continue;
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("dimension_type", type); body.put("scope_code", dimension.get("scope_code")); body.put("scope_name", dimension.get("scope_name")); body.put("metric_code", metric);
                body.put("limit_amount", actual.multiply(new BigDecimal("1.30")).setScale(2, RoundingMode.HALF_UP)); body.put("warning_ratio", new BigDecimal("0.800000"));
                body.put("owner", "风险管理部"); body.put("status", "ENABLED"); body.put("effective_date", LocalDate.now()); body.put("expiry_date", null);
                mapper.insertPortfolioLimit(body);
            }
        }
    }

    private List<Map<String, Object>> evaluateLimits(Map<String, Object> summary, List<Map<String, Object>> industries, List<Map<String, Object>> products, List<Map<String, Object>> organizations, boolean capture, String operator) {
        Map<String, Map<String, Object>> dimensions = new LinkedHashMap<>();
        indexDimensions(dimensions, industries, "INDUSTRY"); indexDimensions(dimensions, products, "PRODUCT"); indexDimensions(dimensions, organizations, "ORGANIZATION");
        BigDecimal totalEad = decimal(summary.get("ead_amount")); BigDecimal totalEl = decimal(summary.get("el_downturn_amount")); LocalDate dataDate = parseDate(mapper.getLatestPortfolioDataDate());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> limit : mapper.listPortfolioLimits()) {
            Map<String, Object> row = new LinkedHashMap<>(limit); Map<String, Object> dimension = dimensions.get(value(limit.get("dimension_type"), "") + "|" + value(limit.get("scope_code"), ""));
            BigDecimal actual = dimension == null ? BigDecimal.ZERO : ("EAD".equals(limit.get("metric_code")) ? decimal(dimension.get("ead_amount")) : decimal(dimension.get("el_downturn_amount")));
            BigDecimal total = "EAD".equals(limit.get("metric_code")) ? totalEad : totalEl; BigDecimal limitAmount = decimal(limit.get("limit_amount"));
            BigDecimal utilization = limitAmount.signum() == 0 ? BigDecimal.ZERO : actual.divide(limitAmount, 6, RoundingMode.HALF_UP); BigDecimal concentration = total.signum() == 0 ? BigDecimal.ZERO : actual.divide(total, 6, RoundingMode.HALF_UP);
            String status = "ENABLED".equals(limit.get("status")) ? (utilization.compareTo(BigDecimal.ONE) > 0 ? "BREACH" : utilization.compareTo(decimal(limit.get("warning_ratio"))) >= 0 ? "WARNING" : "NORMAL") : "DISABLED";
            row.put("actual_amount", actual); row.put("portfolio_total_amount", total); row.put("utilization_ratio", utilization); row.put("concentration_ratio", concentration); row.put("monitor_status", status); result.add(row);
            if (capture) { Map<String, Object> snapshot = new LinkedHashMap<>(); snapshot.put("limit_id", limit.get("id")); snapshot.put("data_date", dataDate); snapshot.put("actual_amount", actual); snapshot.put("portfolio_total_amount", total); snapshot.put("utilization_ratio", utilization); snapshot.put("concentration_ratio", concentration); snapshot.put("monitor_status", status); snapshot.put("evidence_json", json(Map.of("scope_name", limit.get("scope_name"), "metric", limit.get("metric_code")))); snapshot.put("captured_by", operator(operator)); mapper.upsertPortfolioLimitSnapshot(snapshot); }
        }
        return result;
    }

    private void indexDimensions(Map<String, Map<String, Object>> target, List<Map<String, Object>> rows, String type) { for (Map<String, Object> row : rows) target.put(type + "|" + value(row.get("scope_code"), ""), row); }
    private void applyLimitForecast(List<Map<String, Object>> limits, List<Map<String, Object>> forecasts, Map<String, Object> summary) {
        Map<String, Map<String, Object>> index = new LinkedHashMap<>();
        for (Map<String, Object> forecast : forecasts) index.put(value(forecast.get("dimension_type"), "") + "|" + value(forecast.get("scope_code"), ""), forecast);
        BigDecimal ead = decimal(summary.get("ead_amount"));
        BigDecimal lossRate = ead.signum() == 0 ? BigDecimal.ZERO : decimal(summary.get("el_downturn_amount")).divide(ead, 8, RoundingMode.HALF_UP);
        for (Map<String, Object> row : limits) {
            Map<String, Object> forecast = index.get(value(row.get("dimension_type"), "") + "|" + value(row.get("scope_code"), ""));
            BigDecimal reservationEad = forecast == null ? BigDecimal.ZERO : decimal(forecast.get("reserved_amount"));
            BigDecimal reservation = "EAD".equals(row.get("metric_code")) ? reservationEad : reservationEad.multiply(lossRate);
            BigDecimal projected = decimal(row.get("actual_amount")).add(reservation).setScale(2, RoundingMode.HALF_UP);
            BigDecimal limit = decimal(row.get("limit_amount"));
            BigDecimal utilization = limit.signum() == 0 ? BigDecimal.ZERO : projected.divide(limit, 6, RoundingMode.HALF_UP);
            String status = utilization.compareTo(BigDecimal.ONE) > 0 ? "BREACH" : utilization.compareTo(decimal(row.get("warning_ratio"))) >= 0 ? "WARNING" : "NORMAL";
            row.put("reserved_amount", reservation.setScale(2, RoundingMode.HALF_UP)); row.put("projected_amount", projected);
            row.put("available_amount", limit.subtract(projected).max(BigDecimal.ZERO)); row.put("forecast_utilization_ratio", utilization);
            row.put("forecast_status", status); row.put("forecast_application_count", forecast == null ? 0 : forecast.get("application_count"));
        }
    }
    private List<Map<String, Object>> tagSegments(List<Map<String, Object>> rows, String type) { for (Map<String, Object> row : rows) row.put("segment_type", type); return rows; }
    private Map<String, Object> portfolioSummary(Map<String, Object> base, List<Map<String, Object>> industries, List<Map<String, Object>> limits) { Map<String, Object> result = new LinkedHashMap<>(base); BigDecimal total=decimal(base.get("ead_amount")); BigDecimal hhi=BigDecimal.ZERO; for(Map<String,Object> row:industries){BigDecimal share=total.signum()==0?BigDecimal.ZERO:decimal(row.get("ead_amount")).divide(total,6,RoundingMode.HALF_UP); hhi=hhi.add(share.multiply(share));} result.put("industry_hhi",hhi.setScale(6,RoundingMode.HALF_UP)); result.putAll(limitSummary(limits)); return result; }
    private Map<String, Object> limitSummary(List<Map<String, Object>> rows) { int warning=0,breach=0; for(Map<String,Object> row:rows){if("WARNING".equals(row.get("monitor_status")))warning++;if("BREACH".equals(row.get("monitor_status")))breach++;} return Map.of("limit_total",rows.size(),"limit_warning_count",warning,"limit_breach_count",breach); }
    private Map<String, Object> effectivenessSummary(List<Map<String, Object>> rows) { Map<String,Integer> counts=new LinkedHashMap<>(); for(Map<String,Object> row:rows) counts.merge(value(row.get("effectiveness_status"),"OBSERVING"),1,Integer::sum); return Map.of("total_count",rows.size(),"status_counts",counts); }
    private String calibrationStatus(BigDecimal pd, BigDecimal lgd, BigDecimal ead) { if(pd.abs().compareTo(new BigDecimal("0.030000"))>=0||lgd.abs().compareTo(new BigDecimal("0.100000"))>=0||ead.abs().compareTo(new BigDecimal("0.150000"))>=0)return "RECALIBRATE"; if(pd.abs().compareTo(new BigDecimal("0.015000"))>=0||lgd.abs().compareTo(new BigDecimal("0.050000"))>=0||ead.abs().compareTo(new BigDecimal("0.080000"))>=0)return "WATCH"; return "STABLE"; }
    private String calibrationRecommendation(String status, BigDecimal pd, BigDecimal lgd, BigDecimal ead) { return "RECALIBRATE".equals(status)?"偏差超过校准阈值，建议复核分段参数与样本口径。":"WATCH".equals(status)?"偏差进入关注区间，建议持续观察下一批回溯结果。":"当前分段偏差处于稳定区间。"; }
    private String effectivenessStatus(int days,int baseline,int current,int overdue,int defaults){if(days<30)return "OBSERVING";if(current<=baseline-15&&overdue==0&&defaults==0)return "EFFECTIVE";if(current<baseline)return "PARTIAL";return "INEFFECTIVE";}
    private String effectivenessConclusion(int days,String status,int baseline,int current,int overdue,int defaults){return "关闭后 "+days+" 天，风险评分 "+baseline+"→"+current+"，逾期 "+overdue+" 笔，违约 "+defaults+" 笔；评价："+status+"。";}
    private String json(Object value){try{return objectMapper.writeValueAsString(value);}catch(JsonProcessingException ex){throw new IllegalStateException("组合风险结果序列化失败",ex);}}
    private LocalDate parseDate(Object value){try{return LocalDate.parse(value(value,LocalDate.now().toString()).substring(0,10));}catch(Exception ex){return LocalDate.now();}}
    private LocalDateTime parseDateTime(Object value){if(value instanceof LocalDateTime dt)return dt;try{return LocalDateTime.parse(String.valueOf(value).replace(' ','T').substring(0,19));}catch(Exception ex){return LocalDateTime.now();}}
    private BigDecimal positive(Object value,BigDecimal fallback){BigDecimal result=decimal(value);return result.signum()>0?result:fallback;}
    private BigDecimal decimal(Object value){try{return value==null||String.valueOf(value).isBlank()?BigDecimal.ZERO:value instanceof BigDecimal number?number:new BigDecimal(String.valueOf(value));}catch(Exception ex){return BigDecimal.ZERO;}}
    private int integer(Object value){return decimal(value).intValue();}
    private Long asLong(Object value){return value instanceof Number number?number.longValue():Long.valueOf(String.valueOf(value));}
    private String value(Object value,String fallback){String text=value==null?"":String.valueOf(value).trim();return text.isBlank()?fallback:text;}
    private String operator(String operator){return value(operator,"SYSTEM");}
}
