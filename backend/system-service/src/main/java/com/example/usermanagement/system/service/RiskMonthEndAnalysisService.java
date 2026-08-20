package com.example.usermanagement.system.service;

import java.time.LocalDate;
import java.util.Map;

public interface RiskMonthEndAnalysisService {
    Map<String, Object> getAnalysis(LocalDate currentMonth, LocalDate baseMonth, String dimension);
    Map<String, Object> captureMonthEnd(LocalDate monthEndDate, String operator, Map<String, Object> options);
    Map<String, Object> getChangeDrilldown(LocalDate currentMonth, LocalDate baseMonth, String level,
                                           String industry, String customerNo, String contractNo, String changeType);
    Map<String, Object> updateQualityIssue(Long issueId, Map<String, Object> body, String operator);
}
