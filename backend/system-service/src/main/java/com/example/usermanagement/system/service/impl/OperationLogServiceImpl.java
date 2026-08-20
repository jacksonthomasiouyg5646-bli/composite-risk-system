package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.SystemCrudMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OperationLogServiceImpl extends AbstractSystemCrudService {
    private final SystemCrudMapper mapper;
    public OperationLogServiceImpl(SystemCrudMapper mapper) { this.mapper = mapper; }
    @Override public Map<String, Object> get(Long id) { return mapper.getOperationLog(id); }
    @Override public void delete(Long id) { mapper.deleteOperationLog(id); }
    @Override protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) { return mapper.listOperationLogs(keyword, limit, offset); }
    @Override protected long countRows(String keyword) { return mapper.countOperationLogs(keyword); }
    @Override protected void insert(Map<String, Object> body) { mapper.insertOperationLog(body); }
    @Override protected void updateRow(Long id, Map<String, Object> body) { mapper.updateOperationLog(id, body); }
}
