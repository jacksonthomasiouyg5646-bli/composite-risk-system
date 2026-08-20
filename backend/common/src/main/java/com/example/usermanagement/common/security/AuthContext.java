package com.example.usermanagement.common.security;

import java.util.List;

public record AuthContext(Long userId, String username, List<String> roles, List<String> permissions, Long tenantId) {
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
