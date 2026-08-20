package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.SystemCrudMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TenantServiceImpl extends AbstractSystemCrudService {
    private final SystemCrudMapper mapper;
    public TenantServiceImpl(SystemCrudMapper mapper) { this.mapper = mapper; }
    @Override public Map<String, Object> get(Long id) { return mapper.getTenant(id); }
    @Override public void delete(Long id) { mapper.deleteTenant(id); }
    @Override protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) { return mapper.listTenants(keyword, limit, offset); }
    @Override protected long countRows(String keyword) { return mapper.countTenants(keyword); }
    @Override protected void insert(Map<String, Object> body) { mapper.insertTenant(body); }
    @Override protected void updateRow(Long id, Map<String, Object> body) { mapper.updateTenant(id, body); }
}
