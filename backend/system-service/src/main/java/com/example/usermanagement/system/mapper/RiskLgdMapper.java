package com.example.usermanagement.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RiskLgdMapper {
    Map<String, Object> getLgdOverviewSummary();

    List<Map<String, Object>> listProductDistribution();

    List<Map<String, Object>> listIndustryDistribution();

    List<Map<String, Object>> listHighLgdExposures(@Param("limit") int limit);

    List<Map<String, Object>> listLgdLedger(@Param("filters") Map<String, Object> filters, @Param("limit") int limit, @Param("offset") int offset);

    long countLgdLedger(@Param("filters") Map<String, Object> filters);

    List<Map<String, Object>> listAllLgdLedger();

    Map<String, Object> getDebtLgdDetail(@Param("debtNo") String debtNo);

    List<Map<String, Object>> listDebtCollaterals(@Param("debtNo") String debtNo);

    List<Map<String, Object>> listDebtOverdues(@Param("debtNo") String debtNo);

    List<Map<String, Object>> listDebtDefaults(@Param("debtNo") String debtNo);

    Map<String, Object> getCustomerLgdSummary(@Param("customerNo") String customerNo);

    List<Map<String, Object>> listCustomerLgdExposures(@Param("customerNo") String customerNo);

    String getLatestLgdDataDate();

    Map<String, Object> getPublishedLgdModelVersion();

    List<Map<String, Object>> listLgdModelVersions(@Param("limit") int limit);

    List<Map<String, Object>> listLgdSegmentParameters(@Param("versionId") Long versionId);

    void insertLgdModelVersion(Map<String, Object> body);

    void insertLgdSegmentParameter(Map<String, Object> body);

    void insertLgdCalculationRun(Map<String, Object> body);

    List<Map<String, Object>> listLgdCalculationRuns(@Param("limit") int limit);

    void insertLgdStressTestRun(Map<String, Object> body);

    List<Map<String, Object>> listLgdStressTestRuns(@Param("limit") int limit);
}
