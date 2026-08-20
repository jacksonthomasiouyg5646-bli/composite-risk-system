package com.example.usermanagement.system.service;

import java.util.Map;

public interface RiskPortfolioManagementService {
    Map<String, Object> getOverview(String username);
    Map<String, Object> captureLimitSnapshot(String operator);
    Map<String, Object> updateLimit(Long id, Map<String, Object> body, String operator);
    Map<String, Object> runBacktest(String operator);
    Map<String, Object> runAlertEffectiveness(String operator);
    Map<String, Object> runStressTest(String scenarioCode, String operator);
    Map<String, Object> getGroupMembers(String groupCode);
    Map<String, Object> saveWorkbenchPreference(Map<String, Object> body, String username);
}
