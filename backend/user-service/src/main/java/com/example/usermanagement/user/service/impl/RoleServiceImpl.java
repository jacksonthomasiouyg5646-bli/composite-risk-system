package com.example.usermanagement.user.service.impl;

import com.example.usermanagement.user.mapper.UserCrudMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RoleServiceImpl extends AbstractUserCrudService {
    private final UserCrudMapper mapper;

    public RoleServiceImpl(UserCrudMapper mapper) {
        this.mapper = mapper;
    }

    @Override public Map<String, Object> get(Long id) { return mapper.getRole(id); }
    @Override public void delete(Long id) { mapper.deleteRole(id); }
    @Override protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) { return mapper.listRoles(keyword, limit, offset); }
    @Override protected long countRows(String keyword) { return mapper.countRoles(keyword); }
    @Override protected void insert(Map<String, Object> body) { mapper.insertRole(body); }
    @Override protected void updateRow(Long id, Map<String, Object> body) { mapper.updateRole(id, body); }
}
