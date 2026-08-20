package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.client.ExternalRiskDataClient;
import com.example.usermanagement.system.config.ExternalRiskDataProperties;
import com.example.usermanagement.system.mapper.CreditAiAnalysisMapper;
import com.example.usermanagement.system.mapper.RiskIntelligenceMapper;
import com.example.usermanagement.system.service.CreditAiAnalysisService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CreditAiAnalysisServiceImpl implements CreditAiAnalysisService {
    private static final int RECENT_LIMIT = 5;
    private static final int TIMELINE_LIMIT = 20;

    private final CreditAiAnalysisMapper mapper;
    private final ExternalRiskDataClient externalRiskDataClient;
    private final ExternalRiskDataProperties externalRiskDataProperties;
    private final RiskIntelligenceMapper riskIntelligenceMapper;

    public CreditAiAnalysisServiceImpl(
            CreditAiAnalysisMapper mapper,
            ExternalRiskDataClient externalRiskDataClient,
            ExternalRiskDataProperties externalRiskDataProperties,
            RiskIntelligenceMapper riskIntelligenceMapper
    ) {
        this.mapper = mapper;
        this.externalRiskDataClient = externalRiskDataClient;
        this.externalRiskDataProperties = externalRiskDataProperties;
        this.riskIntelligenceMapper = riskIntelligenceMapper;
    }

    @Override
    public Map<String, Object> analyzeCustomer(String customer, boolean includeExternal) {
        String customerText = customer == null ? "" : customer.trim();
        if (customerText.isEmpty()) {
            throw new IllegalArgumentException("请输入客户编号或客户名称");
        }

        Map<String, Object> profile = mapper.findCustomerProfile(customerText);
        if (profile == null) {
            throw new IllegalArgumentException("未找到匹配的对公客户");
        }

        String customerNo = String.valueOf(profile.get("customer_no"));
        ExternalDataSnapshot externalData = loadExternalData(profile, includeExternal);
        recordExternalDataAccess(customerNo, externalData);
        AnalysisResult analysis = analyze(profile, externalData);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("analysis_model", externalData.available()
                ? "LOCAL_CREDIT_AI_RULE_V1 + EXTERNAL_BIG_DATA_V1"
                : "LOCAL_CREDIT_AI_RULE_V1");
        response.put("analysis_time", LocalDateTime.now());
        response.put("customer", buildCustomer(profile));
        response.put("risk_score", analysis.score());
        response.put("risk_level", analysis.level());
        response.put("risk_conclusion", buildConclusion(profile, analysis));
        response.put("risk_tags", analysis.tags());
        response.put("risk_reasons", analysis.reasons());
        response.put("recommendations", analysis.recommendations());
        response.put("credit_summary", buildCreditSummary(profile));
        response.put("risk_metrics", buildRiskMetrics(profile));
        response.put("external_data", buildExternalData(externalData, analysis));
        response.put("recent_defaults", mapper.listRecentDefaults(customerNo, RECENT_LIMIT));
        response.put("recent_overdues", mapper.listRecentOverdues(customerNo, RECENT_LIMIT));
        response.put("recent_contracts", mapper.listRecentContracts(customerNo, RECENT_LIMIT));
        response.put("collateral_summary", mapper.listCollateralSummary(customerNo, RECENT_LIMIT));
        response.put("customer_timeline", mapper.listCustomerBusinessTimeline(customerNo, TIMELINE_LIMIT));
        return response;
    }

    private AnalysisResult analyze(Map<String, Object> profile, ExternalDataSnapshot externalData) {
        int score = 15;
        List<String> tags = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        int blacklistFlag = asInt(profile.get("blacklist_flag"));
        int debtDefaultCount = asInt(profile.get("debt_default_count"));
        int overdueCount = asInt(profile.get("overdue_count"));
        int maxOverdueDays = asInt(profile.get("max_overdue_days"));
        int customerDefaultDebtCount = asInt(profile.get("customer_default_total_debt_count"));
        BigDecimal maxPd = asBigDecimal(profile.get("max_pd"));
        BigDecimal outstanding = asBigDecimal(profile.get("outstanding_principal_amount_total"));
        BigDecimal secured = asBigDecimal(profile.get("total_secured_amount"));
        BigDecimal totalLimit = asBigDecimal(profile.get("total_limit_amount"));
        BigDecimal usedLimit = asBigDecimal(profile.get("used_limit_amount"));
        String highestDefaultLevel = string(profile.get("highest_default_level"));
        String customerDefaultStatus = string(profile.get("customer_default_status"));
        Integer ratingLevel = parseRatingLevel(profile.get("rating_level"));

        if (blacklistFlag == 1) {
            score += 40;
            tags.add("黑名单客户");
            reasons.add("客户存在黑名单标志，合规与准入风险显著。");
            recommendations.add("暂停新增授信和支用，先完成黑名单原因核验与合规审批。");
        }

        if (debtDefaultCount > 0 || "ACTIVE".equals(customerDefaultStatus)) {
            score += 40;
            tags.add("已违约");
            reasons.add("客户存在 " + debtDefaultCount + " 笔债项违约，客户违约状态为 " + emptyToDash(customerDefaultStatus) + "。");
            recommendations.add("纳入重点风险名单，启动违约处置、催收跟踪和风险缓释复核。");
            if ("A".equals(highestDefaultLevel)) {
                score += 15;
                tags.add("A类违约");
                reasons.add("最高违约等级为 A，命中逾期天数大于 90 天规则。");
            } else if ("B".equals(highestDefaultLevel)) {
                score += 10;
                tags.add("B类违约");
                reasons.add("最高违约等级为 B，命中五级分类后三类规则。");
            } else if ("C".equals(highestDefaultLevel)) {
                score += 8;
                tags.add("C类违约");
                reasons.add("最高违约等级为 C，命中评级等级 21 规则。");
            }
        }

        if (maxOverdueDays > 90) {
            score += 30;
            tags.add("严重逾期");
            reasons.add("最大逾期天数为 " + maxOverdueDays + " 天，超过 90 天警戒线。");
            recommendations.add("提高检查频率，核查还款来源、现金流和担保品可处置性。");
        } else if (maxOverdueDays > 60) {
            score += 20;
            tags.add("高逾期");
            reasons.add("最大逾期天数为 " + maxOverdueDays + " 天，处于高逾期区间。");
        } else if (maxOverdueDays > 30) {
            score += 12;
            tags.add("逾期预警");
            reasons.add("最大逾期天数为 " + maxOverdueDays + " 天，已超过 30 天预警线。");
        } else if (overdueCount > 0) {
            score += 6;
            tags.add("存在逾期");
            reasons.add("客户存在 " + overdueCount + " 笔逾期记录。");
        }

        if (overdueCount > 3) {
            score += Math.min(10, overdueCount / 2);
            reasons.add("逾期笔数较多，共 " + overdueCount + " 笔，体现还款行为波动。");
        }

        if (maxPd.compareTo(new BigDecimal("0.100000")) >= 0) {
            score += 25;
            tags.add("PD高");
            reasons.add("最大违约概率 PD 为 " + ratio(maxPd) + "，处于高风险区间。");
        } else if (maxPd.compareTo(new BigDecimal("0.050000")) >= 0) {
            score += 15;
            tags.add("PD偏高");
            reasons.add("最大违约概率 PD 为 " + ratio(maxPd) + "，高于常规关注阈值。");
        } else if (maxPd.compareTo(new BigDecimal("0.020000")) >= 0) {
            score += 8;
            reasons.add("最大违约概率 PD 为 " + ratio(maxPd) + "，需要持续观察。");
        }

        if (ratingLevel != null) {
            if (ratingLevel >= 21) {
                score += 30;
                tags.add("评级21");
                reasons.add("当前评级结果为 " + ratingLevel + "，命中违约触发评级。");
            } else if (ratingLevel >= 18) {
                score += 22;
                tags.add("评级较弱");
                reasons.add("当前评级结果为 " + ratingLevel + "，信用质量偏弱。");
            } else if (ratingLevel >= 15) {
                score += 15;
                reasons.add("当前评级结果为 " + ratingLevel + "，处于关注区间。");
            } else if (ratingLevel >= 10) {
                score += 8;
            }
        }

        if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal coverage = secured.divide(outstanding, 4, RoundingMode.HALF_UP);
            if (coverage.compareTo(new BigDecimal("0.50")) < 0) {
                score += 10;
                tags.add("担保覆盖不足");
                reasons.add("担保覆盖率约为 " + ratio(coverage) + "，低于 50%。");
                recommendations.add("补充押品、保证或其它风险缓释措施，重新评估可回收性。");
            } else if (coverage.compareTo(BigDecimal.ONE) < 0) {
                score += 5;
                reasons.add("担保覆盖率约为 " + ratio(coverage) + "，未完全覆盖未结本金。");
            }
        }

        if (totalLimit.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal utilization = usedLimit.divide(totalLimit, 4, RoundingMode.HALF_UP);
            if (utilization.compareTo(new BigDecimal("0.90")) >= 0) {
                score += 8;
                tags.add("额度高占用");
                reasons.add("额度使用率约为 " + ratio(utilization) + "，接近或达到满额使用。");
            } else if (utilization.compareTo(new BigDecimal("0.75")) >= 0) {
                score += 5;
            }
        }

        if (customerDefaultDebtCount > debtDefaultCount) {
            reasons.add("客户违约汇总债项数为 " + customerDefaultDebtCount + "，需核对债项违约明细和汇总口径。");
        }

        int externalScoreAdjustment = addExternalRiskFactors(externalData, tags, reasons, recommendations);
        score += externalScoreAdjustment;

        score = Math.min(score, 100);
        if (debtDefaultCount > 0 && score < 70) {
            score = 70;
        }
        if ("A".equals(highestDefaultLevel) && score < 85) {
            score = 85;
        }

        String level = riskLevel(score);
        addDefaultRecommendations(level, debtDefaultCount, overdueCount, recommendations);
        if (reasons.isEmpty()) {
            reasons.add("客户当前未命中违约、严重逾期、高 PD 或黑名单等主要风险规则。");
        }
        if (tags.isEmpty()) {
            tags.add("常规关注");
        }

        return new AnalysisResult(score, level, tags, reasons, distinct(recommendations), externalScoreAdjustment);
    }

    private Map<String, Object> buildCustomer(Map<String, Object> profile) {
        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("customer_no", profile.get("customer_no"));
        customer.put("customer_name", profile.get("customer_name"));
        customer.put("industry_name", profile.get("industry_name"));
        customer.put("customer_status", profile.get("customer_status"));
        customer.put("owner_org_name", profile.get("owner_org_name"));
        customer.put("relationship_manager_name", profile.get("relationship_manager_name"));
        customer.put("rating_level", profile.get("rating_level"));
        customer.put("rating_date", profile.get("rating_date"));
        return customer;
    }

    private Map<String, Object> buildCreditSummary(Map<String, Object> profile) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("limit_no", profile.get("limit_no"));
        summary.put("total_limit_amount", profile.get("total_limit_amount"));
        summary.put("used_limit_amount", profile.get("used_limit_amount"));
        summary.put("available_limit_amount", profile.get("available_limit_amount"));
        summary.put("application_count", profile.get("application_count"));
        summary.put("contract_count", profile.get("contract_count"));
        summary.put("drawdown_count", profile.get("drawdown_count"));
        summary.put("outstanding_principal_amount_total", profile.get("outstanding_principal_amount_total"));
        summary.put("collateral_count", profile.get("collateral_count"));
        summary.put("total_secured_amount", profile.get("total_secured_amount"));
        return summary;
    }

    private Map<String, Object> buildRiskMetrics(Map<String, Object> profile) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("ead_amount_total", profile.get("ead_amount_total"));
        metrics.put("el_avg_total", profile.get("el_avg_total"));
        metrics.put("max_pd", profile.get("max_pd"));
        metrics.put("max_lgd_avg", profile.get("max_lgd_avg"));
        metrics.put("max_lgd_downturn", profile.get("max_lgd_downturn"));
        metrics.put("overdue_count", profile.get("overdue_count"));
        metrics.put("max_overdue_days", profile.get("max_overdue_days"));
        metrics.put("overdue_amount_total", profile.get("overdue_amount_total"));
        metrics.put("debt_default_count", profile.get("debt_default_count"));
        metrics.put("highest_default_level", profile.get("highest_default_level"));
        metrics.put("default_exposure_amount_total", profile.get("default_exposure_amount_total"));
        metrics.put("customer_default_status", profile.get("customer_default_status"));
        return metrics;
    }

    private ExternalDataSnapshot loadExternalData(Map<String, Object> profile, boolean includeExternal) {
        String providerName = externalRiskDataProperties.getProviderName();
        if (!includeExternal) {
            return ExternalDataSnapshot.localOnly(providerName);
        }
        if (!externalRiskDataProperties.isEnabled()) {
            return ExternalDataSnapshot.notEnabled(providerName);
        }

        Map<String, String> request = new LinkedHashMap<>();
        request.put("customerNo", string(profile.get("customer_no")));
        request.put("customerName", string(profile.get("customer_name")));
        request.put("scene", "CREDIT_RISK_ANALYSIS");

        try {
            Map<String, Object> raw = externalRiskDataClient.queryCustomerRisk(
                    request,
                    externalRiskDataProperties.getAuthorization(),
                    externalRiskDataProperties.getApiKey()
            );
            return normalizeExternalData(raw, providerName);
        } catch (Exception ex) {
            return ExternalDataSnapshot.unavailable(providerName);
        }
    }

    private ExternalDataSnapshot normalizeExternalData(Map<String, Object> raw, String configuredProviderName) {
        if (raw == null || "UNAVAILABLE".equalsIgnoreCase(string(raw.get("_external_status")))) {
            return ExternalDataSnapshot.unavailable(configuredProviderName);
        }

        Map<String, Object> data = asMap(findValue(raw, "data", "result", "payload"));
        if (data == null) {
            data = raw;
        }

        String providerName = string(findValue(data, "providerName", "provider_name"));
        if (providerName.isBlank()) {
            providerName = configuredProviderName;
        }
        String dataSource = string(findValue(data, "dataSource", "data_source", "source"));
        Integer externalRiskScore = optionalInt(findValue(data, "riskScore", "risk_score", "score"));
        Integer creditScore = optionalInt(findValue(data, "creditScore", "credit_score"));

        return new ExternalDataSnapshot(
                true,
                true,
                true,
                "AVAILABLE",
                "已融合外部数据",
                providerName,
                dataSource,
                externalRiskScore,
                string(findValue(data, "riskLevel", "risk_level")),
                creditScore,
                asBoolean(findValue(data, "dishonestPersonFlag", "dishonest_person_flag", "discreditedFlag")),
                asBoolean(findValue(data, "sanctionsFlag", "sanctions_flag", "watchlistFlag")),
                asBoolean(findValue(data, "taxArrearsFlag", "tax_arrears_flag")),
                optionalInt(findValue(data, "courtCaseCount", "court_case_count", "judicialCaseCount"), 0),
                optionalInt(findValue(data, "enforcementCount", "enforcement_count", "executionCount"), 0),
                optionalInt(findValue(data, "negativeNewsCount", "negative_news_count", "negativeMediaCount"), 0),
                stringList(findValue(data, "riskTags", "risk_tags", "tags")),
                stringList(findValue(data, "riskSignals", "risk_signals", "alerts", "signals")),
                string(findValue(data, "updatedAt", "updated_at", "dataDate", "data_date"))
        );
    }

    private Map<String, Object> buildExternalData(ExternalDataSnapshot externalData, AnalysisResult analysis) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requested", externalData.requested());
        data.put("enabled", externalData.enabled());
        data.put("available", externalData.available());
        data.put("status", externalData.status());
        data.put("status_label", externalData.statusLabel());
        data.put("provider_name", externalData.providerName());
        data.put("data_source", externalData.dataSource());
        data.put("risk_score", externalData.riskScore());
        data.put("risk_level", externalData.riskLevel());
        data.put("credit_score", externalData.creditScore());
        data.put("dishonest_person_flag", externalData.dishonestPersonFlag());
        data.put("sanctions_flag", externalData.sanctionsFlag());
        data.put("tax_arrears_flag", externalData.taxArrearsFlag());
        data.put("court_case_count", externalData.courtCaseCount());
        data.put("enforcement_count", externalData.enforcementCount());
        data.put("negative_news_count", externalData.negativeNewsCount());
        data.put("risk_tags", externalData.riskTags());
        data.put("risk_signals", externalData.riskSignals());
        data.put("updated_at", externalData.updatedAt());
        data.put("score_adjustment", analysis.externalScoreAdjustment());
        return data;
    }

    private void recordExternalDataAccess(String customerNo, ExternalDataSnapshot externalData) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("customer_no", customerNo);
        log.put("provider_name", externalData.providerName());
        log.put("query_status", externalData.status());
        log.put("data_available", externalData.available() ? 1 : 0);
        log.put("external_risk_score", externalData.riskScore());
        log.put("data_source", externalData.dataSource());
        riskIntelligenceMapper.insertExternalDataAccessLog(log);
    }

    private int addExternalRiskFactors(
            ExternalDataSnapshot externalData,
            List<String> tags,
            List<String> reasons,
            List<String> recommendations
    ) {
        if (!externalData.available()) {
            return 0;
        }

        int adjustment = 0;
        Integer externalRiskScore = externalData.riskScore();
        if (externalRiskScore != null) {
            if (externalRiskScore >= 80) {
                adjustment += 25;
                tags.add("外部高风险评分");
                reasons.add("外部大数据风险评分为 " + externalRiskScore + "，处于高风险区间。");
            } else if (externalRiskScore >= 65) {
                adjustment += 16;
                tags.add("外部风险偏高");
                reasons.add("外部大数据风险评分为 " + externalRiskScore + "，建议关注跨域风险信号。");
            } else if (externalRiskScore >= 50) {
                adjustment += 8;
                reasons.add("外部大数据风险评分为 " + externalRiskScore + "，建议纳入持续监测。");
            }
        }

        if (externalData.dishonestPersonFlag()) {
            adjustment += 30;
            tags.add("外部失信信号");
            reasons.add("外部大数据返回失信被执行人或类似高风险信号。");
        }
        if (externalData.sanctionsFlag()) {
            adjustment += 35;
            tags.add("外部高风险名单");
            reasons.add("外部大数据返回制裁、观察名单或合规高风险信号。");
        }
        if (externalData.taxArrearsFlag()) {
            adjustment += 10;
            tags.add("外部税务异常");
            reasons.add("外部大数据提示存在税款欠缴或类似税务异常信号。");
        }
        if (externalData.enforcementCount() > 0) {
            adjustment += Math.min(15, 5 + externalData.enforcementCount() * 2);
            tags.add("司法执行信号");
            reasons.add("外部大数据返回 " + externalData.enforcementCount() + " 条司法执行相关记录。");
        }
        if (externalData.courtCaseCount() > 0) {
            adjustment += Math.min(8, 2 + externalData.courtCaseCount());
            reasons.add("外部大数据返回 " + externalData.courtCaseCount() + " 条涉诉或司法案件记录。");
        }
        if (externalData.negativeNewsCount() > 0) {
            adjustment += Math.min(8, externalData.negativeNewsCount() * 2);
            reasons.add("外部大数据返回 " + externalData.negativeNewsCount() + " 条负面舆情或经营异常信号。");
        }

        for (String tag : externalData.riskTags()) {
            tags.add("外部：" + tag);
        }
        if (!externalData.riskSignals().isEmpty()) {
            List<String> topSignals = externalData.riskSignals().subList(0, Math.min(3, externalData.riskSignals().size()));
            reasons.add("外部已识别 " + externalData.riskSignals().size() + " 条风险信号：" + String.join("、", topSignals) + "。");
        }
        if (adjustment > 0) {
            recommendations.add("对外部风险信号进行人工核验，将已确认结果纳入授信审批和贷后监测口径。");
        }
        return adjustment;
    }

    private String buildConclusion(Map<String, Object> profile, AnalysisResult analysis) {
        return "客户 " + profile.get("customer_name") + " 当前评估为" + analysis.level()
                + "，风险评分 " + analysis.score() + "/100。建议按系统给出的风险原因和处置建议进行跟踪。";
    }

    private void addDefaultRecommendations(String level, int debtDefaultCount, int overdueCount, List<String> recommendations) {
        if ("极高风险".equals(level) || "高风险".equals(level)) {
            recommendations.add("冻结新增授信审批，存量业务转入重点监控。");
            recommendations.add("按周更新还款、押品和催收进展，必要时启动风险化解预案。");
        } else if ("中风险".equals(level)) {
            recommendations.add("维持授信审慎准入，新增业务需补充现金流和担保复核。");
        } else {
            recommendations.add("保持常规贷后监测，关注评级、逾期和额度使用率变化。");
        }
        if (debtDefaultCount == 0 && overdueCount == 0) {
            recommendations.add("未发现当前逾期或违约，可按正常客户执行周期性复评。");
        }
    }

    private String riskLevel(int score) {
        if (score >= 85) {
            return "极高风险";
        }
        if (score >= 70) {
            return "高风险";
        }
        if (score >= 45) {
            return "中风险";
        }
        return "低风险";
    }

    private List<String> distinct(List<String> values) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (!result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private int asInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Integer optionalInt(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            if (value instanceof Number number) {
                return number.intValue();
            }
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int optionalInt(Object value, int fallback) {
        Integer result = optionalInt(value);
        return result == null ? fallback : result;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean asBoolean(Object value) {
        String text = string(value).trim();
        return "1".equals(text)
                || "true".equalsIgnoreCase(text)
                || "yes".equalsIgnoreCase(text)
                || "y".equalsIgnoreCase(text);
    }

    private Object findValue(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            if (values.containsKey(key)) {
                return values.get(key);
            }
        }
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            for (String key : keys) {
                if (key.equalsIgnoreCase(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private List<String> stringList(Object value) {
        List<String> values = new ArrayList<>();
        if (value instanceof Iterable<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> object) {
                    Map<String, Object> map = asMap(object);
                    String text = string(findValue(map, "name", "label", "message", "title", "type"));
                    if (!text.isBlank()) {
                        values.add(text.trim());
                    }
                } else {
                    String text = string(item).trim();
                    if (!text.isBlank()) {
                        values.add(text);
                    }
                }
            }
        } else {
            String text = string(value).trim();
            if (!text.isBlank()) {
                for (String item : text.split("[,;|]")) {
                    if (!item.isBlank()) {
                        values.add(item.trim());
                    }
                }
            }
        }
        return distinct(values);
    }

    private String emptyToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private Integer parseRatingLevel(Object value) {
        String rating = string(value);
        if (rating.isBlank()) {
            return null;
        }
        String digits = rating.replaceAll("\\D+", "");
        if (digits.isBlank()) {
            return null;
        }
        return Integer.parseInt(digits);
    }

    private String ratio(BigDecimal value) {
        return value.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%";
    }

    private record ExternalDataSnapshot(
            boolean requested,
            boolean enabled,
            boolean available,
            String status,
            String statusLabel,
            String providerName,
            String dataSource,
            Integer riskScore,
            String riskLevel,
            Integer creditScore,
            boolean dishonestPersonFlag,
            boolean sanctionsFlag,
            boolean taxArrearsFlag,
            int courtCaseCount,
            int enforcementCount,
            int negativeNewsCount,
            List<String> riskTags,
            List<String> riskSignals,
            String updatedAt
    ) {
        private static ExternalDataSnapshot localOnly(String providerName) {
            return new ExternalDataSnapshot(
                    false, false, false, "LOCAL_ONLY", "未选择外部大数据", providerName, "", null, "", null,
                    false, false, false, 0, 0, 0, List.of(), List.of(), ""
            );
        }

        private static ExternalDataSnapshot notEnabled(String providerName) {
            return new ExternalDataSnapshot(
                    true, false, false, "NOT_ENABLED", "外部大数据未配置", providerName, "", null, "", null,
                    false, false, false, 0, 0, 0, List.of(), List.of(), ""
            );
        }

        private static ExternalDataSnapshot unavailable(String providerName) {
            return new ExternalDataSnapshot(
                    true, true, false, "UNAVAILABLE", "外部数据不可用，已降级为本地分析", providerName, "", null, "", null,
                    false, false, false, 0, 0, 0, List.of(), List.of(), ""
            );
        }
    }

    private record AnalysisResult(
            int score,
            String level,
            List<String> tags,
            List<String> reasons,
            List<String> recommendations,
            int externalScoreAdjustment
    ) {
    }
}
