package com.example.usermanagement.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface RiskAlertSubscriptionMapper {
    List<Map<String, Object>> listSubscriptions(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    long countSubscriptions(@Param("keyword") String keyword);

    Map<String, Object> getSubscription(@Param("id") Long id);

    List<Map<String, Object>> listReadyDailySubscriptions();

    void insertSubscription(Map<String, Object> body);

    void updateSubscription(@Param("id") Long id, @Param("body") Map<String, Object> body);

    void updateLastDispatch(@Param("id") Long id, @Param("lastDispatchAt") LocalDateTime lastDispatchAt);

    void deleteSubscription(@Param("id") Long id);
}
