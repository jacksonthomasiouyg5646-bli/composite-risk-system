package com.example.usermanagement.system.service;

import com.example.usermanagement.common.api.PageResult;

import java.util.Map;

public interface RiskGovernanceService {
    Map<String, Object> getDataGovernanceOverview();
    Map<String, Object> captureDataQuality();
    Map<String, Object> getModelGovernanceOverview();
    Map<String, Object> getModelVersionDetail(Long versionId);
    Map<String, Object> createModelVersion(Map<String, Object> body, String operator);
    Map<String, Object> updateModelVersionRule(Long versionId, String ruleCode, Map<String, Object> body, String operator);
    Map<String, Object> simulateModelVersion(Long versionId, String operator);
    Map<String, Object> submitModelVersion(Long versionId, String operator, String comment);
    Map<String, Object> approveModelVersion(Long versionId, String operator, String comment);
    Map<String, Object> publishModelVersion(Long versionId, String operator, String comment);
    Map<String, Object> rollbackModelVersion(Long versionId, String operator, String comment);
    PageResult<Map<String, Object>> listAlertCases(String state, int page, int size);
    Map<String, Object> refreshAlertCases();
    Map<String, Object> startAlertCase(String customerNo, String operator);
    Map<String, Object> closeAlertCase(String customerNo, String comment, String operator);
    Map<String, Object> batchStartAlertCases(Map<String, Object> body, String operator);
    Map<String, Object> batchCloseAlertCases(Map<String, Object> body, String operator);
    Map<String, Object> runStressTest(Map<String, Object> body, String operator);
    Map<String, Object> getRelationshipGraph(String customerNo);
}
