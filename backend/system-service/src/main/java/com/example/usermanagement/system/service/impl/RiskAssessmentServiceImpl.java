package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.SystemCrudMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RiskAssessmentServiceImpl extends AbstractSystemCrudService {
    private final SystemCrudMapper mapper;

    public RiskAssessmentServiceImpl(SystemCrudMapper mapper) {
        this.mapper = mapper;
    }

    @Override public Map<String, Object> get(Long id) { return mapper.getRiskAssessment(id); }
    @Override public void delete(Long id) { mapper.deleteRiskAssessment(id); }
    @Override protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) { return mapper.listRiskAssessments(keyword, limit, offset); }
    @Override protected long countRows(String keyword) { return mapper.countRiskAssessments(keyword); }
    @Override protected void insert(Map<String, Object> body) { mapper.insertRiskAssessment(body); }
    @Override protected void updateRow(Long id, Map<String, Object> body) { mapper.updateRiskAssessment(id, body); }
}
