package com.example.usermanagement.common.service;

import java.util.Map;
import java.util.Set;

public final class CrudInputGuard {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SERVER_CONTROLLED_FIELDS = Set.of(
            "id",
            "created_at",
            "updated_at",
            "created_by",
            "updated_by",
            "deleted",
            "deleted_at",
            "deleted_by",
            "version",
            "last_login_at",
            "last_login_ip",
            "login_fail_count",
            "locked_until"
    );

    private CrudInputGuard() {
    }

    public static int safePage(int page) {
        return Math.max(page, 1);
    }

    public static int safeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    public static int safeOffset(int page, int size) {
        long offset = ((long) safePage(page) - 1L) * safeSize(size);
        return offset > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) offset;
    }

    public static void sanitizeBody(Map<String, Object> body) {
        requireBody(body);
        SERVER_CONTROLLED_FIELDS.forEach(body::remove);
        body.entrySet().removeIf(entry -> entry.getValue() == null);
        if (body.isEmpty()) {
            throw new IllegalArgumentException("request body must contain at least one editable field");
        }
    }

    public static void sanitizeBody(Map<String, Object> body, Set<String> editableFields) {
        requireBody(body);
        body.keySet().retainAll(editableFields);
        sanitizeBody(body);
    }

    public static void requireBody(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            throw new IllegalArgumentException("request body must not be empty");
        }
    }

    public static long requirePositiveId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        return id;
    }
}
