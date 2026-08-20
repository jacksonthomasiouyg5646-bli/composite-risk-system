package com.example.usermanagement.system.service;

import com.example.usermanagement.common.api.PageResult;

import java.util.Map;

public interface RiskScoringRuleService {
    PageResult<Map<String, Object>> list(int page, int size, String keyword);

    Map<String, Object> create(Map<String, Object> body);

    Map<String, Object> update(Long id, Map<String, Object> body);

    void delete(Long id);
}
