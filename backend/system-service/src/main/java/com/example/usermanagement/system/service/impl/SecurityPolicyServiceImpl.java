package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.SystemCrudMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SecurityPolicyServiceImpl extends AbstractSystemCrudService {
    private final SystemCrudMapper mapper;
    public SecurityPolicyServiceImpl(SystemCrudMapper mapper) { this.mapper = mapper; }
    @Override public Map<String, Object> get(Long id) { return mapper.getSecurityPolicy(id); }
    @Override public void delete(Long id) { mapper.deleteSecurityPolicy(id); }
    @Override protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) { return mapper.listSecurityPolicies(keyword, limit, offset); }
    @Override protected long countRows(String keyword) { return mapper.countSecurityPolicies(keyword); }
    @Override protected void insert(Map<String, Object> body) { mapper.insertSecurityPolicy(body); }
    @Override protected void updateRow(Long id, Map<String, Object> body) { mapper.updateSecurityPolicy(id, body); }
}
