package com.example.usermanagement.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RiskIntelligenceMapper {
    void insertExternalDataAccessLog(Map<String, Object> body);

    void insertAiChatLog(Map<String, Object> body);

    Map<String, Object> getExternalDataStatusSummary();

    Map<String, Object> getLatestExternalDataAccess();

    List<Map<String, Object>> listModelSnapshots(@Param("limit") int limit);

    void upsertModelSnapshot(Map<String, Object> body);

    List<Map<String, Object>> listProductReport();
}
