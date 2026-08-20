package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.SystemCrudMapper;
import com.example.usermanagement.system.service.ImportExportService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class ImportExportServiceImpl implements ImportExportService {
    private final SystemCrudMapper mapper;

    public ImportExportServiceImpl(SystemCrudMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, Object> importUsers() {
        return Map.of("imported", 0, "message", "Upload parsing can be extended here; endpoint is ready.");
    }

    @Override
    public byte[] exportUsers() {
        StringBuilder csv = new StringBuilder("id,username,display_name,email,phone,status\n");
        mapper.listUsersForExport().forEach(row -> {
            csv.append(row.get("id")).append(',')
                    .append(row.get("username")).append(',')
                    .append(row.get("display_name")).append(',')
                    .append(row.get("email")).append(',')
                    .append(row.get("phone")).append(',')
                    .append(row.get("status")).append('\n');
        });
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}
