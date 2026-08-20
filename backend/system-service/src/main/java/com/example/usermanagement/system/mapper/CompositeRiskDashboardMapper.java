package com.example.usermanagement.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CompositeRiskDashboardMapper {
    List<Map<String, Object>> listRiskFeatures();

    Map<String, Object> findRiskFeatureByCustomerNo(@Param("customerNo") String customerNo);

    long countOpenTreatmentPlans();

    Map<String, Object> getSummary();

    List<Map<String, Object>> listAlerts(@Param("limit") int limit);

    List<Map<String, Object>> listWarningTrend();

    List<Map<String, Object>> listIndustryDistribution(@Param("limit") int limit);

    Map<String, Object> findAlertByCustomerNo(@Param("customerNo") String customerNo);

    Map<String, Object> findRiskRegisterByCode(@Param("riskCode") String riskCode);

    Map<String, Object> findTreatmentPlanByRiskCode(@Param("riskCode") String riskCode);

    void insertCompositeRiskRegister(@Param("body") Map<String, Object> body);

    void insertCompositeTreatmentPlan(@Param("body") Map<String, Object> body);
}
