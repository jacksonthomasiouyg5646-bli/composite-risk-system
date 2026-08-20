package com.example.usermanagement.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RiskPortfolioMapper {
    Map<String, Object> getPortfolioSummary();
    List<Map<String, Object>> listIndustryConcentration();
    List<Map<String, Object>> listProductConcentration();
    List<Map<String, Object>> listOrganizationConcentration();
    List<Map<String, Object>> listTopCustomerConcentration(@Param("limit") int limit);
    List<Map<String, Object>> listPortfolioLimits();
    void insertPortfolioLimit(Map<String, Object> body);
    void updatePortfolioLimit(@Param("id") Long id, @Param("body") Map<String, Object> body);
    void upsertPortfolioLimitSnapshot(Map<String, Object> body);
    List<Map<String, Object>> listLatestLimitSnapshots();
    String getLatestPortfolioDataDate();
    List<Map<String, Object>> listProductBacktestSegments();
    List<Map<String, Object>> listIndustryBacktestSegments();
    void insertBacktestRun(Map<String, Object> body);
    String getLatestBacktestRunCode();
    List<Map<String, Object>> listLatestBacktestRuns();
    List<Map<String, Object>> listClosedAlertEffectivenessBases();
    void upsertAlertEffectiveness(Map<String, Object> body);
    List<Map<String, Object>> listLatestAlertEffectiveness();
    List<Map<String, Object>> listLimitForecast();
    List<Map<String, Object>> listStressScenarios();
    Map<String, Object> findStressScenario(@Param("scenarioCode") String scenarioCode);
    List<Map<String, Object>> listStressSegments();
    void insertStressResult(Map<String, Object> body);
    List<Map<String, Object>> listLatestStressResults();
    List<Map<String, Object>> listGroupRiskOverview();
    List<Map<String, Object>> listGroupMembers(@Param("groupCode") String groupCode);
    List<Map<String, Object>> listModelLifecycle();
    Map<String, Object> getModelLifecycleSummary();
    Map<String, Object> getAlertEffectivenessMetrics();
    Map<String, Object> getWorkbenchPreference(@Param("username") String username);
    void upsertWorkbenchPreference(Map<String, Object> body);
}
