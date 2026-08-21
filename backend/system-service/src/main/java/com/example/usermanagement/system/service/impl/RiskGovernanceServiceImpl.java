package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.common.api.PageResult;
import com.example.usermanagement.system.mapper.RiskGovernanceMapper;
import com.example.usermanagement.system.mapper.RiskIntelligenceMapper;
import com.example.usermanagement.system.mapper.RiskScoringRuleMapper;
import com.example.usermanagement.system.service.CompositeRiskDashboardService;
import com.example.usermanagement.system.service.CompositeRiskScoringEngine;
import com.example.usermanagement.system.service.RiskGovernanceService;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RiskGovernanceServiceImpl implements RiskGovernanceService {
    private static final Set<String> METRICS = Set.of(
            "blacklist_flag", "debt_default_count", "max_overdue_days", "overdue_count",
            "max_pd", "rating_numeric", "utilization_rate", "coverage_rate"
    );
    private static final Set<String> OPERATORS = Set.of("GT", "GTE", "LT", "LTE", "EQ");
    private static final Set<String> EFFECTS = Set.of("ADD", "FLOOR");

    private final RiskGovernanceMapper mapper;
    private final RiskIntelligenceMapper intelligenceMapper;
    private final RiskScoringRuleMapper riskScoringRuleMapper;
    private final CompositeRiskDashboardService compositeRiskDashboardService;
    private final CompositeRiskScoringEngine scoringEngine;
    private final ObjectMapper objectMapper;

    public RiskGovernanceServiceImpl(
            RiskGovernanceMapper mapper,
            RiskIntelligenceMapper intelligenceMapper,
            RiskScoringRuleMapper riskScoringRuleMapper,
            CompositeRiskDashboardService compositeRiskDashboardService,
            CompositeRiskScoringEngine scoringEngine,
            ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.intelligenceMapper = intelligenceMapper;
        this.riskScoringRuleMapper = riskScoringRuleMapper;
        this.compositeRiskDashboardService = compositeRiskDashboardService;
        this.scoringEngine = scoringEngine;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> getDataGovernanceOverview() {
        Map<String, Object> live = evaluateDataQuality();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("live", live);
        result.put("latest_snapshot", mapper.getLatestDataQualitySnapshot());
        result.put("lineage", mapper.listDataLineage());
        result.put("ingestion_summary", mapper.getIngestionSummary());
        result.put("ingestion_batches", mapper.listIngestionBatches(12));
        result.put("audit", getAuditOverview());
        return result;
    }

    @Override
    public Map<String, Object> captureDataQuality() {
        Map<String, Object> quality = evaluateDataQuality();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("snapshot_date", LocalDate.now());
        snapshot.put("quality_score", quality.get("quality_score"));
        snapshot.put("check_total", quality.get("check_total"));
        snapshot.put("pass_count", quality.get("pass_count"));
        snapshot.put("warning_count", quality.get("warning_count"));
        snapshot.put("failed_count", quality.get("failed_count"));
        snapshot.put("issue_total", quality.get("issue_total"));
        snapshot.put("detail_json", json(quality.get("checks")));
        snapshot.put("captured_at", LocalDateTime.now());
        mapper.upsertDataQualitySnapshot(snapshot);
        snapshot.put("checks", quality.get("checks"));
        return snapshot;
    }

    @Override
    @Transactional
    public Map<String, Object> simulateDataIngestion(String operator) {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> batches = List.of(
                ingestionBatch("EXT-CREDIT", "CreditProfile", "SUCCESS", 200, 196, 4, new BigDecimal("98.00"), now.minusMinutes(42), operator, "征信评分、逾期摘要、负面记录增量接入"),
                ingestionBatch("EXT-BIZ", "BusinessRegistration", "SUCCESS", 200, 199, 1, new BigDecimal("99.50"), now.minusMinutes(35), operator, "工商状态、股权关系、经营异常清单接入"),
                ingestionBatch("EXT-LAWSUIT", "JudicialCase", "WARNING", 120, 112, 8, new BigDecimal("93.33"), now.minusMinutes(28), operator, "司法涉诉和被执行信息接入，部分记录客户号待人工匹配"),
                ingestionBatch("EXT-NEWS", "PublicOpinion", "SUCCESS", 80, 78, 2, new BigDecimal("97.50"), now.minusMinutes(16), operator, "公开舆情与重大负面事件摘要接入")
        );
        batches.forEach(mapper::insertIngestionBatch);

        List<Map<String, Object>> scores = compositeRiskDashboardService.listCustomerScorings();
        int logged = 0;
        for (Map<String, Object> score : scores.stream().limit(24).toList()) {
            Map<String, Object> log = new LinkedHashMap<>();
            int riskScore = integer(score.get("risk_score"));
            log.put("customer_no", score.get("customer_no"));
            log.put("provider_name", "模拟外部风险数据平台");
            log.put("query_status", logged % 11 == 0 ? "UNAVAILABLE" : "AVAILABLE");
            log.put("data_available", logged % 11 == 0 ? 0 : 1);
            log.put("external_risk_score", Math.min(100, Math.max(0, riskScore + (logged % 5 - 2) * 3)));
            log.put("data_source", logged % 3 == 0 ? "司法涉诉" : logged % 3 == 1 ? "工商经营" : "征信摘要");
            intelligenceMapper.insertExternalDataAccessLog(log);
            logged++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch_count", batches.size());
        result.put("external_log_count", logged);
        result.put("ingestion_summary", mapper.getIngestionSummary());
        result.put("ingestion_batches", mapper.listIngestionBatches(12));
        result.put("simulated_at", now);
        return result;
    }

    @Override
    public Map<String, Object> getAuditOverview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", mapper.getPermissionAuditSummary());
        result.put("role_matrix", mapper.listRolePermissionMatrix());
        result.put("sensitive_operations", mapper.listSensitiveOperationLogs(20));
        result.put("audit_opinion", List.of(
                "高敏操作已纳入 POST/PUT/DELETE 审计日志，覆盖模型发布、预警关闭、批量处置等路径。",
                "管理员角色拥有全量权限，业务角色需按最小授权原则继续拆分为风控处置、模型治理、数据治理和审计查看。",
                "建议生产环境开启日志归档和不可篡改存储，满足事后追溯和内控检查。"
        ));
        return result;
    }

    @Override
    public Map<String, Object> getModelGovernanceOverview() {
        Map<String, Object> published = ensurePublishedBaseline();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("published_version", modelDetail(asLong(published.get("id"))));
        result.put("versions", mapper.listModelVersions(30));
        result.put("stress_test_runs", mapper.listStressTestRuns(10));
        return result;
    }

    @Override
    public Map<String, Object> getModelVersionDetail(Long versionId) {
        return modelDetail(versionId);
    }

    @Override
    @Transactional
    public Map<String, Object> createModelVersion(Map<String, Object> body, String operator) {
        Map<String, Object> baseline = ensurePublishedBaseline();
        List<Map<String, Object>> activeRules = riskScoringRuleMapper.listEnabledRules();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String code = value(body == null ? null : body.get("version_code"), "CRS-" + timestamp).toUpperCase();
        String name = value(body == null ? null : body.get("version_name"), "组合风险评分模型 " + timestamp);
        Map<String, Object> version = new LinkedHashMap<>();
        version.put("version_code", code);
        version.put("version_name", name);
        version.put("status", "DRAFT");
        version.put("baseline_version_id", asLong(baseline.get("id")));
        version.put("rule_count", activeRules.size());
        version.put("created_by", operator(operator));
        mapper.insertModelVersion(version);
        mapper.copyActiveRulesToVersion(asLong(version.get("id")));
        recordModelAction(asLong(version.get("id")), "CREATE", "DRAFT", operator, value(body == null ? null : body.get("comment"), "从当前发布版本复制规则"));
        return modelDetail(asLong(version.get("id")));
    }

    @Override
    @Transactional
    public Map<String, Object> updateModelVersionRule(Long versionId, String ruleCode, Map<String, Object> body, String operator) {
        requireStatus(versionId, "DRAFT");
        if (mapper.listModelVersionRules(versionId).stream().noneMatch(rule -> ruleCode.equals(rule.get("rule_code")))) {
            throw new IllegalArgumentException("未找到模型版本规则：" + ruleCode);
        }
        mapper.updateModelVersionRule(versionId, ruleCode, normalizeRule(body));
        recordModelAction(versionId, "EDIT_RULE", "DRAFT", operator, "调整规则 " + ruleCode);
        return modelDetail(versionId);
    }

    @Override
    @Transactional
    public Map<String, Object> simulateModelVersion(Long versionId, String operator) {
        Map<String, Object> version = requireVersion(versionId);
        if (!Set.of("DRAFT", "IN_REVIEW", "APPROVED").contains(value(version.get("status"), ""))) {
            throw new IllegalArgumentException("当前版本不能执行模拟");
        }
        List<Map<String, Object>> versionRules = mapper.listModelVersionRules(versionId);
        if (versionRules.isEmpty()) {
            throw new IllegalArgumentException("模型版本不包含可模拟的规则");
        }
        List<Map<String, Object>> currentRules = riskScoringRuleMapper.listEnabledRules();
        List<Map<String, Object>> samples = new ArrayList<>();
        int currentHigh = 0;
        int simulatedHigh = 0;
        int riskUpgrade = 0;
        int riskDowngrade = 0;
        int changed = 0;
        BigDecimal totalDelta = BigDecimal.ZERO;
        List<Map<String, Object>> features = compositeRiskDashboardService.listCustomerScorings();
        for (Map<String, Object> feature : features) {
            Map<String, Object> current = scoringEngine.score(feature, currentRules);
            Map<String, Object> simulated = scoringEngine.score(feature, versionRules);
            int currentScore = integer(current.get("risk_score"));
            int simulatedScore = integer(simulated.get("risk_score"));
            int delta = simulatedScore - currentScore;
            if (currentScore >= 65) currentHigh++;
            if (simulatedScore >= 65) simulatedHigh++;
            if (riskRank(simulatedScore) > riskRank(currentScore)) riskUpgrade++;
            if (riskRank(simulatedScore) < riskRank(currentScore)) riskDowngrade++;
            if (delta != 0) changed++;
            totalDelta = totalDelta.add(BigDecimal.valueOf(delta));
            if (delta != 0) {
                Map<String, Object> sample = new LinkedHashMap<>();
                sample.put("customer_no", feature.get("customer_no"));
                sample.put("customer_name", feature.get("customer_name"));
                sample.put("current_score", currentScore);
                sample.put("simulated_score", simulatedScore);
                sample.put("score_delta", delta);
                sample.put("current_level", current.get("risk_level"));
                sample.put("simulated_level", simulated.get("risk_level"));
                samples.add(sample);
            }
        }
        samples.sort(Comparator.comparingInt((Map<String, Object> row) -> Math.abs(integer(row.get("score_delta")))).reversed());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("customer_total", features.size());
        summary.put("changed_customer_count", changed);
        summary.put("current_high_risk_count", currentHigh);
        summary.put("simulated_high_risk_count", simulatedHigh);
        summary.put("high_risk_delta", simulatedHigh - currentHigh);
        summary.put("risk_upgrade_count", riskUpgrade);
        summary.put("risk_downgrade_count", riskDowngrade);
        summary.put("average_score_delta", features.isEmpty() ? BigDecimal.ZERO : totalDelta.divide(BigDecimal.valueOf(features.size()), 2, RoundingMode.HALF_UP));
        summary.put("impact_samples", samples.stream().limit(10).toList());
        LocalDateTime now = LocalDateTime.now();
        mapper.updateModelVersionSimulation(versionId, json(summary), now);
        recordModelAction(versionId, "SIMULATE", "COMPLETED", operator, "完成客户组合影响模拟");
        return Map.of("version", modelDetail(versionId), "simulation", summary, "simulated_at", now);
    }

    @Override
    @Transactional
    public Map<String, Object> submitModelVersion(Long versionId, String operator, String comment) {
        Map<String, Object> version = requireStatus(versionId, "DRAFT");
        if (version.get("simulation_summary") == null) {
            throw new IllegalArgumentException("提交审批前必须完成模型模拟");
        }
        LocalDateTime now = LocalDateTime.now();
        mapper.submitModelVersion(versionId, operator(operator), now);
        recordModelAction(versionId, "SUBMIT", "IN_REVIEW", operator, comment);
        return modelDetail(versionId);
    }

    @Override
    @Transactional
    public Map<String, Object> approveModelVersion(Long versionId, String operator, String comment) {
        requireStatus(versionId, "IN_REVIEW");
        LocalDateTime now = LocalDateTime.now();
        mapper.approveModelVersion(versionId, operator(operator), value(comment, "审批通过"), now);
        recordModelAction(versionId, "APPROVE", "APPROVED", operator, comment);
        return modelDetail(versionId);
    }

    @Override
    @Transactional
    public Map<String, Object> publishModelVersion(Long versionId, String operator, String comment) {
        requireStatus(versionId, "APPROVED");
        mapper.retirePublishedModelVersions(versionId);
        mapper.publishRulesFromVersion(versionId);
        LocalDateTime now = LocalDateTime.now();
        mapper.publishModelVersion(versionId, operator(operator), now);
        recordModelAction(versionId, "PUBLISH", "PUBLISHED", operator, comment);
        return modelDetail(versionId);
    }

    @Override
    @Transactional
    public Map<String, Object> rollbackModelVersion(Long versionId, String operator, String comment) {
        Map<String, Object> version = requireVersion(versionId);
        if (!"RETIRED".equals(value(version.get("status"), ""))) {
            throw new IllegalArgumentException("只能回滚到已退役的历史发布版本");
        }
        mapper.retirePublishedModelVersions(versionId);
        mapper.publishRulesFromVersion(versionId);
        LocalDateTime now = LocalDateTime.now();
        mapper.publishModelVersion(versionId, operator(operator), now);
        recordModelAction(versionId, "ROLLBACK", "PUBLISHED", operator, value(comment, "回滚到历史已发布版本"));
        return modelDetail(versionId);
    }

    @Override
    @Transactional
    public PageResult<Map<String, Object>> listAlertCases(String state, int page, int size) {
        int escalated = mapper.escalateOverdueAlertCases(LocalDateTime.now());
        if (escalated > 0) synchronizeAlertCases(mapper.listActiveAlertCases(), LocalDateTime.now());
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> items = mapper.listAlertCases(value(state, ""), safeSize, (safePage - 1) * safeSize);
        return new PageResult<>(items, mapper.countAlertCases(value(state, "")), safePage, safeSize);
    }

    @Override
    @Transactional
    public Map<String, Object> refreshAlertCases() {
        int refreshed = 0;
        int synchronizedCount = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> score : compositeRiskDashboardService.listCustomerScorings()) {
            int riskScore = integer(score.get("risk_score"));
            if (riskScore < 45) continue;
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("customer_no", score.get("customer_no"));
            body.put("risk_code", "CREDIT-" + score.get("customer_no"));
            body.put("priority", value(score.get("priority"), "P3"));
            body.put("risk_score", riskScore);
            body.put("alert_state", "OPEN");
            body.put("owner", value(score.get("relationship_manager_name"), value(score.get("owner_org_name"), "风险管理部")));
            body.put("sla_due_at", now.plusHours(slaHours(value(score.get("priority"), "P3"))));
            body.put("detected_at", now);
            mapper.upsertAlertCase(body);
            synchronizeAlertCase(mapper.getAlertCase(value(score.get("customer_no"), "")), now);
            refreshed++;
            synchronizedCount++;
        }
        int escalated = mapper.escalateOverdueAlertCases(now);
        if (escalated > 0) synchronizedCount += synchronizeAlertCases(mapper.listActiveAlertCases(), now);
        return Map.of("refreshed_case_count", refreshed, "synchronized_case_count", synchronizedCount, "escalated_case_count", escalated, "summary", mapper.getAlertCaseSummary(), "refreshed_at", now);
    }

    @Override
    @Transactional
    public Map<String, Object> startAlertCase(String customerNo, String operator) {
        Map<String, Object> before = requireAlertCase(customerNo);
        mapper.startAlertCase(customerNo, operator(operator), LocalDateTime.now());
        Map<String, Object> alertCase = mapper.getAlertCase(customerNo);
        recordAlertTimeline(customerNo, "START", value(before.get("alert_state"), ""), value(alertCase.get("alert_state"), ""), operator, "开始处置预警案件");
        synchronizeAlertCase(alertCase, LocalDateTime.now());
        return alertCaseResult(alertCase);
    }

    @Override
    @Transactional
    public Map<String, Object> closeAlertCase(String customerNo, String comment, String operator) {
        if (value(comment, "").isBlank()) throw new IllegalArgumentException("关闭案例时必须填写处置结论");
        Map<String, Object> before = requireAlertCase(customerNo);
        mapper.closeAlertCase(customerNo, value(comment, ""), LocalDateTime.now());
        Map<String, Object> alertCase = mapper.getAlertCase(customerNo);
        recordAlertTimeline(customerNo, "CLOSE_DIRECT", value(before.get("alert_state"), ""), value(alertCase.get("alert_state"), ""), operator, comment);
        synchronizeAlertCase(alertCase, LocalDateTime.now());
        return alertCaseResult(alertCase);
    }

    @Override
    @Transactional
    public Map<String, Object> submitAlertCaseReview(String customerNo, String comment, String operator) {
        if (value(comment, "").isBlank()) throw new IllegalArgumentException("提交复核时必须填写处置说明");
        return changeAlertWorkflow(customerNo, "SUBMIT_REVIEW", "PENDING_REVIEW", comment, operator);
    }

    @Override
    @Transactional
    public Map<String, Object> approveAlertCaseReview(String customerNo, String comment, String operator) {
        if (value(comment, "").isBlank()) throw new IllegalArgumentException("复核通过时必须填写复核意见");
        return changeAlertWorkflow(customerNo, "APPROVE_REVIEW", "RESOLVED", comment, operator);
    }

    @Override
    @Transactional
    public Map<String, Object> rejectAlertCaseReview(String customerNo, String comment, String operator) {
        if (value(comment, "").isBlank()) throw new IllegalArgumentException("退回复核时必须填写退回原因");
        return changeAlertWorkflow(customerNo, "REJECT_REVIEW", "REJECTED", comment, operator);
    }

    @Override
    @Transactional
    public Map<String, Object> escalateAlertCase(String customerNo, String comment, String operator) {
        Map<String, Object> before = requireAlertCase(customerNo);
        mapper.escalateAlertCase(customerNo, value(comment, "人工升级预警案件"), operator(operator), LocalDateTime.now());
        Map<String, Object> alertCase = mapper.getAlertCase(customerNo);
        recordAlertTimeline(customerNo, "ESCALATE", value(before.get("alert_state"), ""), value(alertCase.get("alert_state"), ""), operator, comment);
        synchronizeAlertCase(alertCase, LocalDateTime.now());
        return alertCaseResult(alertCase);
    }

    @Override
    public Map<String, Object> getAlertCaseTimeline(String customerNo) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("case", requireAlertCase(customerNo));
        result.put("timeline", mapper.listAlertCaseTimeline(customerNo));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> batchStartAlertCases(Map<String, Object> body, String operator) {
        List<String> customerNos = customerNos(body);
        List<Map<String, Object>> results = new ArrayList<>();
        List<Map<String, Object>> failures = new ArrayList<>();
        for (String customerNo : customerNos) {
            try {
                results.add(startAlertCase(customerNo, operator));
            } catch (RuntimeException ex) {
                failures.add(Map.of("customer_no", customerNo, "message", ex.getMessage()));
            }
        }
        return batchResult("START", results, failures);
    }

    @Override
    @Transactional
    public Map<String, Object> batchCloseAlertCases(Map<String, Object> body, String operator) {
        String comment = value(body == null ? null : body.get("comment"), "");
        if (comment.isBlank()) throw new IllegalArgumentException("批量关闭案件时必须填写统一处置结论");
        List<String> customerNos = customerNos(body);
        List<Map<String, Object>> results = new ArrayList<>();
        List<Map<String, Object>> failures = new ArrayList<>();
        for (String customerNo : customerNos) {
            try {
                results.add(closeAlertCase(customerNo, comment, operator));
            } catch (RuntimeException ex) {
                failures.add(Map.of("customer_no", customerNo, "message", ex.getMessage()));
            }
        }
        return batchResult("CLOSE", results, failures);
    }

    @Override
    @Transactional
    public Map<String, Object> runStressTest(Map<String, Object> body, String operator) {
        String scenarioCode = value(body == null ? null : body.get("scenario_code"), "INDUSTRY_DOWNTURN").toUpperCase();
        Map<String, Object> parameters = defaultStressParameters(scenarioCode, body);
        List<Map<String, Object>> rules = riskScoringRuleMapper.listEnabledRules();
        List<Map<String, Object>> impacts = new ArrayList<>();
        int currentHigh = 0;
        int stressedHigh = 0;
        BigDecimal currentScoreTotal = BigDecimal.ZERO;
        BigDecimal stressedScoreTotal = BigDecimal.ZERO;
        for (Map<String, Object> current : compositeRiskDashboardService.listCustomerScorings()) {
            Map<String, Object> stressedFeature = new LinkedHashMap<>(current);
            stressedFeature.put("max_pd", decimal(current.get("max_pd")).multiply(decimal(parameters.get("pd_multiplier"))).min(BigDecimal.ONE));
            stressedFeature.put("coverage_rate", decimal(current.get("coverage_rate")).multiply(BigDecimal.ONE.subtract(decimal(parameters.get("collateral_haircut")))).max(BigDecimal.ZERO));
            stressedFeature.put("utilization_rate", decimal(current.get("utilization_rate")).add(decimal(parameters.get("utilization_addition"))).min(BigDecimal.ONE));
            Map<String, Object> stressed = scoringEngine.score(stressedFeature, rules);
            int before = integer(current.get("risk_score"));
            int after = integer(stressed.get("risk_score"));
            if (before >= 65) currentHigh++;
            if (after >= 65) stressedHigh++;
            currentScoreTotal = currentScoreTotal.add(BigDecimal.valueOf(before));
            stressedScoreTotal = stressedScoreTotal.add(BigDecimal.valueOf(after));
            if (after != before) {
                Map<String, Object> impact = new LinkedHashMap<>();
                impact.put("customer_no", current.get("customer_no"));
                impact.put("customer_name", current.get("customer_name"));
                impact.put("industry_name", current.get("industry_name"));
                impact.put("current_score", before);
                impact.put("stressed_score", after);
                impact.put("score_delta", after - before);
                impact.put("current_level", current.get("risk_level"));
                impact.put("stressed_level", stressed.get("risk_level"));
                impacts.add(impact);
            }
        }
        impacts.sort(Comparator.comparingInt((Map<String, Object> row) -> integer(row.get("score_delta"))).reversed());
        int total = compositeRiskDashboardService.listCustomerScorings().size();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("customer_total", total);
        summary.put("current_high_risk_count", currentHigh);
        summary.put("stressed_high_risk_count", stressedHigh);
        summary.put("high_risk_delta", stressedHigh - currentHigh);
        summary.put("current_average_score", total == 0 ? BigDecimal.ZERO : currentScoreTotal.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        summary.put("stressed_average_score", total == 0 ? BigDecimal.ZERO : stressedScoreTotal.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        summary.put("impact_samples", impacts.stream().limit(12).toList());
        Map<String, Object> run = new LinkedHashMap<>();
        run.put("scenario_code", scenarioCode);
        run.put("scenario_name", stressScenarioName(scenarioCode));
        run.put("parameter_json", json(parameters));
        run.put("summary_json", json(summary));
        run.put("run_by", operator(operator));
        mapper.insertStressTestRun(run);
        return Map.of("run", run, "parameters", parameters, "summary", summary);
    }

    @Override
    public Map<String, Object> getRelationshipGraph(String customerNo) {
        String normalized = value(customerNo, "");
        if (normalized.isBlank()) throw new IllegalArgumentException("客户编号不能为空");
        Map<String, Object> customer = compositeRiskDashboardService.getCustomerScoring(normalized);
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        String customerId = "customer:" + normalized;
        nodes.add(node(customerId, value(customer.get("customer_name"), normalized), "CUSTOMER", customer));
        for (Map<String, Object> contract : mapper.listCustomerContracts(normalized)) {
            String id = "contract:" + contract.get("contract_no");
            nodes.add(node(id, value(contract.get("contract_no"), "合同"), "CONTRACT", contract));
            edges.add(edge(customerId, id, "签订合同"));
        }
        for (Map<String, Object> drawdown : mapper.listCustomerDrawdowns(normalized)) {
            String id = "drawdown:" + drawdown.get("drawdown_no");
            nodes.add(node(id, value(drawdown.get("debt_no"), value(drawdown.get("drawdown_no"), "债项")), "DRAWDOWN", drawdown));
            String contractId = "contract:" + value(drawdown.get("contract_no"), "");
            edges.add(edge(contractId.equals("contract:") ? customerId : contractId, id, "债项支用"));
        }
        for (Map<String, Object> collateral : mapper.listCustomerCollaterals(normalized)) {
            String id = "collateral:" + collateral.get("collateral_no");
            nodes.add(node(id, value(collateral.get("collateral_no"), "押品"), "COLLATERAL", collateral));
            edges.add(edge("contract:" + collateral.get("contract_no"), id, "押品担保"));
        }
        for (Map<String, Object> peer : mapper.listManagerPeers(normalized)) {
            String id = "peer:" + peer.get("customer_no");
            nodes.add(node(id, value(peer.get("customer_name"), value(peer.get("customer_no"), "关联客户")), "PEER", peer));
            edges.add(edge(customerId, id, "同客户经理"));
        }
        return Map.of("customer", customer, "nodes", nodes, "edges", edges);
    }

    private int synchronizeAlertCases(List<Map<String, Object>> alertCases, LocalDateTime now) {
        int synchronizedCount = 0;
        for (Map<String, Object> alertCase : alertCases) {
            synchronizeAlertCase(alertCase, now);
            synchronizedCount++;
        }
        return synchronizedCount;
    }

    private Map<String, Object> alertCaseResult(Map<String, Object> alertCase) {
        Map<String, Object> result = new LinkedHashMap<>(alertCase);
        result.put("summary", mapper.getAlertCaseSummary());
        return result;
    }

    private void synchronizeAlertCase(Map<String, Object> alertCase, LocalDateTime now) {
        if (alertCase == null) return;
        Long alertCaseId = asLong(alertCase.get("id"));
        String customerNo = value(alertCase.get("customer_no"), "");
        String riskCode = value(alertCase.get("risk_code"), "CREDIT-" + customerNo);
        String customerName = value(alertCase.get("customer_name"), customerNo);
        String state = value(alertCase.get("alert_state"), "OPEN");
        int riskScore = integer(alertCase.get("risk_score"));
        String owner = value(alertCase.get("owner"), value(alertCase.get("owner_org_name"), "风险管理部"));
        LocalDate dueDate = localDate(alertCase.get("sla_due_at"));

        Map<String, Object> registerBody = new LinkedHashMap<>();
        registerBody.put("risk_code", riskCode);
        registerBody.put("risk_name", "组合风险预警 - " + customerName);
        registerBody.put("category", "信用风险");
        registerBody.put("level", registerLevel(riskScore));
        registerBody.put("owner_department", value(alertCase.get("owner_org_name"), "风险管理部"));
        registerBody.put("responsible_person", owner);
        registerBody.put("status", "RESOLVED".equals(state) ? "已关闭" : "监控中");
        registerBody.put("identified_at", localDate(alertCase.get("first_detected_at")));
        registerBody.put("due_date", dueDate);
        registerBody.put("description", alertDescription(alertCase));
        Map<String, Object> riskRegister = mapper.findAlertRiskRegister(riskCode);
        if (riskRegister == null) {
            mapper.insertAlertRiskRegister(registerBody);
        } else {
            mapper.updateAlertRiskRegister(asLong(riskRegister.get("id")), registerBody);
        }

        Map<String, Object> treatment = mapper.findAlertTreatmentPlan(riskCode);
        Map<String, Object> treatmentBody = new LinkedHashMap<>();
        treatmentBody.put("plan_code", "TRT-ALERT-" + alertCaseId);
        treatmentBody.put("risk_code", riskCode);
        treatmentBody.put("action", "核验组合风险信号，复核现金流、还款来源、抵押品覆盖与存量合同，并形成处置结论。");
        treatmentBody.put("owner", owner);
        treatmentBody.put("due_date", dueDate);
        treatmentBody.put("progress", treatmentProgress(state, treatment));
        treatmentBody.put("status", treatmentStatus(state));
        if (treatment == null) {
            mapper.insertAlertTreatmentPlan(treatmentBody);
            treatment = treatmentBody;
        } else {
            mapper.updateAlertTreatmentPlan(asLong(treatment.get("id")), treatmentBody);
        }

        String eventCode = "AEV-CASE-" + alertCaseId;
        Map<String, Object> riskEvent = mapper.findAlertRiskEvent(eventCode);
        Map<String, Object> eventBody = new LinkedHashMap<>();
        eventBody.put("event_code", eventCode);
        eventBody.put("title", "组合风险预警案件 - " + customerName);
        eventBody.put("risk_code", riskCode);
        eventBody.put("severity", eventSeverity(alertCase.get("priority")));
        eventBody.put("occurred_at", localDate(alertCase.get("first_detected_at")));
        eventBody.put("loss_amount", BigDecimal.ZERO);
        eventBody.put("owner", owner);
        eventBody.put("status", eventStatus(state));
        eventBody.put("summary", alertDescription(alertCase));
        if (riskEvent == null) {
            mapper.insertAlertRiskEvent(eventBody);
            riskEvent = eventBody;
        } else {
            mapper.updateAlertRiskEvent(asLong(riskEvent.get("id")), eventBody);
        }

        String indicatorCode = "KRI-ALERT-" + customerNo;
        Map<String, Object> indicator = mapper.findAlertRiskIndicator(indicatorCode);
        Map<String, Object> indicatorBody = new LinkedHashMap<>();
        indicatorBody.put("indicator_code", indicatorCode);
        indicatorBody.put("name", "组合风险预警评分 - " + customerName);
        indicatorBody.put("threshold_value", "45 分");
        indicatorBody.put("current_value", riskScore + " 分");
        indicatorBody.put("trend", indicatorTrend(state));
        indicatorBody.put("owner", owner);
        indicatorBody.put("status", indicatorStatus(state, riskScore));
        if (indicator == null) {
            mapper.insertAlertRiskIndicator(indicatorBody);
            indicator = indicatorBody;
        } else {
            mapper.updateAlertRiskIndicator(asLong(indicator.get("id")), indicatorBody);
        }

        Map<String, Object> link = new LinkedHashMap<>();
        link.put("alert_case_id", alertCaseId);
        link.put("risk_code", riskCode);
        link.put("treatment_plan_id", asLong(treatment.get("id")));
        link.put("risk_event_id", asLong(riskEvent.get("id")));
        link.put("risk_indicator_id", asLong(indicator.get("id")));
        link.put("last_sync_state", state);
        link.put("last_sync_at", now);
        mapper.upsertAlertCaseLink(link);
    }

    private String alertDescription(Map<String, Object> alertCase) {
        String closure = value(alertCase.get("closure_comment"), "");
        String description = "组合风险预警案件状态：" + value(alertCase.get("alert_state"), "OPEN")
                + "；风险评分：" + integer(alertCase.get("risk_score"))
                + "；优先级：" + value(alertCase.get("priority"), "P3")
                + "；SLA：" + value(alertCase.get("sla_due_at"), "-");
        return closure.isBlank() ? description : description + "；处置结论：" + closure;
    }

    private int treatmentProgress(String state, Map<String, Object> existing) {
        if ("RESOLVED".equals(state)) return 100;
        if ("OPEN".equals(state)) return 0;
        return Math.max(50, existing == null ? 0 : integer(existing.get("progress")));
    }

    private String treatmentStatus(String state) {
        return switch (state) {
            case "IN_PROGRESS" -> "进行中";
            case "OVERDUE" -> "逾期";
            case "RESOLVED" -> "已完成";
            default -> "未开始";
        };
    }

    private String eventStatus(String state) {
        return "RESOLVED".equals(state) ? "已复盘" : "OPEN".equals(state) ? "登记" : "处理中";
    }

    private String indicatorStatus(String state, int riskScore) {
        if ("RESOLVED".equals(state)) return "正常";
        return "OVERDUE".equals(state) || riskScore >= 85 ? "超限" : "预警";
    }

    private String indicatorTrend(String state) {
        return "OVERDUE".equals(state) ? "上升" : "RESOLVED".equals(state) ? "下降" : "平稳";
    }

    private String eventSeverity(Object priority) {
        return switch (value(priority, "P3")) {
            case "P1" -> "重大";
            case "P2" -> "高";
            case "P4" -> "低";
            default -> "中";
        };
    }

    private String registerLevel(int riskScore) {
        if (riskScore >= 85) return "重大";
        if (riskScore >= 65) return "高";
        if (riskScore >= 45) return "中";
        return "低";
    }

    private LocalDate localDate(Object value) {
        if (value instanceof LocalDate date) return date;
        if (value instanceof LocalDateTime dateTime) return dateTime.toLocalDate();
        String text = value(value, "");
        try {
            return text.length() >= 10 ? LocalDate.parse(text.substring(0, 10)) : LocalDate.now();
        } catch (Exception ex) {
            return LocalDate.now();
        }
    }

    @Scheduled(cron = "${risk.data-governance.daily-cron:0 50 7 * * *}", zone = "Asia/Shanghai")
    public void scheduledDataQualitySnapshot() {
        captureDataQuality();
    }

    @Scheduled(cron = "${risk.alert-case.refresh-cron:0 0 * * * *}", zone = "Asia/Shanghai")
    @Transactional
    public void scheduledAlertCaseRefresh() {
        refreshAlertCases();
    }

    private Map<String, Object> evaluateDataQuality() {
        List<Map<String, Object>> checks = new ArrayList<>();
        int pass = 0;
        int warning = 0;
        int failed = 0;
        int issueTotal = 0;
        BigDecimal penalty = BigDecimal.ZERO;
        for (Map<String, Object> raw : mapper.listDataQualityChecks()) {
            Map<String, Object> check = new LinkedHashMap<>(raw);
            int issueCount = integer(check.get("issue_count"));
            int recordTotal = integer(check.get("record_total"));
            String severity = value(check.get("severity"), "MEDIUM");
            String status = "PASS";
            if (issueCount > 0) {
                status = Set.of("CRITICAL", "HIGH").contains(severity) ? "FAILED" : "WARNING";
            }
            if ("PASS".equals(status)) pass++;
            if ("WARNING".equals(status)) warning++;
            if ("FAILED".equals(status)) failed++;
            issueTotal += issueCount;
            BigDecimal ratio = recordTotal == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(issueCount).divide(BigDecimal.valueOf(recordTotal), 4, RoundingMode.HALF_UP);
            BigDecimal weight = switch (severity) {
                case "CRITICAL" -> new BigDecimal("40");
                case "HIGH" -> new BigDecimal("25");
                default -> new BigDecimal("10");
            };
            penalty = penalty.add(ratio.multiply(weight));
            check.put("status", status);
            check.put("issue_rate", ratio);
            checks.add(check);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("quality_score", BigDecimal.valueOf(100).subtract(penalty).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        result.put("check_total", checks.size());
        result.put("pass_count", pass);
        result.put("warning_count", warning);
        result.put("failed_count", failed);
        result.put("issue_total", issueTotal);
        result.put("checks", checks);
        return result;
    }

    private Map<String, Object> ingestionBatch(String sourceSystem, String sourceEntity, String status,
                                               int sourceCount, int acceptedCount, int rejectedCount,
                                               BigDecimal qualityScore, LocalDateTime startedAt,
                                               String operator, String remark) {
        Map<String, Object> batch = new LinkedHashMap<>();
        batch.put("batch_no", "ING-" + sourceSystem + "-" + startedAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        batch.put("source_system", sourceSystem);
        batch.put("source_entity", sourceEntity);
        batch.put("batch_status", status);
        batch.put("source_record_count", sourceCount);
        batch.put("accepted_record_count", acceptedCount);
        batch.put("rejected_record_count", rejectedCount);
        batch.put("quality_score", qualityScore);
        batch.put("started_at", startedAt);
        batch.put("completed_at", startedAt.plusMinutes(3));
        batch.put("operator", operator(operator));
        batch.put("remark", remark);
        return batch;
    }

    private Map<String, Object> changeAlertWorkflow(String customerNo, String action, String nextState, String comment, String operator) {
        Map<String, Object> before = requireAlertCase(customerNo);
        mapper.updateAlertCaseWorkflow(customerNo, nextState, operator(operator), value(comment, ""), LocalDateTime.now());
        Map<String, Object> alertCase = mapper.getAlertCase(customerNo);
        if (value(before.get("alert_state"), "").equals(value(alertCase.get("alert_state"), ""))) {
            throw new IllegalArgumentException("当前案件状态不允许执行该闭环动作");
        }
        recordAlertTimeline(customerNo, action, value(before.get("alert_state"), ""), value(alertCase.get("alert_state"), ""), operator, comment);
        synchronizeAlertCase(alertCase, LocalDateTime.now());
        return alertCaseResult(alertCase);
    }

    private Map<String, Object> requireAlertCase(String customerNo) {
        Map<String, Object> alertCase = mapper.getAlertCase(customerNo);
        if (alertCase == null) throw new IllegalArgumentException("未找到预警案例");
        return alertCase;
    }

    private void recordAlertTimeline(String customerNo, String action, String fromState, String toState, String operator, String comment) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("customer_no", customerNo);
        event.put("action_type", action);
        event.put("from_state", fromState);
        event.put("to_state", toState);
        event.put("operator", operator(operator));
        event.put("comment", value(comment, ""));
        event.put("created_at", LocalDateTime.now());
        mapper.insertAlertCaseTimeline(event);
    }

    private Map<String, Object> ensurePublishedBaseline() {
        Map<String, Object> published = mapper.getPublishedModelVersion();
        if (published != null) return published;
        List<Map<String, Object>> rules = riskScoringRuleMapper.listEnabledRules();
        Map<String, Object> baseline = new LinkedHashMap<>();
        baseline.put("version_code", "CRS-BASELINE-V1");
        baseline.put("version_name", "组合风险评分基线版本 V1");
        baseline.put("status", "PUBLISHED");
        baseline.put("baseline_version_id", null);
        baseline.put("rule_count", rules.size());
        baseline.put("created_by", "SYSTEM");
        mapper.insertModelVersion(baseline);
        mapper.copyActiveRulesToVersion(asLong(baseline.get("id")));
        mapper.publishModelVersion(asLong(baseline.get("id")), "SYSTEM", LocalDateTime.now());
        recordModelAction(asLong(baseline.get("id")), "BASELINE", "PUBLISHED", "SYSTEM", "从当前已启用评分规则创建基线版本");
        return mapper.getModelVersion(asLong(baseline.get("id")));
    }

    private Map<String, Object> modelDetail(Long versionId) {
        Map<String, Object> version = requireVersion(versionId);
        Map<String, Object> result = new LinkedHashMap<>(version);
        result.put("rules", mapper.listModelVersionRules(versionId));
        result.put("approvals", mapper.listModelApprovalLogs(versionId));
        return result;
    }

    private Map<String, Object> requireVersion(Long versionId) {
        Map<String, Object> version = mapper.getModelVersion(versionId);
        if (version == null) throw new IllegalArgumentException("模型版本不存在");
        return version;
    }

    private Map<String, Object> requireStatus(Long versionId, String expected) {
        Map<String, Object> version = requireVersion(versionId);
        if (!expected.equals(value(version.get("status"), ""))) {
            throw new IllegalArgumentException("当前模型版本状态不允许此操作");
        }
        return version;
    }

    private Map<String, Object> normalizeRule(Map<String, Object> body) {
        Map<String, Object> rule = new LinkedHashMap<>(body == null ? Map.of() : body);
        if (rule.containsKey("metric_key")) {
            String metric = value(rule.get("metric_key"), "").toLowerCase();
            if (!METRICS.contains(metric)) throw new IllegalArgumentException("不支持的评分指标");
            rule.put("metric_key", metric);
        }
        if (rule.containsKey("operator_type")) {
            String operator = value(rule.get("operator_type"), "").toUpperCase();
            if (!OPERATORS.contains(operator)) throw new IllegalArgumentException("不支持的比较条件");
            rule.put("operator_type", operator);
        }
        if (rule.containsKey("effect_type")) {
            String effect = value(rule.get("effect_type"), "").toUpperCase();
            if (!EFFECTS.contains(effect)) throw new IllegalArgumentException("不支持的评分动作");
            rule.put("effect_type", effect);
        }
        if (rule.containsKey("threshold_value")) rule.put("threshold_value", decimal(rule.get("threshold_value")));
        if (rule.containsKey("score_value")) rule.put("score_value", integer(rule.get("score_value")));
        if (rule.containsKey("sort_order")) rule.put("sort_order", integer(rule.get("sort_order")));
        if (rule.containsKey("enabled")) rule.put("enabled", enabled(rule.get("enabled")));
        return rule;
    }

    private Map<String, Object> defaultStressParameters(String scenarioCode, Map<String, Object> body) {
        BigDecimal pdMultiplier = "RATE_SHOCK".equals(scenarioCode) ? new BigDecimal("1.20") : new BigDecimal("1.35");
        BigDecimal collateralHaircut = "COLLATERAL_HAIRCUT".equals(scenarioCode) ? new BigDecimal("0.30") : new BigDecimal("0.20");
        BigDecimal utilizationAddition = "RATE_SHOCK".equals(scenarioCode) ? new BigDecimal("0.03") : new BigDecimal("0.05");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("pd_multiplier", body != null && body.containsKey("pd_multiplier") ? decimal(body.get("pd_multiplier")) : pdMultiplier);
        parameters.put("collateral_haircut", body != null && body.containsKey("collateral_haircut") ? decimal(body.get("collateral_haircut")) : collateralHaircut);
        parameters.put("utilization_addition", body != null && body.containsKey("utilization_addition") ? decimal(body.get("utilization_addition")) : utilizationAddition);
        return parameters;
    }

    private String stressScenarioName(String code) {
        return switch (code) {
            case "RATE_SHOCK" -> "利率上行冲击";
            case "COLLATERAL_HAIRCUT" -> "押品价值折减";
            default -> "行业下行情景";
        };
    }

    private int slaHours(String priority) {
        return switch (priority) {
            case "P1" -> 4;
            case "P2" -> 24;
            case "P3" -> 72;
            default -> 120;
        };
    }

    private int riskRank(int score) {
        if (score >= 85) return 4;
        if (score >= 65) return 3;
        if (score >= 45) return 2;
        return 1;
    }

    private Map<String, Object> node(String id, String label, String type, Map<String, Object> meta) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id);
        node.put("label", label);
        node.put("type", type);
        node.put("meta", meta);
        return node;
    }

    private Map<String, Object> edge(String source, String target, String type) {
        return Map.of("source", source, "target", target, "type", type);
    }

    @SuppressWarnings("unchecked")
    private List<String> customerNos(Map<String, Object> body) {
        Object raw = body == null ? null : body.get("customerNos");
        if (raw == null) raw = body == null ? null : body.get("customer_nos");
        if (!(raw instanceof List<?> list)) {
            throw new IllegalArgumentException("批量操作必须提供 customerNos 数组");
        }
        List<String> customerNos = list.stream()
                .map(item -> value(item, ""))
                .filter(item -> !item.isBlank())
                .distinct()
                .limit(51)
                .toList();
        if (customerNos.isEmpty()) {
            throw new IllegalArgumentException("批量操作客户不能为空");
        }
        if (customerNos.size() > 50) {
            throw new IllegalArgumentException("单次批量操作最多支持 50 个客户");
        }
        return customerNos;
    }

    private Map<String, Object> batchResult(String action, List<Map<String, Object>> results, List<Map<String, Object>> failures) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", action);
        result.put("success_count", results.size());
        result.put("failure_count", failures.size());
        result.put("results", results);
        result.put("failures", failures);
        result.put("summary", mapper.getAlertCaseSummary());
        result.put("operated_at", LocalDateTime.now());
        return result;
    }

    private void recordModelAction(Long versionId, String action, String decision, String operator, String comment) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("version_id", versionId);
        log.put("action_type", action);
        log.put("decision", decision);
        log.put("operator", operator(operator));
        log.put("comment", value(comment, ""));
        mapper.insertModelApprovalLog(log);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("风险治理结果序列化失败", ex);
        }
    }

    private int enabled(Object value) {
        String text = value(value, "");
        return "true".equalsIgnoreCase(text) || "1".equals(text) ? 1 : 0;
    }

    private int integer(Object value) {
        try {
            return decimal(value).intValue();
        } catch (Exception ex) {
            return 0;
        }
    }

    private BigDecimal decimal(Object value) {
        try {
            if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
            return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private String operator(String operator) {
        return value(operator, "SYSTEM");
    }

    private String value(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }
}
