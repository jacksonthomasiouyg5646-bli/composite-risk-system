package com.example.usermanagement.system;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.system.service.RiskManagementReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

@RequirePermission("risk:manage")
@RestController
@RequestMapping("/risks/management-reports")
public class RiskManagementReportController {
    private final RiskManagementReportService riskManagementReportService;

    public RiskManagementReportController(RiskManagementReportService riskManagementReportService) {
        this.riskManagementReportService = riskManagementReportService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getReport() {
        return ApiResponse.ok(riskManagementReportService.getReport());
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportReport() {
        byte[] payload = riskManagementReportService.exportCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("risk-management-report-" + LocalDate.now() + ".csv", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(payload);
    }
}
