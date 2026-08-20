package com.example.usermanagement.common.security;

import com.example.usermanagement.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

public class PermissionInterceptor implements HandlerInterceptor {
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequirePermission permission = method.getMethodAnnotation(RequirePermission.class);
        if (permission == null) {
            permission = method.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (permission == null) {
            return true;
        }

        Object contextValue = request.getAttribute(ServletAuthFilter.AUTH_CONTEXT_ATTRIBUTE);
        if (contextValue instanceof AuthContext context && context.hasPermission(permission.value())) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(403, "permission denied")));
        return false;
    }
}
