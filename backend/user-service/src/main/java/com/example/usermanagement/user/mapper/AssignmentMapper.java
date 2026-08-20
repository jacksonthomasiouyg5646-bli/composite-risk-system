package com.example.usermanagement.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssignmentMapper {
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    void deleteUserRoles(@Param("userId") Long userId);

    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    List<Long> findPermissionIdsByRoleId(@Param("roleId") Long roleId);

    void deleteRolePermissions(@Param("roleId") Long roleId);

    void insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}
