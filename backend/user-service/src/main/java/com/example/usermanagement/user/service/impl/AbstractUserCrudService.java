package com.example.usermanagement.user.service.impl;

import com.example.usermanagement.common.api.PageResult;
import com.example.usermanagement.user.service.UserCrudService;

import java.util.Map;

abstract class AbstractUserCrudService implements UserCrudService {
    @Override
    public PageResult<Map<String, Object>> list(int page, int size, String keyword) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        return new PageResult<>(listRows(keyword, safeSize, offset), countRows(keyword), safePage, safeSize);
    }

    @Override
    public Map<String, Object> create(Map<String, Object> body) {
        clean(body);
        insert(body);
        return get(((Number) body.get("id")).longValue());
    }

    @Override
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        clean(body);
        updateRow(id, body);
        return get(id);
    }

    protected void clean(Map<String, Object> body) {
        body.remove("id");
        body.remove("created_at");
        body.remove("updated_at");
    }

    protected abstract java.util.List<Map<String, Object>> listRows(String keyword, int limit, int offset);

    protected abstract long countRows(String keyword);

    protected abstract void insert(Map<String, Object> body);

    protected abstract void updateRow(Long id, Map<String, Object> body);
}
