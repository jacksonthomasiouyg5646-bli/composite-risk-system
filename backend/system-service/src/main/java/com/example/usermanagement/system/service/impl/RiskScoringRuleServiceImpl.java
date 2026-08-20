package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.common.api.PageResult;
import com.example.usermanagement.common.service.CrudInputGuard;
import com.example.usermanagement.system.mapper.RiskScoringRuleMapper;
import com.example.usermanagement.system.service.RiskScoringRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class RiskScoringRuleServiceImpl implements RiskScoringRuleService {
    private static final Set<String> METRICS = Set.of(
            "blacklist_flag", "debt_default_count", "max_overdue_days", "overdue_count",
            "max_pd", "rating_numeric", "utilization_rate", "coverage_rate"
    );
    private static final Set<String> OPERATORS = Set.of("GT", "GTE", "LT", "LTE", "EQ");
    private static final Set<String> EFFECTS = Set.of("ADD", "FLOOR");
    private static final Set<String> EDITABLE_FIELDS = Set.of(
            "rule_code", "rule_name", "metric_key", "operator_type", "threshold_value",
            "score_value", "effect_type", "enabled", "sort_order", "description"
    );

    private final RiskScoringRuleMapper mapper;

    public RiskScoringRuleServiceImpl(RiskScoringRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PageResult<Map<String, Object>> list(int page, int size, String keyword) {
        int safePage = CrudInputGuard.safePage(page);
        int safeSize = CrudInputGuard.safeSize(size);
        int offset = CrudInputGuard.safeOffset(safePage, safeSize);
        return new PageResult<>(mapper.listRules(keyword, safeSize, offset), mapper.countRules(keyword), safePage, safeSize);
    }

    @Override
    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        Map<String, Object> rule = normalize(body, true);
        mapper.insertRule(rule);
        return mapper.getRule(asLong(rule.get("id")));
    }

    @Override
    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        CrudInputGuard.requirePositiveId(id);
        if (mapper.getRule(id) == null) {
            throw new IllegalArgumentException("评分规则不存在");
        }
        mapper.updateRule(id, normalize(body, false));
        return mapper.getRule(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CrudInputGuard.requirePositiveId(id);
        if (mapper.getRule(id) == null) {
            throw new IllegalArgumentException("评分规则不存在");
        }
        mapper.deleteRule(id);
    }

    private Map<String, Object> normalize(Map<String, Object> body, boolean creating) {
        Map<String, Object> rule = new LinkedHashMap<>(body == null ? Map.of() : body);
        if (creating) {
            required(rule, "rule_code");
            required(rule, "rule_name");
        }
        CrudInputGuard.sanitizeBody(rule, EDITABLE_FIELDS);
        if (rule.containsKey("metric_key")) {
            String metric = text(rule.get("metric_key")).toLowerCase();
            if (!METRICS.contains(metric)) {
                throw new IllegalArgumentException("不支持的评分指标");
            }
            rule.put("metric_key", metric);
        }
        if (rule.containsKey("operator_type")) {
            String operator = text(rule.get("operator_type")).toUpperCase();
            if (!OPERATORS.contains(operator)) {
                throw new IllegalArgumentException("不支持的比较符");
            }
            rule.put("operator_type", operator);
        }
        if (rule.containsKey("effect_type")) {
            String effect = text(rule.get("effect_type")).toUpperCase();
            if (!EFFECTS.contains(effect)) {
                throw new IllegalArgumentException("不支持的得分动作");
            }
            rule.put("effect_type", effect);
        }
        if (rule.containsKey("threshold_value")) {
            rule.put("threshold_value", decimal(rule.get("threshold_value")));
        }
        if (rule.containsKey("score_value")) {
            rule.put("score_value", integer(rule.get("score_value")));
        }
        if (rule.containsKey("sort_order")) {
            rule.put("sort_order", integer(rule.get("sort_order")));
        }
        if (rule.containsKey("enabled")) {
            rule.put("enabled", enabled(rule.get("enabled")));
        }
        return rule;
    }

    private void required(Map<String, Object> body, String key) {
        if (text(body.get(key)).isBlank()) {
            throw new IllegalArgumentException("缺少字段：" + key);
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private BigDecimal decimal(Object value) {
        try {
            return new BigDecimal(text(value));
        } catch (Exception ex) {
            throw new IllegalArgumentException("阈值必须是数字");
        }
    }

    private int integer(Object value) {
        try {
            return Integer.parseInt(text(value));
        } catch (Exception ex) {
            throw new IllegalArgumentException("评分和排序必须是整数");
        }
    }

    private int enabled(Object value) {
        String text = text(value);
        return "true".equalsIgnoreCase(text) || "1".equals(text) ? 1 : 0;
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }
}
