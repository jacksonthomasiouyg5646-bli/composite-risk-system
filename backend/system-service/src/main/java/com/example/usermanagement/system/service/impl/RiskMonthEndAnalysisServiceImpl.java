package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.RiskMonthEndAnalysisMapper;
import com.example.usermanagement.system.service.RiskMonthEndAnalysisService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RiskMonthEndAnalysisServiceImpl implements RiskMonthEndAnalysisService {
    private static final Set<String> DIMENSIONS = Set.of("INDUSTRY", "PRODUCT", "ORGANIZATION");
    private final RiskMonthEndAnalysisMapper mapper;

    public RiskMonthEndAnalysisServiceImpl(RiskMonthEndAnalysisMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAnalysis(LocalDate currentMonth, LocalDate baseMonth, String dimension) {
        Map<String, Object> current = currentMonth == null
                ? mapper.findLatestPublishedBatch()
                : mapper.findPublishedBatchByMonth(monthEnd(currentMonth));
        if (current == null) throw new IllegalArgumentException("未找到当前月末批次");

        LocalDate currentDate = parseDate(current.get("month_end_date"));
        Map<String, Object> base = baseMonth == null
                ? mapper.findPreviousPublishedBatch(currentDate)
                : mapper.findPublishedBatchByMonth(monthEnd(baseMonth));
        if (base == null) base = current;

        Long currentBatchId = asLong(current.get("id"));
        Long baseBatchId = asLong(base.get("id"));
        String dimensionType = normalizeDimension(dimension);
        Map<String, Object> overview = mapper.getPortfolioComparison(currentBatchId, baseBatchId);

        List<Map<String, Object>> trend = new ArrayList<>(mapper.listMonthlyTrend(12));
        Collections.reverse(trend);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("available_months", mapper.listPublishedBatches());
        result.put("current_batch", current);
        result.put("base_batch", base);
        result.put("overview", overview);
        result.put("el_attribution", elAttribution(overview));
        result.put("monthly_trend", trend);
        result.put("dimension_type", dimensionType);
        result.put("dimension_changes", mapper.listDimensionChanges(currentBatchId, baseBatchId, dimensionType));
        result.put("risk_migration", mapper.listRiskMigration(currentBatchId, baseBatchId));
        result.put("rating_migration", mapper.listRatingMigration(currentBatchId, baseBatchId));
        result.put("change_reasons", mapper.listChangeReasonSummary(currentBatchId, baseBatchId));
        result.put("overdue_default", mapper.getOverdueDefaultComparison(currentBatchId, baseBatchId));
        result.put("change_details", mapper.listChangeDetails(currentBatchId, baseBatchId, 200));
        result.put("reconciliation", mapper.listReconciliation(currentBatchId));
        result.put("source_manifest", mapper.listSourceManifest(currentBatchId));
        result.put("quality_issues", mapper.listQualityIssues(currentBatchId));
        result.put("quality_overview", mapper.getQualityOverview(currentBatchId));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> captureMonthEnd(LocalDate monthEndDate, String operator, Map<String, Object> options) {
        LocalDate sourceDate = parseDate(mapper.getLatestSourceDataDate());
        LocalDate target = monthEndDate == null ? monthEnd(sourceDate) : monthEnd(monthEndDate);
        capture(target, sourceDate, operator(operator), "UPSTREAM_CREDIT", BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.ONE, 100, 0, 0, "MONTH_END", options == null ? Map.of() : options);
        return getAnalysis(target, null, "INDUSTRY");
    }

    @Scheduled(cron = "${risk.month-end.capture-cron:0 30 2 1 * *}", zone = "Asia/Shanghai")
    @Transactional
    public void scheduledMonthEndCapture() {
        LocalDate target = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        captureMonthEnd(target, "SYSTEM", Map.of());
    }

    private Map<String, Object> capture(LocalDate target, LocalDate sourceDate, String operator, String sourceSystem,
                                        BigDecimal eadFactor, BigDecimal pdFactor, BigDecimal lgdDelta,
                                        BigDecimal collateralFactor, int inclusionThreshold, int overdueDelta,
                                        int ratingShift, String batchType, Map<String, Object> options) {
        Map<String, Object> existing = mapper.findPublishedBatchByMonth(target);
        boolean forceNewVersion = Boolean.parseBoolean(String.valueOf(options.getOrDefault("force_new_version", false)));
        if (existing != null && !forceNewVersion) return existing;

        int version = mapper.nextVersion(target);
        Map<String, Object> batch = new LinkedHashMap<>();
        batch.put("batch_no", "ME-" + target.format(DateTimeFormatter.BASIC_ISO_DATE) + "-V" + version);
        batch.put("month_end_date", target);
        batch.put("source_data_date", sourceDate);
        batch.put("version_no", version);
        batch.put("batch_type", batchType);
        batch.put("source_system", sourceSystem);
        batch.put("source_batch_no", text(options.get("source_batch_no"), "SRC-" + sourceDate.format(DateTimeFormatter.BASIC_ISO_DATE)));
        batch.put("run_mode", text(options.get("run_mode"), forceNewVersion ? "REPROCESS" : "FORMAL"));
        batch.put("dependency_status", "READY");
        batch.put("retry_of_batch_id", forceNewVersion && existing != null ? existing.get("id") : null);
        batch.put("input_checksum", text(options.get("input_checksum"), "LOCAL-" + sourceDate.format(DateTimeFormatter.BASIC_ISO_DATE)));
        batch.put("publish_comment", text(options.get("publish_comment"), "月末组合风险正式加工"));
        batch.put("created_by", operator);
        mapper.insertBatch(batch);
        Long batchId = asLong(batch.get("id"));

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("batch_id", batchId);
        manifest.put("source_name", "CORPORATE_CREDIT_EXPOSURE");
        manifest.put("source_batch_no", batch.get("source_batch_no"));
        manifest.put("data_date", sourceDate);
        manifest.put("expected_count", mapper.getCurrentSourceStats().get("record_count"));
        manifest.put("received_count", mapper.getCurrentSourceStats().get("record_count"));
        manifest.put("checksum_value", batch.get("input_checksum"));
        manifest.put("receive_status", "VERIFIED");
        mapper.insertSourceManifest(manifest);

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("batch_id", batchId);
        parameters.put("month_end_date", target);
        parameters.put("ead_factor", eadFactor);
        parameters.put("pd_factor", pdFactor);
        parameters.put("lgd_delta", lgdDelta);
        parameters.put("collateral_factor", collateralFactor);
        parameters.put("inclusion_threshold", inclusionThreshold);
        parameters.put("overdue_delta", overdueDelta);
        parameters.put("rating_shift", ratingShift);
        int records = mapper.insertExposureSnapshots(parameters);
        if (records == 0) throw new IllegalStateException("月末加工未生成风险敞口快照");

        mapper.insertCustomerSnapshots(batchId);
        mapper.insertPortfolioSummary(batchId);
        mapper.insertIndustrySummary(batchId);
        mapper.insertProductSummary(batchId);
        mapper.insertOrganizationSummary(batchId);

        Map<String, Object> stats = mapper.getSnapshotStats(batchId);
        Map<String, Object> sourceStats = "LOCAL_DEMO_BACKFILL".equals(sourceSystem) ? stats : mapper.getCurrentSourceStats();
        int passed = 0;
        passed += reconcile(batchId, "RECORD_COUNT", "风险敞口记录数", sourceStats.get("record_count"), stats.get("record_count"), BigDecimal.ZERO);
        passed += reconcile(batchId, "CUSTOMER_COUNT", "风险客户数量", sourceStats.get("customer_count"), stats.get("customer_count"), BigDecimal.ZERO);
        passed += reconcile(batchId, "TOTAL_EAD", "组合 EAD 金额", sourceStats.get("total_ead"), stats.get("total_ead"), new BigDecimal("0.01"));
        Map<String, Object> quality = mapper.getDataQualityStats(batchId);
        passed += qualityCheck(batchId, "UNIQUE_EXPOSURE", "债项敞口唯一性", "DUPLICATE", quality.get("duplicate_count"));
        passed += qualityCheck(batchId, "RELATION_INTEGRITY", "客户合同债项关联完整性", "MISSING_RELATION", quality.get("missing_link_count"));
        passed += qualityCheck(batchId, "PARAMETER_RANGE", "PD/LGD/EAD 参数范围", "INVALID_PARAMETER", quality.get("invalid_parameter_count"));

        Map<String, Object> completion = new LinkedHashMap<>(stats);
        completion.put("id", batchId);
        completion.put("record_count", records);
        completion.put("quality_score", new BigDecimal(passed).divide(new BigDecimal("6"), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
        completion.put("reconciliation_status", passed == 6 ? "PASSED" : "FAILED");
        mapper.completeBatch(completion);

        Map<String, Object> previous = mapper.findPreviousPublishedBatch(target);
        if (previous != null) ensureChangeDetails(batchId, asLong(previous.get("id")));
        return mapper.findPublishedBatchByMonth(target);
    }

    private int qualityCheck(Long batchId, String code, String name, String type, Object invalidCount) {
        BigDecimal count = decimal(invalidCount);
        int passed = reconcile(batchId, code, name, BigDecimal.ZERO, count, BigDecimal.ZERO);
        if (passed == 0) {
            Map<String, Object> issue = new LinkedHashMap<>();
            issue.put("batch_id", batchId);
            issue.put("check_code", code);
            issue.put("issue_level", count.compareTo(new BigDecimal("10")) >= 0 ? "HIGH" : "MEDIUM");
            issue.put("issue_type", type);
            issue.put("issue_count", count.intValue());
            issue.put("issue_description", name + "发现 " + count.intValue() + " 条异常数据");
            issue.put("owner_org", "风险数据管理");
            mapper.insertQualityIssue(issue);
        }
        return passed;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getChangeDrilldown(LocalDate currentMonth, LocalDate baseMonth, String level,
                                                   String industry, String customerNo, String contractNo, String changeType) {
        Map<String, Object> current = currentMonth == null ? mapper.findLatestPublishedBatch() : mapper.findPublishedBatchByMonth(monthEnd(currentMonth));
        if (current == null) throw new IllegalArgumentException("未找到当前月末批次");
        LocalDate currentDate = parseDate(current.get("month_end_date"));
        Map<String, Object> base = baseMonth == null ? mapper.findPreviousPublishedBatch(currentDate) : mapper.findPublishedBatchByMonth(monthEnd(baseMonth));
        if (base == null) base = current;
        Long currentId = asLong(current.get("id"));
        Long baseId = asLong(base.get("id"));
        String drillLevel = Set.of("INDUSTRY", "CUSTOMER", "CONTRACT", "DEBT").contains(String.valueOf(level).toUpperCase())
                ? String.valueOf(level).toUpperCase() : "INDUSTRY";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("level", drillLevel);
        result.put("rows", mapper.listChangeDrilldown(currentId, baseId, drillLevel, industry, customerNo, contractNo, changeType, 300));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> updateQualityIssue(Long issueId, Map<String, Object> body, String operator) {
        String status = text(body.get("status"), "PROCESSING").toUpperCase();
        if (!Set.of("OPEN", "PROCESSING", "RESOLVED").contains(status)) throw new IllegalArgumentException("无效的问题状态");
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("id", issueId);
        update.put("status", status);
        update.put("resolution_note", text(body.get("resolution_note"), ""));
        update.put("owner_name", text(body.get("owner_name"), operator));
        update.put("operator", operator);
        if (mapper.updateQualityIssue(update) == 0) throw new IllegalArgumentException("质量问题不存在");
        update.put("updated", true);
        return update;
    }

    private int reconcile(Long batchId, String code, String name, Object sourceValue, Object snapshotValue, BigDecimal tolerance) {
        BigDecimal source = decimal(sourceValue);
        BigDecimal snapshot = decimal(snapshotValue);
        BigDecimal difference = source.subtract(snapshot).abs();
        boolean passed = difference.compareTo(tolerance) <= 0;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("batch_id", batchId);
        body.put("check_code", code);
        body.put("check_name", name);
        body.put("source_value", source);
        body.put("snapshot_value", snapshot);
        body.put("difference_value", source.subtract(snapshot));
        body.put("tolerance_value", tolerance);
        body.put("check_status", passed ? "PASSED" : "FAILED");
        body.put("detail_message", passed ? "勾稽通过" : "源数据与月末快照存在差异");
        mapper.insertReconciliation(body);
        return passed ? 1 : 0;
    }

    private void ensureChangeDetails(Long currentBatchId, Long baseBatchId) {
        if (currentBatchId.equals(baseBatchId) || mapper.countChangeDetails(currentBatchId, baseBatchId) > 0) return;
        mapper.deleteChangeDetails(currentBatchId, baseBatchId);
        mapper.insertCurrentChangeDetails(currentBatchId, baseBatchId);
        mapper.insertExitChangeDetails(currentBatchId, baseBatchId);
    }

    private List<Map<String, Object>> elAttribution(Map<String, Object> overview) {
        if (overview == null) return List.of();
        BigDecimal currentEad = decimal(overview.get("current_ead"));
        BigDecimal baseEad = decimal(overview.get("base_ead"));
        BigDecimal currentPd = decimal(overview.get("current_pd"));
        BigDecimal basePd = decimal(overview.get("base_pd"));
        BigDecimal currentLgd = decimal(overview.get("current_lgd_downturn"));
        BigDecimal baseLgd = decimal(overview.get("base_lgd_downturn"));
        BigDecimal actualDelta = decimal(overview.get("el_downturn_delta"));
        BigDecimal eadEffect = currentEad.subtract(baseEad).multiply(basePd).multiply(baseLgd);
        BigDecimal pdEffect = currentEad.multiply(currentPd.subtract(basePd)).multiply(baseLgd);
        BigDecimal lgdEffect = currentEad.multiply(currentPd).multiply(currentLgd.subtract(baseLgd));
        BigDecimal residual = actualDelta.subtract(eadEffect).subtract(pdEffect).subtract(lgdEffect);
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(attribution("EAD_EFFECT", "规模效应", eadEffect));
        result.add(attribution("PD_EFFECT", "PD 变化效应", pdEffect));
        result.add(attribution("LGD_EFFECT", "LGD 变化效应", lgdEffect));
        result.add(attribution("MIX_RESIDUAL", "结构与交叉效应", residual));
        return result;
    }

    private Map<String, Object> attribution(String code, String name, BigDecimal value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("effect_code", code);
        row.put("effect_name", name);
        row.put("amount", value.setScale(2, RoundingMode.HALF_UP));
        return row;
    }

    private String normalizeDimension(String dimension) {
        String value = dimension == null ? "INDUSTRY" : dimension.trim().toUpperCase();
        return DIMENSIONS.contains(value) ? value : "INDUSTRY";
    }

    private LocalDate monthEnd(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    private LocalDate parseDate(Object value) {
        if (value instanceof LocalDate date) return date;
        if (value == null || String.valueOf(value).isBlank()) return LocalDate.now();
        return LocalDate.parse(String.valueOf(value).substring(0, 10));
    }

    private BigDecimal decimal(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
        try {
            return value instanceof BigDecimal number ? number : new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private String operator(String value) {
        return value == null || value.isBlank() ? "SYSTEM" : value.trim();
    }

    private String text(Object value, String defaultValue) {
        return value == null || String.valueOf(value).isBlank() ? defaultValue : String.valueOf(value).trim();
    }
}
