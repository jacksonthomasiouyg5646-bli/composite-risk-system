package com.example.usermanagement.system.service;

import java.util.Map;

public interface RiskModelMonitoringService {
    Map<String, Object> getOverview();

    Map<String, Object> captureSnapshot();

    Map<String, Object> getExternalDataStatus();
}
