package com.example.usermanagement.system.service;

import java.time.LocalDate;
import java.util.Map;

public interface CreditDefaultTrendService {
    Map<String, Object> getTrend(LocalDate startDate, LocalDate endDate, String keyword, String defaultLevel);
}
