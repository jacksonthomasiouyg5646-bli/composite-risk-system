package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.SystemCrudMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TreatmentPlanServiceImpl extends AbstractSystemCrudService {
    private final SystemCrudMapper mapper;

    public TreatmentPlanServiceImpl(SystemCrudMapper mapper) {
        this.mapper = mapper;
    }

    @Override public Map<String, Object> get(Long id) { return mapper.getTreatmentPlan(id); }
    @Override public void delete(Long id) { mapper.deleteTreatmentPlan(id); }
    @Override protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) { return mapper.listTreatmentPlans(keyword, limit, offset); }
    @Override protected long countRows(String keyword) { return mapper.countTreatmentPlans(keyword); }
    @Override protected void insert(Map<String, Object> body) { mapper.insertTreatmentPlan(body); }
    @Override protected void updateRow(Long id, Map<String, Object> body) { mapper.updateTreatmentPlan(id, body); }
}
