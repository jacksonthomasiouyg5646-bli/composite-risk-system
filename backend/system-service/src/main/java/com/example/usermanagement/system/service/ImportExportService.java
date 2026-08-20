package com.example.usermanagement.system.service;

import java.util.Map;

public interface ImportExportService {
    Map<String, Object> importUsers();

    byte[] exportUsers();
}
