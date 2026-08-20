package com.example.usermanagement.common.web;

import com.example.usermanagement.common.security.AuthContext;
import com.example.usermanagement.common.security.ServletAuthFilter;
import com.example.usermanagement.common.mapper.CommonLogMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class OperationLogFilter extends OncePerRequestFilter {
    private final CommonLogMapper commonLogMapper;

    public OperationLogFilter(CommonLogMapper commonLogMapper) {
        this.commonLogMapper = commonLogMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            writeLog(request, response);
        }
    }

    private void writeLog(HttpServletRequest request, HttpServletResponse response) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return;
        }
        try {
            Object contextValue = request.getAttribute(ServletAuthFilter.AUTH_CONTEXT_ATTRIBUTE);
            String username = contextValue instanceof AuthContext context ? context.username() : null;
            commonLogMapper.insertOperationLog(
                    username,
                    moduleName(request.getRequestURI()),
                    request.getRequestURI(),
                    method,
                    request.getRequestURI(),
                    response.getStatus() < 400 ? "SUCCESS" : "FAILED");
        } catch (Exception ignored) {
            // Logging must not break business requests.
        }
    }

    private String moduleName(String uri) {
        String[] parts = uri.split("/");
        return parts.length > 1 && !parts[1].isBlank() ? parts[1] : "system";
    }
}
