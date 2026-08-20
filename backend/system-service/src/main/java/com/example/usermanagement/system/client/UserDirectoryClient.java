package com.example.usermanagement.system.client;

import com.example.usermanagement.system.client.fallback.UserDirectoryClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "user-service",
        path = "/internal/users",
        fallbackFactory = UserDirectoryClientFallbackFactory.class
)
public interface UserDirectoryClient {
    @GetMapping("/enabled")
    List<Map<String, Object>> listEnabledUsers(
            @RequestHeader("X-Internal-Service-Key") String internalServiceKey);
}
