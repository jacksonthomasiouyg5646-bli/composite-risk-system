package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.RiskIntelligenceMapper;
import com.example.usermanagement.system.service.CompositeRiskDashboardService;
import com.example.usermanagement.system.service.CreditAiAnalysisService;
import com.example.usermanagement.system.service.CustomerRiskChatService;
import com.example.usermanagement.system.service.RiskLgdService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerRiskChatServiceImpl implements CustomerRiskChatService {
    private final CreditAiAnalysisService creditAiAnalysisService;
    private final CompositeRiskDashboardService compositeRiskDashboardService;
    private final RiskIntelligenceMapper riskIntelligenceMapper;
    private final RiskLgdService riskLgdService;

    public CustomerRiskChatServiceImpl(
            CreditAiAnalysisService creditAiAnalysisService,
            CompositeRiskDashboardService compositeRiskDashboardService,
            RiskIntelligenceMapper riskIntelligenceMapper,
            RiskLgdService riskLgdService
    ) {
        this.creditAiAnalysisService = creditAiAnalysisService;
        this.compositeRiskDashboardService = compositeRiskDashboardService;
        this.riskIntelligenceMapper = riskIntelligenceMapper;
        this.riskLgdService = riskLgdService;
    }

    @Override
    public Map<String, Object> ask(Map<String, Object> request) {
        String customer = text(request.get("customer"));
        String question = text(request.get("question"));
        boolean includeExternal = !request.containsKey("includeExternal") || Boolean.parseBoolean(String.valueOf(request.get("includeExternal")));
        if (customer.isBlank()) {
            throw new IllegalArgumentException("请输入客户编号或客户名称");
        }
        if (question.isBlank()) {
            question = "该客户当前风险如何？";
        }

        Map<String, Object> analysis = creditAiAnalysisService.analyzeCustomer(customer, includeExternal);
        String customerNo = text(map(analysis.get("customer")).get("customer_no"));
        Map<String, Object> scoring = compositeRiskDashboardService.getCustomerScoring(customerNo);
        Map<String, Object> lgd = riskLgdService.getCustomerLgd(customerNo);
        String answerType = resolveAnswerType(question);
        List<Map<String, Object>> evidence = evidence(answerType, analysis, scoring, lgd);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("assistant_model", "GROUNDED_CREDIT_RISK_CHAT_V1");
        response.put("customer", analysis.get("customer"));
        response.put("question", question);
        response.put("answer_type", answerType);
        response.put("answer", answer(answerType, analysis, scoring, lgd));
        response.put("evidence", evidence);
        response.put("recommendations", analysis.get("recommendations"));
        response.put("risk_score", scoring.get("risk_score"));
        response.put("risk_level", scoring.get("risk_level"));
        response.put("forecast_score", scoring.get("forecast_score"));
        response.put("forecast_level", scoring.get("forecast_level"));
        response.put("forecast_change", scoring.get("forecast_change"));
        response.put("external_data", analysis.get("external_data"));
        response.put("lgd", lgd.get("summary"));

        Map<String, Object> log = new LinkedHashMap<>();
        log.put("customer_no", map(analysis.get("customer")).get("customer_no"));
        log.put("question", question.substring(0, Math.min(question.length(), 500)));
        log.put("answer_type", answerType);
        log.put("external_status", map(analysis.get("external_data")).get("status"));
        riskIntelligenceMapper.insertAiChatLog(log);
        return response;
    }

    private String answer(String answerType, Map<String, Object> analysis, Map<String, Object> scoring, Map<String, Object> lgd) {
        Map<String, Object> customer = map(analysis.get("customer"));
        Map<String, Object> metrics = map(analysis.get("risk_metrics"));
        Map<String, Object> credit = map(analysis.get("credit_summary"));
        Map<String, Object> external = map(analysis.get("external_data"));
        Map<String, Object> lgdSummary = map(lgd.get("summary"));
        String name = text(customer.get("customer_name"));
        String opening = "客户 " + name + " 当前组合风险评分为 " + integer(scoring.get("risk_score")) + "，等级为 " + text(scoring.get("risk_level")) + "。";

        return switch (answerType) {
            case "LGD" -> opening + "该客户有效风险敞口 " + integer(lgdSummary.get("exposure_count")) + " 笔，EAD "
                    + amount(lgdSummary.get("ead_amount_total")) + "，EAD 加权平均 LGD 为 " + percent(lgdSummary.get("weighted_lgd_avg"))
                    + "，衰退 LGD 为 " + percent(lgdSummary.get("weighted_lgd_downturn")) + "，加权回收率为 "
                    + percent(lgdSummary.get("weighted_recovery_rate")) + "，衰退预期损失为 " + amount(lgdSummary.get("el_downturn_amount"))
                    + "。建议优先复核衰退 LGD 较高且抵押品覆盖不足的债项。";
            case "DEFAULT_OVERDUE" -> opening + "债项违约 " + integer(metrics.get("debt_default_count")) + " 笔，最大逾期 "
                    + integer(metrics.get("max_overdue_days")) + " 天，逾期记录 " + integer(metrics.get("overdue_count")) + " 笔。";
            case "CREDIT_EXPOSURE" -> opening + "当前额度总额 " + amount(credit.get("total_limit_amount")) + "，已用额度 "
                    + amount(credit.get("used_limit_amount")) + "，未结本金 " + amount(credit.get("outstanding_principal_amount_total"))
                    + "，EAD " + amount(metrics.get("ead_amount_total")) + "。";
            case "COLLATERAL" -> opening + "押品担保金额 " + amount(credit.get("total_secured_amount")) + "，未结本金 "
                    + amount(credit.get("outstanding_principal_amount_total")) + "，请结合覆盖率与押品可处置性复核。";
            case "FORECAST" -> opening + "按当前规则预测，30 天评分为 " + integer(scoring.get("forecast_score")) + "，预测等级为 "
                    + text(scoring.get("forecast_level")) + "，结论为“" + text(scoring.get("forecast_change")) + "”。";
            case "EXTERNAL" -> opening + "外部数据状态为“" + text(external.get("status_label")) + "”，数据提供方为“"
                    + text(external.get("provider_name")) + "”。";
            case "RECOMMENDATION" -> opening + "建议优先核验风险证据，并执行系统给出的处置建议与任务闭环。";
            default -> opening + "主要依据为：" + String.join("；", stringList(analysis.get("risk_reasons"))) + "。";
        };
    }

    private List<Map<String, Object>> evidence(String answerType, Map<String, Object> analysis, Map<String, Object> scoring, Map<String, Object> lgd) {
        Map<String, Object> metrics = map(analysis.get("risk_metrics"));
        Map<String, Object> credit = map(analysis.get("credit_summary"));
        Map<String, Object> external = map(analysis.get("external_data"));
        Map<String, Object> lgdSummary = map(lgd.get("summary"));
        List<Map<String, Object>> result = new ArrayList<>();
        addEvidence(result, "组合评分", integer(scoring.get("risk_score")) + " / 100", "组合评分规则");
        addEvidence(result, "30天预测", integer(scoring.get("forecast_score")) + " / 100 · " + text(scoring.get("forecast_change")), "预测规则");

        switch (answerType) {
            case "LGD" -> {
                addEvidence(result, "EAD 加权平均 LGD", percent(lgdSummary.get("weighted_lgd_avg")), "风险敞口通用信息表");
                addEvidence(result, "EAD 加权衰退 LGD", percent(lgdSummary.get("weighted_lgd_downturn")), "风险敞口通用信息表");
                addEvidence(result, "加权产品回收率", percent(lgdSummary.get("weighted_recovery_rate")), "风险敞口通用信息表");
                addEvidence(result, "衰退预期损失", amount(lgdSummary.get("el_downturn_amount")), "风险敞口通用信息表");
            }
            case "DEFAULT_OVERDUE" -> {
                addEvidence(result, "债项违约", integer(metrics.get("debt_default_count")) + " 笔", "对公债项违约信息表");
                addEvidence(result, "最大逾期", integer(metrics.get("max_overdue_days")) + " 天", "对公贷款逾期信息表");
            }
            case "CREDIT_EXPOSURE" -> {
                addEvidence(result, "未结本金", amount(credit.get("outstanding_principal_amount_total")), "对公信贷债项支用信息表");
                addEvidence(result, "风险敞口 EAD", amount(metrics.get("ead_amount_total")), "风险敞口通用信息表");
            }
            case "COLLATERAL" -> addEvidence(result, "押品担保金额", amount(credit.get("total_secured_amount")), "押品与合同关联表");
            case "EXTERNAL" -> addEvidence(result, "外部数据状态", text(external.get("status_label")), text(external.get("provider_name")));
            default -> {
                addEvidence(result, "违约概率 PD", percent(metrics.get("max_pd")), "风险敞口通用信息表");
                addEvidence(result, "评级结果", text(map(analysis.get("customer")).get("rating_level")), "对公评级信息表");
            }
        }
        return result;
    }

    private void addEvidence(List<Map<String, Object>> evidence, String label, String value, String source) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("value", value);
        item.put("source", source);
        evidence.add(item);
    }

    private String resolveAnswerType(String question) {
        String text = question.toLowerCase();
        if (text.contains("lgd") || question.contains("\u635f\u5931\u7387") || question.contains("\u56de\u6536\u7387") || question.contains("\u8870\u9000")) return "LGD";
        if (text.contains("违约") || text.contains("逾期")) return "DEFAULT_OVERDUE";
        if (text.contains("额度") || text.contains("敞口") || text.contains("合同") || text.contains("支用")) return "CREDIT_EXPOSURE";
        if (text.contains("押品") || text.contains("担保")) return "COLLATERAL";
        if (text.contains("预测") || text.contains("趋势") || text.contains("未来") || text.contains("30天")) return "FORECAST";
        if (text.contains("外部") || text.contains("大数据") || text.contains("司法") || text.contains("舆情")) return "EXTERNAL";
        if (text.contains("建议") || text.contains("措施") || text.contains("处置")) return "RECOMMENDATION";
        return "OVERVIEW";
    }

    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> result = new LinkedHashMap<>();
            source.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return Map.of();
    }

    private List<String> stringList(Object value) {
        if (value instanceof Iterable<?> items) {
            List<String> result = new ArrayList<>();
            for (Object item : items) if (item != null) result.add(String.valueOf(item));
            return result;
        }
        return List.of();
    }

    private String amount(Object value) {
        return new java.text.DecimalFormat("#,##0.00").format(decimal(value)) + " 元";
    }

    private String percent(Object value) {
        return decimal(value).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%";
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

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
