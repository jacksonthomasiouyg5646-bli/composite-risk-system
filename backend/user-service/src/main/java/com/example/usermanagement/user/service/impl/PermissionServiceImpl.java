package com.example.usermanagement.user.service.impl;

import com.example.usermanagement.user.mapper.UserCrudMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PermissionServiceImpl extends AbstractUserCrudService {
    private final UserCrudMapper mapper;

    public PermissionServiceImpl(UserCrudMapper mapper) {
        this.mapper = mapper;
    }

    @Override public Map<String, Object> get(Long id) { return mapper.getPermission(id); }
    @Override public void delete(Long id) { mapper.deletePermission(id); }
    @Override protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) { return mapper.listPermissions(keyword, limit, offset); }
    @Override protected long countRows(String keyword) { return mapper.countPermissions(keyword); }
    @Override protected void insert(Map<String, Object> body) { mapper.insertPermission(body); }
    @Override protected void updateRow(Long id, Map<String, Object> body) { mapper.updatePermission(id, body); }
}
