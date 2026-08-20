package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.SystemCrudMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RiskEventServiceImpl extends AbstractSystemCrudService {
    private final SystemCrudMapper mapper;

    public RiskEventServiceImpl(SystemCrudMapper mapper) {
        this.mapper = mapper;
    }

    @Override public Map<String, Object> get(Long id) { return mapper.getRiskEvent(id); }
    @Override public void delete(Long id) { mapper.deleteRiskEvent(id); }
    @Override protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) { return mapper.listRiskEvents(keyword, limit, offset); }
    @Override protected long countRows(String keyword) { return mapper.countRiskEvents(keyword); }
    @Override protected void insert(Map<String, Object> body) { mapper.insertRiskEvent(body); }
    @Override protected void updateRow(Long id, Map<String, Object> body) { mapper.updateRiskEvent(id, body); }
}
