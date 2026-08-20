package com.example.usermanagement.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RiskLedgerMapper {
    List<Map<String, Object>> listRiskLedgers(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    long countRiskLedgers(@Param("keyword") String keyword);
}
