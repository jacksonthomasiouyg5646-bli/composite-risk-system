package com.example.usermanagement.system.service;

import java.util.Map;

public interface CreditAiAnalysisService {
    Map<String, Object> analyzeCustomer(String customer, boolean includeExternal);
}
