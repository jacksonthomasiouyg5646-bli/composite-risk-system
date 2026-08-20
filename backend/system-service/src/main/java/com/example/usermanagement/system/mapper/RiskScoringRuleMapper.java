package com.example.usermanagement.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RiskScoringRuleMapper {
    List<Map<String, Object>> listEnabledRules();

    List<Map<String, Object>> listRules(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    long countRules(@Param("keyword") String keyword);

    Map<String, Object> getRule(@Param("id") Long id);

    void insertRule(Map<String, Object> body);

    void updateRule(@Param("id") Long id, @Param("body") Map<String, Object> body);

    void deleteRule(@Param("id") Long id);
}
