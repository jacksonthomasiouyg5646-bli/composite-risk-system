package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.SystemCrudMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ControlMeasureServiceImpl extends AbstractSystemCrudService {
    private final SystemCrudMapper mapper;

    public ControlMeasureServiceImpl(SystemCrudMapper mapper) {
        this.mapper = mapper;
    }

    @Override public Map<String, Object> get(Long id) { return mapper.getControlMeasure(id); }
    @Override public void delete(Long id) { mapper.deleteControlMeasure(id); }
    @Override protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) { return mapper.listControlMeasures(keyword, limit, offset); }
    @Override protected long countRows(String keyword) { return mapper.countControlMeasures(keyword); }
    @Override protected void insert(Map<String, Object> body) { mapper.insertControlMeasure(body); }
    @Override protected void updateRow(Long id, Map<String, Object> body) { mapper.updateControlMeasure(id, body); }
}
