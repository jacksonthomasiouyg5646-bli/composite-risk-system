package com.example.usermanagement.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface CreditDefaultTrendMapper {
    String findMaxStatDate();

    List<Map<String, Object>> listDebtDefaultDaily(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("keyword") String keyword,
            @Param("defaultLevel") String defaultLevel);

    List<Map<String, Object>> listCustomerDefaultDaily(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("keyword") String keyword,
            @Param("defaultLevel") String defaultLevel);

    List<Map<String, Object>> listOverdueDaily(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("keyword") String keyword,
            @Param("defaultLevel") String defaultLevel);

    List<Map<String, Object>> listDefaultLevelDistribution(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("keyword") String keyword,
            @Param("defaultLevel") String defaultLevel);

    List<Map<String, Object>> listRecentDefaults(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("keyword") String keyword,
            @Param("defaultLevel") String defaultLevel,
            @Param("limit") int limit);
}
