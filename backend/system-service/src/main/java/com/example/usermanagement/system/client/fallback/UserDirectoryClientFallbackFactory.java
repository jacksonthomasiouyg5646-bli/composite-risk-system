package com.example.usermanagement.system.client.fallback;

import com.example.usermanagement.system.client.UserDirectoryClient;
import com.example.usermanagement.system.mapper.SystemCrudMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class UserDirectoryClientFallbackFactory implements FallbackFactory<UserDirectoryClient> {
    private static final Logger log = LoggerFactory.getLogger(UserDirectoryClientFallbackFactory.class);

    private final SystemCrudMapper mapper;

    public UserDirectoryClientFallbackFactory(SystemCrudMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public UserDirectoryClient create(Throwable cause) {
        log.warn("user-service is unavailable, use local user directory fallback", cause);
        return internalServiceKey -> {
            try {
                return mapper.listUsersForExport().stream()
                        .filter(this::isEnabledUser)
                        .map(row -> {
                            Map<String, Object> copy = new LinkedHashMap<>(row);
                            return copy;
                        })
                        .toList();
            } catch (Exception ex) {
                log.error("local fallback for enabled users failed", ex);
                return List.of();
            }
        };
    }

    private boolean isEnabledUser(Map<String, Object> row) {
        return row != null
                && "ENABLED".equalsIgnoreCase(Objects.toString(row.get("status"), ""))
                && !Objects.toString(row.get("email"), "").isBlank();
    }
}
