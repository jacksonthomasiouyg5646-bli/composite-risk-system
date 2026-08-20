package com.example.usermanagement.common.security;

import com.example.usermanagement.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public class ServletAuthFilter extends OncePerRequestFilter {
    public static final String AUTH_CONTEXT_ATTRIBUTE = "authContext";

    private final JwtService jwtService;
    private final TokenSessionService tokenSessionService;
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    private final List<String> publicPrefixes;
    private final String internalServiceKey;

    public ServletAuthFilter(JwtService jwtService, TokenSessionService tokenSessionService,
                             List<String> publicPrefixes, String internalServiceKey) {
        this.jwtService = jwtService;
        this.tokenSessionService = tokenSessionService;
        this.publicPrefixes = publicPrefixes;
        this.internalServiceKey = internalServiceKey == null ? "" : internalServiceKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (request.getRequestURI().startsWith("/internal/")) {
            String providedKey = request.getHeader("X-Internal-Service-Key");
            if (!matchesInternalServiceKey(providedKey)) {
                unauthorized(response, "invalid internal service credentials");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            unauthorized(response, "missing token");
            return;
        }

        try {
            String token = header.substring(7);
            String tokenId = jwtService.tokenId(token);
            if (!tokenSessionService.validateAndRefresh(tokenId)) {
                unauthorized(response, "session expired");
                return;
            }
            AuthContext context = jwtService.parse(token);
            request.setAttribute(AUTH_CONTEXT_ATTRIBUTE, context);
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            unauthorized(response, "invalid token");
        }
    }

    private boolean isPublic(String uri) {
        return publicPrefixes.stream().anyMatch(uri::startsWith);
    }

    private boolean matchesInternalServiceKey(String providedKey) {
        return !internalServiceKey.isBlank()
                && providedKey != null
                && MessageDigest.isEqual(
                internalServiceKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8));
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(401, message)));
    }
}
