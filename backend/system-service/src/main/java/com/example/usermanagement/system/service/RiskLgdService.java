package com.example.usermanagement.system.service;

import com.example.usermanagement.common.api.PageResult;

import java.util.Map;

public interface RiskLgdService {
    Map<String, Object> getOverview();

    PageResult<Map<String, Object>> listLedger(Map<String, Object> filters, int page, int size);

    Map<String, Object> getDebtDetail(String debtNo);

    Map<String, Object> getCustomerLgd(String customerNo);

    Map<String, Object> getGovernance();

    Map<String, Object> captureCalculationRun(String operator);

    Map<String, Object> runStressTest(Map<String, Object> body, String operator);
}
