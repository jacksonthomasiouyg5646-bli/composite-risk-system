package com.example.usermanagement.user.service.impl;

import com.example.usermanagement.common.api.PageResult;
import com.example.usermanagement.common.service.CrudInputGuard;
import com.example.usermanagement.user.service.UserCrudService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

abstract class AbstractUserCrudService implements UserCrudService {
    private static final Map<String, Set<String>> EDITABLE_FIELDS = Map.of(
            "UserServiceImpl", Set.of("tenant_id", "department_id", "post_id", "username", "password_hash", "display_name", "email", "phone", "avatar", "status"),
            "RoleServiceImpl", Set.of("tenant_id", "name", "code", "description", "status"),
            "PermissionServiceImpl", Set.of("name", "code", "module", "description"),
            "DepartmentServiceImpl", Set.of("tenant_id", "parent_id", "name", "code", "leader", "phone", "sort_order", "status"),
            "PostServiceImpl", Set.of("tenant_id", "name", "code", "sort_order", "status"),
            "MenuServiceImpl", Set.of("parent_id", "title", "path", "component", "icon", "permission_code", "sort_order", "visible")
    );

    @Override
    public PageResult<Map<String, Object>> list(int page, int size, String keyword) {
        int safePage = CrudInputGuard.safePage(page);
        int safeSize = CrudInputGuard.safeSize(size);
        int offset = CrudInputGuard.safeOffset(safePage, safeSize);
        return new PageResult<>(listRows(keyword, safeSize, offset), countRows(keyword), safePage, safeSize);
    }

    @Override
    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        clean(body);
        insert(body);
        return get(((Number) body.get("id")).longValue());
    }

    @Override
    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        CrudInputGuard.requirePositiveId(id);
        clean(body);
        updateRow(id, body);
        return get(id);
    }

    protected void clean(Map<String, Object> body) {
        Set<String> editableFields = EDITABLE_FIELDS.get(getClass().getSimpleName());
        if (editableFields == null) {
            CrudInputGuard.sanitizeBody(body);
            return;
        }
        CrudInputGuard.sanitizeBody(body, editableFields);
    }

    protected abstract java.util.List<Map<String, Object>> listRows(String keyword, int limit, int offset);

    protected abstract long countRows(String keyword);

    protected abstract void insert(Map<String, Object> body);

    protected abstract void updateRow(Long id, Map<String, Object> body);
}
