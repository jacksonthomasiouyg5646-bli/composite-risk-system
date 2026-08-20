package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.common.api.PageResult;
import com.example.usermanagement.common.service.CrudInputGuard;
import com.example.usermanagement.common.service.CrudService;
import com.example.usermanagement.system.mapper.RiskAlertSubscriptionMapper;
import com.example.usermanagement.system.service.CompositeRiskDashboardService;
import com.example.usermanagement.system.service.RiskAlertSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class RiskAlertSubscriptionServiceImpl implements RiskAlertSubscriptionService {
    private static final Logger log = LoggerFactory.getLogger(RiskAlertSubscriptionServiceImpl.class);
    private static final Set<String> CHANNELS = Set.of("SYSTEM", "EMAIL");
    private static final Set<String> TARGET_TYPES = Set.of("ALL", "USER");
    private static final Set<String> EDITABLE_FIELDS = Set.of(
            "subscription_code", "subscription_name", "channel", "target_type", "recipients", "enabled"
    );

    private final RiskAlertSubscriptionMapper mapper;
    private final CompositeRiskDashboardService compositeRiskDashboardService;
    private final CrudService notificationService;

    public RiskAlertSubscriptionServiceImpl(
            RiskAlertSubscriptionMapper mapper,
            CompositeRiskDashboardService compositeRiskDashboardService,
            @Qualifier("notificationServiceImpl") CrudService notificationService
    ) {
        this.mapper = mapper;
        this.compositeRiskDashboardService = compositeRiskDashboardService;
        this.notificationService = notificationService;
    }

    @Override
    public PageResult<Map<String, Object>> list(int page, int size, String keyword) {
        int safePage = CrudInputGuard.safePage(page);
        int safeSize = CrudInputGuard.safeSize(size);
        int offset = CrudInputGuard.safeOffset(safePage, safeSize);
        return new PageResult<>(mapper.listSubscriptions(keyword, safeSize, offset), mapper.countSubscriptions(keyword), safePage, safeSize);
    }

    @Override
    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        Map<String, Object> subscription = normalize(body, true);
        mapper.insertSubscription(subscription);
        return mapper.getSubscription(asLong(subscription.get("id")));
    }

    @Override
    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        CrudInputGuard.requirePositiveId(id);
        if (mapper.getSubscription(id) == null) {
            throw new IllegalArgumentException("预警订阅不存在");
        }
        mapper.updateSubscription(id, normalize(body, false));
        return mapper.getSubscription(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CrudInputGuard.requirePositiveId(id);
        if (mapper.getSubscription(id) == null) {
            throw new IllegalArgumentException("预警订阅不存在");
        }
        mapper.deleteSubscription(id);
    }

    @Override
    @Transactional
    public Map<String, Object> dispatch(Long id) {
        CrudInputGuard.requirePositiveId(id);
        Map<String, Object> subscription = mapper.getSubscription(id);
        if (subscription == null) {
            throw new IllegalArgumentException("预警订阅不存在");
        }
        return dispatchSubscription(subscription);
    }

    @Override
    @Scheduled(cron = "${risk.alert-subscription.daily-cron:0 30 8 * * *}", zone = "Asia/Shanghai")
    public void dispatchDailyReports() {
        for (Map<String, Object> subscription : mapper.listReadyDailySubscriptions()) {
            try {
                dispatchSubscription(subscription);
            } catch (Exception ex) {
                log.error("daily composite risk report dispatch failed, subscriptionId={}", subscription.get("id"), ex);
            }
        }
    }

    private Map<String, Object> dispatchSubscription(Map<String, Object> subscription) {
        Map<String, Object> overview = compositeRiskDashboardService.getOverview();
        Map<String, Object> summary = map(overview.get("summary"));
        Map<String, Object> notificationBody = new LinkedHashMap<>();
        notificationBody.put("title", "组合风险日报 - " + java.time.LocalDate.now());
        notificationBody.put("content", reportContent(summary));
        notificationBody.put("channel", subscription.get("channel"));
        notificationBody.put("target_type", subscription.get("target_type"));
        notificationBody.put("recipients", subscription.get("recipients"));
        notificationBody.put("status", "PUBLISHED");
        Map<String, Object> notification = notificationService.create(notificationBody);
        LocalDateTime dispatchedAt = LocalDateTime.now();
        mapper.updateLastDispatch(asLong(subscription.get("id")), dispatchedAt);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subscription", mapper.getSubscription(asLong(subscription.get("id"))));
        result.put("notification", notification);
        result.put("dispatched_at", dispatchedAt);
        return result;
    }

    private String reportContent(Map<String, Object> summary) {
        return "监测客户 " + integer(summary.get("customer_total")) + " 个；极高风险 " + integer(summary.get("extreme_risk_count"))
                + " 个；高风险 " + integer(summary.get("high_risk_count")) + " 个；预警客户 " + integer(summary.get("warning_customer_count"))
                + " 个；30 天风险上迁 " + integer(summary.get("forecast_upgrade_count")) + " 个；预测高风险 "
                + integer(summary.get("forecast_high_risk_count")) + " 个；最大行业集中度 " + value(summary.get("top_industry_name"), "-")
                + " " + percent(summary.get("top_industry_concentration")) + "。";
    }

    private Map<String, Object> normalize(Map<String, Object> body, boolean creating) {
        Map<String, Object> subscription = new LinkedHashMap<>(body == null ? Map.of() : body);
        if (creating) {
            required(subscription, "subscription_code");
            required(subscription, "subscription_name");
        }
        CrudInputGuard.sanitizeBody(subscription, EDITABLE_FIELDS);
        subscription.put("frequency", "DAILY");
        if (creating || subscription.containsKey("channel")) {
            String channel = value(subscription.get("channel"), "SYSTEM").toUpperCase();
            if (!CHANNELS.contains(channel)) throw new IllegalArgumentException("不支持的通知通道");
            subscription.put("channel", channel);
        }
        if (creating || subscription.containsKey("target_type")) {
            String targetType = value(subscription.get("target_type"), "ALL").toUpperCase();
            if (!TARGET_TYPES.contains(targetType)) throw new IllegalArgumentException("不支持的通知目标");
            subscription.put("target_type", targetType);
        }
        if (creating || subscription.containsKey("enabled")) {
            subscription.put("enabled", enabled(subscription.get("enabled")));
        }
        return subscription;
    }

    private void required(Map<String, Object> body, String key) {
        if (value(body.get(key), "").isBlank()) throw new IllegalArgumentException("缺少字段：" + key);
    }

    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> result = new LinkedHashMap<>();
            source.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return Map.of();
    }

    private String percent(Object value) {
        try {
            return new BigDecimal(String.valueOf(value)).multiply(new BigDecimal("100")).setScale(2, java.math.RoundingMode.HALF_UP) + "%";
        } catch (Exception ex) {
            return "0.00%";
        }
    }

    private int integer(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private int enabled(Object value) {
        String text = String.valueOf(value);
        return "true".equalsIgnoreCase(text) || "1".equals(text) ? 1 : 0;
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private String value(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }
}
