package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.SystemCrudMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LoginLogServiceImpl extends AbstractSystemCrudService {
    private final SystemCrudMapper mapper;
    public LoginLogServiceImpl(SystemCrudMapper mapper) { this.mapper = mapper; }
    @Override public Map<String, Object> get(Long id) { return mapper.getLoginLog(id); }
    @Override public void delete(Long id) { mapper.deleteLoginLog(id); }
    @Override protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) { return mapper.listLoginLogs(keyword, limit, offset); }
    @Override protected long countRows(String keyword) { return mapper.countLoginLogs(keyword); }
    @Override protected void insert(Map<String, Object> body) { mapper.insertLoginLog(body); }
    @Override protected void updateRow(Long id, Map<String, Object> body) { mapper.updateLoginLog(id, body); }
}
