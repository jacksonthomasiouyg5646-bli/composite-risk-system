package com.example.usermanagement.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommonLogMapper {
    void insertOperationLog(
            @Param("username") String username,
            @Param("module") String module,
            @Param("action") String action,
            @Param("method") String method,
            @Param("requestUri") String requestUri,
            @Param("status") String status);

    void insertErrorLog(
            @Param("serviceName") String serviceName,
            @Param("traceId") String traceId,
            @Param("message") String message,
            @Param("stackTrace") String stackTrace);
}
