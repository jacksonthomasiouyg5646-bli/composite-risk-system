package com.example.usermanagement.user.service;

import java.util.List;

public interface AssignmentService {
    List<Long> userRoles(Long userId);

    void saveUserRoles(Long userId, List<Long> roleIds);

    List<Long> rolePermissions(Long roleId);

    void saveRolePermissions(Long roleId, List<Long> permissionIds);
}
