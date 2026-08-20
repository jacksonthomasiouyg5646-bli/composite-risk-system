package com.example.usermanagement.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CreditAiAnalysisMapper {
    Map<String, Object> findCustomerProfile(@Param("customer") String customer);

    List<Map<String, Object>> listRecentDefaults(@Param("customerNo") String customerNo, @Param("limit") int limit);

    List<Map<String, Object>> listRecentOverdues(@Param("customerNo") String customerNo, @Param("limit") int limit);

    List<Map<String, Object>> listRecentContracts(@Param("customerNo") String customerNo, @Param("limit") int limit);

    List<Map<String, Object>> listCollateralSummary(@Param("customerNo") String customerNo, @Param("limit") int limit);

    List<Map<String, Object>> listCustomerTimeline(@Param("customerNo") String customerNo, @Param("limit") int limit);

    List<Map<String, Object>> listCustomerBusinessTimeline(@Param("customerNo") String customerNo, @Param("limit") int limit);
}
