package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.SystemCrudMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RiskIndicatorServiceImpl extends AbstractSystemCrudService {
    private final SystemCrudMapper mapper;

    public RiskIndicatorServiceImpl(SystemCrudMapper mapper) {
        this.mapper = mapper;
    }

    @Override public Map<String, Object> get(Long id) { return mapper.getRiskIndicator(id); }
    @Override public void delete(Long id) { mapper.deleteRiskIndicator(id); }
    @Override protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) { return mapper.listRiskIndicators(keyword, limit, offset); }
    @Override protected long countRows(String keyword) { return mapper.countRiskIndicators(keyword); }
    @Override protected void insert(Map<String, Object> body) { mapper.insertRiskIndicator(body); }
    @Override protected void updateRow(Long id, Map<String, Object> body) { mapper.updateRiskIndicator(id, body); }
}
