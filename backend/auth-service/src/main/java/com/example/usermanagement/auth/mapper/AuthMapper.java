package com.example.usermanagement.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AuthMapper {
    Map<String, Object> findUserByUsername(@Param("username") String username);

    Map<String, Object> findPublicUserById(@Param("id") Long id);

    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);

    void updateLastLoginAt(@Param("id") Long id);

    void updatePasswordByUsername(@Param("username") String username, @Param("passwordHash") String passwordHash);

    void insertLoginLog(
            @Param("username") String username,
            @Param("ipAddress") String ipAddress,
            @Param("userAgent") String userAgent,
            @Param("status") String status,
            @Param("message") String message);
}
