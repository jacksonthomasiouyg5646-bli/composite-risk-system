package com.example.usermanagement.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface RiskMonthEndAnalysisMapper {
    int countPublishedBatches();
    String getLatestSourceDataDate();
    int nextVersion(@Param("monthEndDate") LocalDate monthEndDate);
    void insertBatch(Map<String, Object> body);
    void insertSourceManifest(Map<String, Object> body);
    int insertExposureSnapshots(Map<String, Object> body);
    int insertCustomerSnapshots(@Param("batchId") Long batchId);
    int insertPortfolioSummary(@Param("batchId") Long batchId);
    int insertIndustrySummary(@Param("batchId") Long batchId);
    int insertProductSummary(@Param("batchId") Long batchId);
    int insertOrganizationSummary(@Param("batchId") Long batchId);
    Map<String, Object> getSnapshotStats(@Param("batchId") Long batchId);
    Map<String, Object> getCurrentSourceStats();
    Map<String, Object> getDataQualityStats(@Param("batchId") Long batchId);
    void completeBatch(Map<String, Object> body);
    void insertReconciliation(Map<String, Object> body);
    List<Map<String, Object>> listPublishedBatches();
    Map<String, Object> findPublishedBatchByMonth(@Param("monthEndDate") LocalDate monthEndDate);
    Map<String, Object> findLatestPublishedBatch();
    Map<String, Object> findPreviousPublishedBatch(@Param("monthEndDate") LocalDate monthEndDate);
    int countChangeDetails(@Param("currentBatchId") Long currentBatchId, @Param("baseBatchId") Long baseBatchId);
    void deleteChangeDetails(@Param("currentBatchId") Long currentBatchId, @Param("baseBatchId") Long baseBatchId);
    int insertCurrentChangeDetails(@Param("currentBatchId") Long currentBatchId, @Param("baseBatchId") Long baseBatchId);
    int insertExitChangeDetails(@Param("currentBatchId") Long currentBatchId, @Param("baseBatchId") Long baseBatchId);
    Map<String, Object> getPortfolioComparison(@Param("currentBatchId") Long currentBatchId, @Param("baseBatchId") Long baseBatchId);
    List<Map<String, Object>> listMonthlyTrend(@Param("limit") int limit);
    List<Map<String, Object>> listDimensionChanges(@Param("currentBatchId") Long currentBatchId, @Param("baseBatchId") Long baseBatchId, @Param("dimensionType") String dimensionType);
    List<Map<String, Object>> listRiskMigration(@Param("currentBatchId") Long currentBatchId, @Param("baseBatchId") Long baseBatchId);
    List<Map<String, Object>> listRatingMigration(@Param("currentBatchId") Long currentBatchId, @Param("baseBatchId") Long baseBatchId);
    List<Map<String, Object>> listChangeReasonSummary(@Param("currentBatchId") Long currentBatchId, @Param("baseBatchId") Long baseBatchId);
    List<Map<String, Object>> listChangeDetails(@Param("currentBatchId") Long currentBatchId, @Param("baseBatchId") Long baseBatchId, @Param("limit") int limit);
    Map<String, Object> getOverdueDefaultComparison(@Param("currentBatchId") Long currentBatchId, @Param("baseBatchId") Long baseBatchId);
    List<Map<String, Object>> listReconciliation(@Param("batchId") Long batchId);
    List<Map<String, Object>> listSourceManifest(@Param("batchId") Long batchId);
    void insertQualityIssue(Map<String, Object> body);
    List<Map<String, Object>> listQualityIssues(@Param("batchId") Long batchId);
    Map<String, Object> getQualityOverview(@Param("batchId") Long batchId);
    int updateQualityIssue(Map<String, Object> body);
    List<Map<String, Object>> listChangeDrilldown(@Param("currentBatchId") Long currentBatchId,
                                                  @Param("baseBatchId") Long baseBatchId,
                                                  @Param("level") String level,
                                                  @Param("industry") String industry,
                                                  @Param("customerNo") String customerNo,
                                                  @Param("contractNo") String contractNo,
                                                  @Param("changeType") String changeType,
                                                  @Param("limit") int limit);
}
