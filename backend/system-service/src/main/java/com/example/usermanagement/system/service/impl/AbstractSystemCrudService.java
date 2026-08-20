package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.common.api.PageResult;
import com.example.usermanagement.common.service.CrudInputGuard;
import com.example.usermanagement.system.service.SystemCrudService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class AbstractSystemCrudService implements SystemCrudService {
    private static final Map<String, Set<String>> EDITABLE_FIELDS = Map.ofEntries(
            Map.entry("LoginLogServiceImpl", Set.of("username", "ip_address", "user_agent", "status", "message")),
            Map.entry("OperationLogServiceImpl", Set.of("username", "module", "action", "method", "request_uri", "status")),
            Map.entry("ErrorLogServiceImpl", Set.of("service_name", "trace_id", "message", "stack_trace")),
            Map.entry("NotificationServiceImpl", Set.of("title", "content", "channel", "target_type", "status")),
            Map.entry("ConfigServiceImpl", Set.of("config_key", "config_value", "description")),
            Map.entry("SecurityPolicyServiceImpl", Set.of("name", "policy_key", "policy_value", "enabled", "description")),
            Map.entry("TenantServiceImpl", Set.of("name", "code", "contact_name", "contact_phone", "status")),
            Map.entry("RiskRegisterServiceImpl", Set.of("risk_code", "risk_name", "category", "level", "owner_department", "responsible_person", "status", "identified_at", "due_date", "description")),
            Map.entry("RiskAssessmentServiceImpl", Set.of("risk_code", "risk_name", "likelihood", "impact", "inherent_level", "residual_level", "assessor", "assessed_at", "conclusion")),
            Map.entry("ControlMeasureServiceImpl", Set.of("control_code", "risk_code", "control_name", "control_type", "frequency", "owner", "effectiveness", "status")),
            Map.entry("TreatmentPlanServiceImpl", Set.of("plan_code", "risk_code", "action", "owner", "due_date", "progress", "status")),
            Map.entry("RiskEventServiceImpl", Set.of("event_code", "title", "risk_code", "severity", "occurred_at", "loss_amount", "owner", "status", "summary")),
            Map.entry("RiskIndicatorServiceImpl", Set.of("indicator_code", "name", "threshold", "current_value", "trend", "owner", "status"))
    );

    @Override
    public PageResult<Map<String, Object>> list(int page, int size, String keyword) {
        int safePage = CrudInputGuard.safePage(page);
        int safeSize = CrudInputGuard.safeSize(size);
        int offset = CrudInputGuard.safeOffset(safePage, safeSize);
        return new PageResult<>(listRows(keyword, safeSize, offset), countRows(keyword), safePage, safeSize);
    }

    @Override
    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        clean(body);
        insert(body);
        return get(((Number) body.get("id")).longValue());
    }

    @Override
    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        CrudInputGuard.requirePositiveId(id);
        clean(body);
        updateRow(id, body);
        return get(id);
    }

    protected void clean(Map<String, Object> body) {
        Set<String> editableFields = EDITABLE_FIELDS.get(getClass().getSimpleName());
        if (editableFields == null) {
            CrudInputGuard.sanitizeBody(body);
            return;
        }
        CrudInputGuard.sanitizeBody(body, editableFields);
    }

    protected abstract List<Map<String, Object>> listRows(String keyword, int limit, int offset);
    protected abstract long countRows(String keyword);
    protected abstract void insert(Map<String, Object> body);
    protected abstract void updateRow(Long id, Map<String, Object> body);
}
