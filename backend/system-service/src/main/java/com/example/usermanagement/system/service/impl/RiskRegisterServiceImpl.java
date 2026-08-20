package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.SystemCrudMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RiskRegisterServiceImpl extends AbstractSystemCrudService {
    private final SystemCrudMapper mapper;

    public RiskRegisterServiceImpl(SystemCrudMapper mapper) {
        this.mapper = mapper;
    }

    @Override public Map<String, Object> get(Long id) { return mapper.getRiskRegister(id); }
    @Override public void delete(Long id) { mapper.deleteRiskRegister(id); }
    @Override protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) { return mapper.listRiskRegisters(keyword, limit, offset); }
    @Override protected long countRows(String keyword) { return mapper.countRiskRegisters(keyword); }
    @Override protected void insert(Map<String, Object> body) { mapper.insertRiskRegister(body); }
    @Override protected void updateRow(Long id, Map<String, Object> body) { mapper.updateRiskRegister(id, body); }
}
