package com.example.usermanagement.system.service;

import com.example.usermanagement.common.api.PageResult;

import java.util.Map;

public interface RiskLedgerService {
    PageResult<Map<String, Object>> list(int page, int size, String keyword);
}
