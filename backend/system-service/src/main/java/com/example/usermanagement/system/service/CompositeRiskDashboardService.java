package com.example.usermanagement.system.service;

import java.util.Map;
import java.util.List;

public interface CompositeRiskDashboardService {
    Map<String, Object> getOverview();

    List<Map<String, Object>> listCustomerScorings();

    Map<String, Object> getCustomerScoring(String customerNo);

    Map<String, Object> createTreatment(String customerNo);
}
