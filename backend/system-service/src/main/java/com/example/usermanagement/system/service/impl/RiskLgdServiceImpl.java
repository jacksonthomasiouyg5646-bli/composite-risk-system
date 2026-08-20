package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.common.api.PageResult;
import com.example.usermanagement.system.mapper.RiskLgdMapper;
import com.example.usermanagement.system.service.RiskLgdService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RiskLgdServiceImpl implements RiskLgdService {
    private final RiskLgdMapper mapper;
    private final ObjectMapper objectMapper;

    public RiskLgdServiceImpl(RiskLgdMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", mapper.getLgdOverviewSummary());
        result.put("product_distribution", mapper.listProductDistribution());
        result.put("industry_distribution", mapper.listIndustryDistribution());
        result.put("high_lgd_exposures", mapper.listHighLgdExposures(12));
        return result;
    }

    @Override
    public PageResult<Map<String, Object>> listLedger(Map<String, Object> filters, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Map<String, Object> normalized = new LinkedHashMap<>(filters == null ? Map.of() : filters);
        normalizeFilter(normalized, "keyword");
        normalizeFilter(normalized, "customerNo");
        normalizeFilter(normalized, "productType");
        normalizeFilter(normalized, "industryName");
        normalizeFilter(normalized, "ratingResult");
        if (normalized.containsKey("minLgdDownturn")) normalized.put("minLgdDownturn", decimal(normalized.get("minLgdDownturn")));
        if (normalized.containsKey("defaultFlag")) normalized.put("defaultFlag", integer(normalized.get("defaultFlag")));
        return new PageResult<>(mapper.listLgdLedger(normalized, safeSize, (safePage - 1) * safeSize), mapper.countLgdLedger(normalized), safePage, safeSize);
    }

    @Override
    public Map<String, Object> getDebtDetail(String debtNo) {
        Map<String, Object> detail = mapper.getDebtLgdDetail(text(debtNo));
        if (detail == null) throw new IllegalArgumentException("未找到债项 LGD 敞口数据");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("detail", detail);
        result.put("collaterals", mapper.listDebtCollaterals(text(debtNo)));
        result.put("overdues", mapper.listDebtOverdues(text(debtNo)));
        result.put("defaults", mapper.listDebtDefaults(text(debtNo)));
        return result;
    }

    @Override
    public Map<String, Object> getCustomerLgd(String customerNo) {
        String normalized = text(customerNo);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", mapper.getCustomerLgdSummary(normalized));
        result.put("exposures", mapper.listCustomerLgdExposures(normalized));
        return result;
    }

    @Override
    public Map<String, Object> getGovernance() {
        Map<String, Object> version = ensureBaselineVersion();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("published_version", version);
        result.put("parameters", mapper.listLgdSegmentParameters(asLong(version.get("id"))));
        result.put("versions", mapper.listLgdModelVersions(20));
        result.put("calculation_runs", mapper.listLgdCalculationRuns(20));
        result.put("stress_test_runs", mapper.listLgdStressTestRuns(10));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> captureCalculationRun(String operator) {
        Map<String, Object> version = ensureBaselineVersion();
        Map<String, Object> summary = mapper.getLgdOverviewSummary();
        Map<String, Object> run = new LinkedHashMap<>();
        run.put("model_version_id", asLong(version.get("id")));
        run.put("run_type", "BASELINE");
        run.put("source_data_date", mapper.getLatestLgdDataDate());
        run.put("exposure_count", integer(summary.get("exposure_count")));
        run.put("ead_amount", decimal(summary.get("ead_amount_total")));
        run.put("weighted_lgd_avg", decimal(summary.get("weighted_lgd_avg")));
        run.put("weighted_lgd_downturn", decimal(summary.get("weighted_lgd_downturn")));
        run.put("el_avg_amount", decimal(summary.get("el_avg_amount")));
        run.put("el_downturn_amount", decimal(summary.get("el_downturn_amount")));
        run.put("result_summary", json(summary));
        run.put("run_by", operator(operator));
        mapper.insertLgdCalculationRun(run);
        run.put("version_code", version.get("version_code"));
        return run;
    }

    @Override
    @Transactional
    public Map<String, Object> runStressTest(Map<String, Object> body, String operator) {
        String scenarioCode = text(body == null ? null : body.get("scenario_code")).toUpperCase();
        if (scenarioCode.isBlank()) scenarioCode = "COLLATERAL_HAIRCUT";
        Map<String, Object> parameters = stressParameters(scenarioCode, body);
        BigDecimal lgdAddition = decimal(parameters.get("lgd_addition"));
        BigDecimal recoveryHaircut = decimal(parameters.get("recovery_haircut"));
        BigDecimal collateralHaircut = decimal(parameters.get("collateral_haircut"));
        int currentHigh = 0;
        int stressedHigh = 0;
        BigDecimal currentEl = BigDecimal.ZERO;
        BigDecimal stressedEl = BigDecimal.ZERO;
        BigDecimal currentLgdTotal = BigDecimal.ZERO;
        BigDecimal stressedLgdTotal = BigDecimal.ZERO;
        BigDecimal totalEad = BigDecimal.ZERO;
        List<Map<String, Object>> impacts = new ArrayList<>();
        List<Map<String, Object>> allRows = mapper.listAllLgdLedger();
        for (Map<String, Object> row : allRows) {
            BigDecimal ead = decimal(row.get("ead_amount"));
            BigDecimal pd = decimal(row.get("pd"));
            BigDecimal lgdDownturn = decimal(row.get("lgd_downturn"));
            BigDecimal recovery = decimal(row.get("product_recovery_rate"));
            BigDecimal coverage = decimal(row.get("coverage_rate"));
            BigDecimal coverageImpact = BigDecimal.ONE.subtract(coverage.min(BigDecimal.ONE)).max(BigDecimal.ZERO)
                    .multiply(collateralHaircut).multiply(new BigDecimal("0.20"));
            BigDecimal recoveryImpact = BigDecimal.ONE.subtract(recovery.min(BigDecimal.ONE)).max(BigDecimal.ZERO)
                    .multiply(recoveryHaircut).multiply(new BigDecimal("0.15"));
            BigDecimal stressedLgd = lgdDownturn.add(lgdAddition).add(coverageImpact).add(recoveryImpact).min(BigDecimal.ONE);
            BigDecimal currentDebtEl = pd.multiply(lgdDownturn).multiply(ead);
            BigDecimal stressedDebtEl = pd.multiply(stressedLgd).multiply(ead);
            if (lgdDownturn.compareTo(new BigDecimal("0.600000")) >= 0) currentHigh++;
            if (stressedLgd.compareTo(new BigDecimal("0.600000")) >= 0) stressedHigh++;
            currentEl = currentEl.add(currentDebtEl);
            stressedEl = stressedEl.add(stressedDebtEl);
            currentLgdTotal = currentLgdTotal.add(ead.multiply(lgdDownturn));
            stressedLgdTotal = stressedLgdTotal.add(ead.multiply(stressedLgd));
            totalEad = totalEad.add(ead);
            BigDecimal delta = stressedDebtEl.subtract(currentDebtEl);
            if (delta.signum() > 0) {
                Map<String, Object> impact = new LinkedHashMap<>();
                impact.put("debt_no", row.get("debt_no"));
                impact.put("customer_no", row.get("customer_no"));
                impact.put("customer_name", row.get("customer_name"));
                impact.put("product_type", row.get("product_type"));
                impact.put("ead_amount", ead);
                impact.put("current_lgd_downturn", lgdDownturn);
                impact.put("stressed_lgd_downturn", stressedLgd);
                impact.put("el_delta", delta.setScale(2, RoundingMode.HALF_UP));
                impacts.add(impact);
            }
        }
        impacts.sort(Comparator.comparing((Map<String, Object> row) -> decimal(row.get("el_delta"))).reversed());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("exposure_count", allRows.size());
        summary.put("current_high_lgd_count", currentHigh);
        summary.put("stressed_high_lgd_count", stressedHigh);
        summary.put("high_lgd_delta", stressedHigh - currentHigh);
        summary.put("current_weighted_lgd_downturn", divide(currentLgdTotal, totalEad));
        summary.put("stressed_weighted_lgd_downturn", divide(stressedLgdTotal, totalEad));
        summary.put("current_el_downturn", currentEl.setScale(2, RoundingMode.HALF_UP));
        summary.put("stressed_el_downturn", stressedEl.setScale(2, RoundingMode.HALF_UP));
        summary.put("el_delta", stressedEl.subtract(currentEl).setScale(2, RoundingMode.HALF_UP));
        summary.put("impact_samples", impacts.stream().limit(12).toList());
        Map<String, Object> run = new LinkedHashMap<>();
        run.put("scenario_code", scenarioCode);
        run.put("scenario_name", scenarioName(scenarioCode));
        run.put("parameter_json", json(parameters));
        run.put("summary_json", json(summary));
        run.put("run_by", operator(operator));
        mapper.insertLgdStressTestRun(run);
        return Map.of("run", run, "parameters", parameters, "summary", summary);
    }

    @Scheduled(cron = "${risk.lgd.daily-run-cron:0 10 8 * * *}", zone = "Asia/Shanghai")
    public void scheduledCalculationRun() {
        captureCalculationRun("SYSTEM");
    }

    private Map<String, Object> ensureBaselineVersion() {
        Map<String, Object> current = mapper.getPublishedLgdModelVersion();
        if (current != null) return current;
        Map<String, Object> summary = mapper.getLgdOverviewSummary();
        Map<String, Object> version = new LinkedHashMap<>();
        version.put("version_code", "LGD-BASELINE-V1");
        version.put("version_name", "LGD 参数基线版本 V1");
        version.put("status", "PUBLISHED");
        version.put("source_description", "基于 corporate_risk_exposure 当前有效债项敞口的 EAD 加权 LGD 与产品回收率生成");
        version.put("created_by", "SYSTEM");
        version.put("approved_by", "SYSTEM");
        version.put("approved_at", LocalDateTime.now());
        version.put("effective_date", LocalDate.now());
        mapper.insertLgdModelVersion(version);
        Map<String, Object> all = new LinkedHashMap<>();
        all.put("version_id", asLong(version.get("id")));
        all.put("segment_type", "ALL");
        all.put("segment_code", "ALL");
        all.put("segment_name", "全量有效风险敞口");
        all.put("lgd_avg", decimal(summary.get("weighted_lgd_avg")));
        all.put("lgd_downturn", decimal(summary.get("weighted_lgd_downturn")));
        all.put("recovery_rate", decimal(summary.get("weighted_recovery_rate")));
        all.put("collateral_haircut", BigDecimal.ZERO);
        all.put("enabled", 1);
        mapper.insertLgdSegmentParameter(all);
        for (Map<String, Object> product : mapper.listProductDistribution()) {
            Map<String, Object> parameter = new LinkedHashMap<>();
            parameter.put("version_id", asLong(version.get("id")));
            parameter.put("segment_type", "PRODUCT");
            parameter.put("segment_code", product.get("product_type"));
            parameter.put("segment_name", product.get("product_type"));
            parameter.put("lgd_avg", decimal(product.get("weighted_lgd_avg")));
            parameter.put("lgd_downturn", decimal(product.get("weighted_lgd_downturn")));
            parameter.put("recovery_rate", decimal(product.get("weighted_recovery_rate")));
            parameter.put("collateral_haircut", BigDecimal.ZERO);
            parameter.put("enabled", 1);
            mapper.insertLgdSegmentParameter(parameter);
        }
        return mapper.getPublishedLgdModelVersion();
    }

    private Map<String, Object> stressParameters(String scenarioCode, Map<String, Object> body) {
        BigDecimal lgdAddition = switch (scenarioCode) {
            case "RECOVERY_DELAY" -> new BigDecimal("0.060000");
            case "INDUSTRY_RECOVERY_DOWN" -> new BigDecimal("0.100000");
            default -> new BigDecimal("0.080000");
        };
        BigDecimal recoveryHaircut = "RECOVERY_DELAY".equals(scenarioCode) ? new BigDecimal("0.180000") : new BigDecimal("0.120000");
        BigDecimal collateralHaircut = "COLLATERAL_HAIRCUT".equals(scenarioCode) ? new BigDecimal("0.300000") : new BigDecimal("0.100000");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("lgd_addition", body != null && body.containsKey("lgd_addition") ? decimal(body.get("lgd_addition")) : lgdAddition);
        parameters.put("recovery_haircut", body != null && body.containsKey("recovery_haircut") ? decimal(body.get("recovery_haircut")) : recoveryHaircut);
        parameters.put("collateral_haircut", body != null && body.containsKey("collateral_haircut") ? decimal(body.get("collateral_haircut")) : collateralHaircut);
        return parameters;
    }

    private String scenarioName(String scenarioCode) {
        return switch (scenarioCode) {
            case "RECOVERY_DELAY" -> "回收周期延长";
            case "INDUSTRY_RECOVERY_DOWN" -> "行业回收率下调";
            default -> "押品价值折减";
        };
    }

    private void normalizeFilter(Map<String, Object> filters, String key) {
        if (filters.containsKey(key)) filters.put(key, text(filters.get(key)));
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        return denominator.signum() == 0 ? BigDecimal.ZERO : numerator.divide(denominator, 6, RoundingMode.HALF_UP);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("LGD 结果序列化失败", ex);
        }
    }

    private String operator(String operator) { return text(operator).isBlank() ? "SYSTEM" : text(operator); }
    private String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private int integer(Object value) { return decimal(value).intValue(); }
    private Long asLong(Object value) { return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value)); }
    private BigDecimal decimal(Object value) { try { return value == null || String.valueOf(value).isBlank() ? BigDecimal.ZERO : value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value)); } catch (Exception ex) { return BigDecimal.ZERO; } }
}
