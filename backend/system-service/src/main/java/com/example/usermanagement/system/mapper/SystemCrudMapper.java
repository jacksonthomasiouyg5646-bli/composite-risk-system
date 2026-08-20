package com.example.usermanagement.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SystemCrudMapper {
    List<Map<String, Object>> listLoginLogs(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);
    long countLoginLogs(@Param("keyword") String keyword);
    Map<String, Object> getLoginLog(@Param("id") Long id);
    void insertLoginLog(Map<String, Object> body);
    void updateLoginLog(@Param("id") Long id, @Param("body") Map<String, Object> body);
    void deleteLoginLog(@Param("id") Long id);

    List<Map<String, Object>> listOperationLogs(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);
    long countOperationLogs(@Param("keyword") String keyword);
    Map<String, Object> getOperationLog(@Param("id") Long id);
    void insertOperationLog(Map<String, Object> body);
    void updateOperationLog(@Param("id") Long id, @Param("body") Map<String, Object> body);
    void deleteOperationLog(@Param("id") Long id);

    List<Map<String, Object>> listErrorLogs(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);
    long countErrorLogs(@Param("keyword") String keyword);
    Map<String, Object> getErrorLog(@Param("id") Long id);
    void insertErrorLog(Map<String, Object> body);
    void updateErrorLog(@Param("id") Long id, @Param("body") Map<String, Object> body);
    void deleteErrorLog(@Param("id") Long id);

    List<Map<String, Object>> listNotifications(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);
    long countNotifications(@Param("keyword") String keyword);
    Map<String, Object> getNotification(@Param("id") Long id);
    void insertNotification(Map<String, Object> body);
    void updateNotification(@Param("id") Long id, @Param("body") Map<String, Object> body);
    void deleteNotification(@Param("id") Long id);

    List<Map<String, Object>> listConfigs(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);
    long countConfigs(@Param("keyword") String keyword);
    Map<String, Object> getConfig(@Param("id") Long id);
    void insertConfig(Map<String, Object> body);
    void updateConfig(@Param("id") Long id, @Param("body") Map<String, Object> body);
    void deleteConfig(@Param("id") Long id);

    List<Map<String, Object>> listSecurityPolicies(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);
    long countSecurityPolicies(@Param("keyword") String keyword);
    Map<String, Object> getSecurityPolicy(@Param("id") Long id);
    void insertSecurityPolicy(Map<String, Object> body);
    void updateSecurityPolicy(@Param("id") Long id, @Param("body") Map<String, Object> body);
    void deleteSecurityPolicy(@Param("id") Long id);

    List<Map<String, Object>> listTenants(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);
    long countTenants(@Param("keyword") String keyword);
    Map<String, Object> getTenant(@Param("id") Long id);
    void insertTenant(Map<String, Object> body);
    void updateTenant(@Param("id") Long id, @Param("body") Map<String, Object> body);
    void deleteTenant(@Param("id") Long id);

    List<Map<String, Object>> listRiskRegisters(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);
    long countRiskRegisters(@Param("keyword") String keyword);
    Map<String, Object> getRiskRegister(@Param("id") Long id);
    void insertRiskRegister(Map<String, Object> body);
    void updateRiskRegister(@Param("id") Long id, @Param("body") Map<String, Object> body);
    void deleteRiskRegister(@Param("id") Long id);

    List<Map<String, Object>> listRiskAssessments(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);
    long countRiskAssessments(@Param("keyword") String keyword);
    Map<String, Object> getRiskAssessment(@Param("id") Long id);
    void insertRiskAssessment(Map<String, Object> body);
    void updateRiskAssessment(@Param("id") Long id, @Param("body") Map<String, Object> body);
    void deleteRiskAssessment(@Param("id") Long id);

    List<Map<String, Object>> listControlMeasures(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);
    long countControlMeasures(@Param("keyword") String keyword);
    Map<String, Object> getControlMeasure(@Param("id") Long id);
    void insertControlMeasure(Map<String, Object> body);
    void updateControlMeasure(@Param("id") Long id, @Param("body") Map<String, Object> body);
    void deleteControlMeasure(@Param("id") Long id);

    List<Map<String, Object>> listTreatmentPlans(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);
    long countTreatmentPlans(@Param("keyword") String keyword);
    Map<String, Object> getTreatmentPlan(@Param("id") Long id);
    void insertTreatmentPlan(Map<String, Object> body);
    void updateTreatmentPlan(@Param("id") Long id, @Param("body") Map<String, Object> body);
    void deleteTreatmentPlan(@Param("id") Long id);

    List<Map<String, Object>> listRiskEvents(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);
    long countRiskEvents(@Param("keyword") String keyword);
    Map<String, Object> getRiskEvent(@Param("id") Long id);
    void insertRiskEvent(Map<String, Object> body);
    void updateRiskEvent(@Param("id") Long id, @Param("body") Map<String, Object> body);
    void deleteRiskEvent(@Param("id") Long id);

    List<Map<String, Object>> listRiskIndicators(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);
    long countRiskIndicators(@Param("keyword") String keyword);
    Map<String, Object> getRiskIndicator(@Param("id") Long id);
    void insertRiskIndicator(Map<String, Object> body);
    void updateRiskIndicator(@Param("id") Long id, @Param("body") Map<String, Object> body);
    void deleteRiskIndicator(@Param("id") Long id);

    List<Map<String, Object>> listUsersForExport();
}
