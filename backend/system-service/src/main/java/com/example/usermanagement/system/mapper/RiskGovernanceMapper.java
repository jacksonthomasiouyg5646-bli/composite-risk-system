package com.example.usermanagement.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface RiskGovernanceMapper {
    List<Map<String, Object>> listDataQualityChecks();

    List<Map<String, Object>> listDataLineage();

    Map<String, Object> getLatestDataQualitySnapshot();

    void upsertDataQualitySnapshot(Map<String, Object> body);

    List<Map<String, Object>> listModelVersions(@Param("limit") int limit);

    Map<String, Object> getModelVersion(@Param("id") Long id);

    Map<String, Object> getPublishedModelVersion();

    List<Map<String, Object>> listModelVersionRules(@Param("versionId") Long versionId);

    List<Map<String, Object>> listModelApprovalLogs(@Param("versionId") Long versionId);

    void insertModelVersion(Map<String, Object> body);

    void copyActiveRulesToVersion(@Param("versionId") Long versionId);

    void updateModelVersionRule(@Param("versionId") Long versionId, @Param("ruleCode") String ruleCode, @Param("body") Map<String, Object> body);

    void updateModelVersionSimulation(@Param("versionId") Long versionId, @Param("summaryJson") String summaryJson, @Param("simulatedAt") LocalDateTime simulatedAt);

    void submitModelVersion(@Param("versionId") Long versionId, @Param("operator") String operator, @Param("submittedAt") LocalDateTime submittedAt);

    void approveModelVersion(@Param("versionId") Long versionId, @Param("operator") String operator, @Param("comment") String comment, @Param("approvedAt") LocalDateTime approvedAt);

    void retirePublishedModelVersions(@Param("versionId") Long versionId);

    void publishRulesFromVersion(@Param("versionId") Long versionId);

    void publishModelVersion(@Param("versionId") Long versionId, @Param("operator") String operator, @Param("publishedAt") LocalDateTime publishedAt);

    void insertModelApprovalLog(Map<String, Object> body);

    List<Map<String, Object>> listAlertCases(@Param("state") String state, @Param("limit") int limit, @Param("offset") int offset);

    long countAlertCases(@Param("state") String state);

    Map<String, Object> getAlertCase(@Param("customerNo") String customerNo);

    Map<String, Object> getAlertCaseSummary();

    void upsertAlertCase(Map<String, Object> body);

    void startAlertCase(@Param("customerNo") String customerNo, @Param("operator") String operator, @Param("startedAt") LocalDateTime startedAt);

    void closeAlertCase(@Param("customerNo") String customerNo, @Param("comment") String comment, @Param("closedAt") LocalDateTime closedAt);

    int escalateOverdueAlertCases(@Param("now") LocalDateTime now);

    List<Map<String, Object>> listActiveAlertCases();

    Map<String, Object> getAlertCaseLink(@Param("alertCaseId") Long alertCaseId);

    Map<String, Object> findAlertRiskRegister(@Param("riskCode") String riskCode);

    void insertAlertRiskRegister(Map<String, Object> body);

    void updateAlertRiskRegister(@Param("id") Long id, @Param("body") Map<String, Object> body);

    Map<String, Object> findAlertTreatmentPlan(@Param("riskCode") String riskCode);

    void insertAlertTreatmentPlan(Map<String, Object> body);

    void updateAlertTreatmentPlan(@Param("id") Long id, @Param("body") Map<String, Object> body);

    Map<String, Object> findAlertRiskEvent(@Param("eventCode") String eventCode);

    void insertAlertRiskEvent(Map<String, Object> body);

    void updateAlertRiskEvent(@Param("id") Long id, @Param("body") Map<String, Object> body);

    Map<String, Object> findAlertRiskIndicator(@Param("indicatorCode") String indicatorCode);

    void insertAlertRiskIndicator(Map<String, Object> body);

    void updateAlertRiskIndicator(@Param("id") Long id, @Param("body") Map<String, Object> body);

    void upsertAlertCaseLink(Map<String, Object> body);

    void insertStressTestRun(Map<String, Object> body);

    List<Map<String, Object>> listStressTestRuns(@Param("limit") int limit);

    List<Map<String, Object>> listCustomerContracts(@Param("customerNo") String customerNo);

    List<Map<String, Object>> listCustomerDrawdowns(@Param("customerNo") String customerNo);

    List<Map<String, Object>> listCustomerCollaterals(@Param("customerNo") String customerNo);

    List<Map<String, Object>> listManagerPeers(@Param("customerNo") String customerNo);
}
