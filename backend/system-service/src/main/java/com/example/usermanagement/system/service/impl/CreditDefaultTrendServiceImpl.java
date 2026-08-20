package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.CreditDefaultTrendMapper;
import com.example.usermanagement.system.service.CreditDefaultTrendService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CreditDefaultTrendServiceImpl implements CreditDefaultTrendService {
    private static final int DEFAULT_DAYS = 180;
    private static final int MAX_DAYS = 366;
    private static final int RECENT_LIMIT = 12;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final CreditDefaultTrendMapper mapper;

    public CreditDefaultTrendServiceImpl(CreditDefaultTrendMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, Object> getTrend(LocalDate startDate, LocalDate endDate, String keyword, String defaultLevel) {
        String safeKeyword = normalizeKeyword(keyword);
        String safeDefaultLevel = normalizeDefaultLevel(defaultLevel);
        DateRange range = resolveRange(startDate, endDate);

        Map<String, Map<String, Object>> dailyByDate = initializeDaily(range.startDate(), range.endDate());
        mergeDebtDefaultDaily(dailyByDate, mapper.listDebtDefaultDaily(range.startDate(), range.endDate(), safeKeyword, safeDefaultLevel));
        mergeCustomerDefaultDaily(dailyByDate, mapper.listCustomerDefaultDaily(range.startDate(), range.endDate(), safeKeyword, safeDefaultLevel));
        mergeOverdueDaily(dailyByDate, mapper.listOverdueDaily(range.startDate(), range.endDate(), safeKeyword, safeDefaultLevel));

        List<Map<String, Object>> daily = new ArrayList<>(dailyByDate.values());
        List<Map<String, Object>> levelDistribution = buildLevelDistribution(
                mapper.listDefaultLevelDistribution(range.startDate(), range.endDate(), safeKeyword, safeDefaultLevel));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("filters", buildFilters(range, safeKeyword, safeDefaultLevel));
        response.put("summary", buildSummary(daily, levelDistribution));
        response.put("daily", daily);
        response.put("level_distribution", levelDistribution);
        response.put("recent_defaults", mapper.listRecentDefaults(
                range.startDate(), range.endDate(), safeKeyword, safeDefaultLevel, RECENT_LIMIT));
        return response;
    }

    private DateRange resolveRange(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedEnd = endDate;
        if (resolvedEnd == null && startDate == null) {
            String maxStatDate = mapper.findMaxStatDate();
            resolvedEnd = maxStatDate == null ? LocalDate.now() : LocalDate.parse(maxStatDate, DATE_FORMATTER);
        }
        if (resolvedEnd == null) {
            resolvedEnd = startDate.plusDays(DEFAULT_DAYS - 1L);
        }

        LocalDate resolvedStart = startDate;
        if (resolvedStart == null) {
            resolvedStart = resolvedEnd.minusDays(DEFAULT_DAYS - 1L);
        }

        if (resolvedStart.isAfter(resolvedEnd)) {
            LocalDate temp = resolvedStart;
            resolvedStart = resolvedEnd;
            resolvedEnd = temp;
        }

        if (ChronoUnit.DAYS.between(resolvedStart, resolvedEnd) >= MAX_DAYS) {
            resolvedStart = resolvedEnd.minusDays(MAX_DAYS - 1L);
        }

        return new DateRange(resolvedStart, resolvedEnd);
    }

    private Map<String, Map<String, Object>> initializeDaily(LocalDate startDate, LocalDate endDate) {
        Map<String, Map<String, Object>> dailyByDate = new LinkedHashMap<>();
        for (LocalDate current = startDate; !current.isAfter(endDate); current = current.plusDays(1)) {
            String statDate = current.format(DATE_FORMATTER);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stat_date", statDate);
            row.put("debt_default_count", 0);
            row.put("customer_default_count", 0);
            row.put("overdue_count", 0);
            row.put("max_overdue_days", 0);
            row.put("default_exposure_amount", BigDecimal.ZERO);
            row.put("overdue_amount", BigDecimal.ZERO);
            dailyByDate.put(statDate, row);
        }
        return dailyByDate;
    }

    private void mergeDebtDefaultDaily(Map<String, Map<String, Object>> dailyByDate, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            Map<String, Object> target = dailyByDate.get(String.valueOf(row.get("stat_date")));
            if (target == null) {
                continue;
            }
            target.put("debt_default_count", asInt(row.get("debt_default_count")));
            target.put("default_exposure_amount", asBigDecimal(row.get("default_exposure_amount")));
        }
    }

    private void mergeCustomerDefaultDaily(Map<String, Map<String, Object>> dailyByDate, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            Map<String, Object> target = dailyByDate.get(String.valueOf(row.get("stat_date")));
            if (target == null) {
                continue;
            }
            target.put("customer_default_count", asInt(row.get("customer_default_count")));
        }
    }

    private void mergeOverdueDaily(Map<String, Map<String, Object>> dailyByDate, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            Map<String, Object> target = dailyByDate.get(String.valueOf(row.get("stat_date")));
            if (target == null) {
                continue;
            }
            target.put("overdue_count", asInt(row.get("overdue_count")));
            target.put("max_overdue_days", asInt(row.get("max_overdue_days")));
            target.put("overdue_amount", asBigDecimal(row.get("overdue_amount")));
        }
    }

    private List<Map<String, Object>> buildLevelDistribution(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> rowByLevel = new HashMap<>();
        for (Map<String, Object> row : rows) {
            rowByLevel.put(String.valueOf(row.get("default_level")), row);
        }

        List<Map<String, Object>> distribution = new ArrayList<>();
        for (String level : List.of("A", "B", "C")) {
            Map<String, Object> source = rowByLevel.get(level);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("default_level", level);
            item.put("debt_default_count", source == null ? 0 : asInt(source.get("debt_default_count")));
            item.put("default_exposure_amount", source == null ? BigDecimal.ZERO : asBigDecimal(source.get("default_exposure_amount")));
            item.put("loss_amount", source == null ? BigDecimal.ZERO : asBigDecimal(source.get("loss_amount")));
            distribution.add(item);
        }
        return distribution;
    }

    private Map<String, Object> buildFilters(DateRange range, String keyword, String defaultLevel) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("start_date", range.startDate().format(DATE_FORMATTER));
        filters.put("end_date", range.endDate().format(DATE_FORMATTER));
        filters.put("keyword", keyword);
        filters.put("default_level", defaultLevel);
        return filters;
    }

    private Map<String, Object> buildSummary(List<Map<String, Object>> daily, List<Map<String, Object>> levelDistribution) {
        int debtDefaultTotal = 0;
        int customerDefaultTotal = 0;
        int overdueTotal = 0;
        int maxDailyDebtDefaultCount = 0;
        int maxOverdueDays = 0;
        BigDecimal defaultExposureTotal = BigDecimal.ZERO;
        BigDecimal overdueAmountTotal = BigDecimal.ZERO;

        for (Map<String, Object> row : daily) {
            int debtDefaultCount = asInt(row.get("debt_default_count"));
            int customerDefaultCount = asInt(row.get("customer_default_count"));
            int overdueCount = asInt(row.get("overdue_count"));

            debtDefaultTotal += debtDefaultCount;
            customerDefaultTotal += customerDefaultCount;
            overdueTotal += overdueCount;
            maxDailyDebtDefaultCount = Math.max(maxDailyDebtDefaultCount, debtDefaultCount);
            maxOverdueDays = Math.max(maxOverdueDays, asInt(row.get("max_overdue_days")));
            defaultExposureTotal = defaultExposureTotal.add(asBigDecimal(row.get("default_exposure_amount")));
            overdueAmountTotal = overdueAmountTotal.add(asBigDecimal(row.get("overdue_amount")));
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("debt_default_total", debtDefaultTotal);
        summary.put("customer_default_total", customerDefaultTotal);
        summary.put("overdue_total", overdueTotal);
        summary.put("max_daily_debt_default_count", maxDailyDebtDefaultCount);
        summary.put("max_overdue_days", maxOverdueDays);
        summary.put("default_exposure_total", defaultExposureTotal);
        summary.put("overdue_amount_total", overdueAmountTotal);
        summary.put("highest_default_level", highestDefaultLevel(levelDistribution));
        return summary;
    }

    private String highestDefaultLevel(List<Map<String, Object>> levelDistribution) {
        for (Map<String, Object> row : levelDistribution) {
            if (asInt(row.get("debt_default_count")) > 0) {
                return String.valueOf(row.get("default_level"));
            }
        }
        return "";
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeDefaultLevel(String defaultLevel) {
        if (defaultLevel == null) {
            return null;
        }
        String normalized = defaultLevel.trim().toUpperCase(Locale.ROOT);
        return List.of("A", "B", "C").contains(normalized) ? normalized : null;
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

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
