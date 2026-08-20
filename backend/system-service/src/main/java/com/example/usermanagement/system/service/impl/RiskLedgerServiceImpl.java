package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.common.api.PageResult;
import com.example.usermanagement.system.mapper.RiskLedgerMapper;
import com.example.usermanagement.system.service.RiskLedgerService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RiskLedgerServiceImpl implements RiskLedgerService {
    private final RiskLedgerMapper mapper;

    public RiskLedgerServiceImpl(RiskLedgerMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PageResult<Map<String, Object>> list(int page, int size, String keyword) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<Map<String, Object>> rows = mapper.listRiskLedgers(keyword, safeSize, offset);
        long total = mapper.countRiskLedgers(keyword);
        return new PageResult<>(rows, total, safePage, safeSize);
    }
}
