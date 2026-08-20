package com.example.usermanagement.user.service.impl;

import com.example.usermanagement.user.mapper.AssignmentMapper;
import com.example.usermanagement.user.service.AssignmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssignmentServiceImpl implements AssignmentService {
    private final AssignmentMapper mapper;

    public AssignmentServiceImpl(AssignmentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Long> userRoles(Long userId) {
        return mapper.findRoleIdsByUserId(userId);
    }

    @Override
    @Transactional
    public void saveUserRoles(Long userId, List<Long> roleIds) {
        mapper.deleteUserRoles(userId);
        safeIds(roleIds).forEach(roleId -> mapper.insertUserRole(userId, roleId));
    }

    @Override
    public List<Long> rolePermissions(Long roleId) {
        return mapper.findPermissionIdsByRoleId(roleId);
    }

    @Override
    @Transactional
    public void saveRolePermissions(Long roleId, List<Long> permissionIds) {
        mapper.deleteRolePermissions(roleId);
        safeIds(permissionIds).forEach(permissionId -> mapper.insertRolePermission(roleId, permissionId));
    }

    private List<Long> safeIds(List<Long> ids) {
        return ids == null ? List.of() : ids;
    }
}
