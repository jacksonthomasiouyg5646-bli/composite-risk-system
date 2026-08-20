package com.example.usermanagement.auth.service.impl;

import com.example.usermanagement.auth.AuthController;
import com.example.usermanagement.auth.mapper.AuthMapper;
import com.example.usermanagement.auth.service.AuthService;
import com.example.usermanagement.common.security.JwtService;
import com.example.usermanagement.common.security.PasswordHashService;
import com.example.usermanagement.common.security.RedisClient;
import com.example.usermanagement.common.security.TokenSessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {
    private static final String CAPTCHA_KEY_PREFIX = "risk:captcha:";
    private static final long CAPTCHA_TTL_SECONDS = 300;

    private final AuthMapper authMapper;
    private final JwtService jwtService;
    private final TokenSessionService tokenSessionService;
    private final RedisClient redisClient;
    private final PasswordHashService passwordHashService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(AuthMapper authMapper, JwtService jwtService, TokenSessionService tokenSessionService,
                           RedisClient redisClient, PasswordHashService passwordHashService) {
        this.authMapper = authMapper;
        this.jwtService = jwtService;
        this.tokenSessionService = tokenSessionService;
        this.redisClient = redisClient;
        this.passwordHashService = passwordHashService;
    }

    @Override
    public Map<String, Object> login(AuthController.LoginRequest request, HttpServletRequest httpRequest) {
        if (!validCaptcha(request.captchaId(), request.captchaCode())) {
            writeLoginLog(request.username(), "FAILED", httpRequest);
            throw new IllegalArgumentException("invalid captcha");
        }
        Map<String, Object> user = authMapper.findUserByUsername(request.username());
        String passwordHash = user == null ? null : String.valueOf(user.get("password_hash"));
        if (user == null || !"ENABLED".equals(String.valueOf(user.get("status")))
                || !passwordHashService.matches(request.password(), passwordHash)) {
            writeLoginLog(request.username(), "FAILED", httpRequest);
            throw new IllegalArgumentException("invalid username or password");
        }

        if (passwordHashService.needsUpgrade(passwordHash)) {
            authMapper.updatePasswordByUsername(request.username(), passwordHashService.encode(request.password()));
        }

        Long userId = ((Number) user.get("id")).longValue();
        Long tenantId = ((Number) user.get("tenant_id")).longValue();
        List<String> roles = authMapper.findRoleCodesByUserId(userId);
        List<String> permissions = authMapper.findPermissionCodesByUserId(userId);
        String tokenId = java.util.UUID.randomUUID().toString();
        String username = String.valueOf(user.get("username"));
        String token = jwtService.createToken(tokenId, userId, username, roles, permissions, tenantId);
        tokenSessionService.create(tokenId, userId, username);
        authMapper.updateLastLoginAt(userId);
        writeLoginLog(request.username(), "SUCCESS", httpRequest);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("token", token);
        payload.put("user", publicUser(user));
        payload.put("roles", roles);
        payload.put("permissions", permissions);
        payload.put("expiresIn", tokenSessionService.ttlSeconds());
        return payload;
    }

    @Override
    public Map<String, Object> captcha() {
        String text = String.format("%06d", secureRandom.nextInt(1_000_000));
        String captchaId = java.util.UUID.randomUUID().toString();
        redisClient.setEx(CAPTCHA_KEY_PREFIX + captchaId, text, CAPTCHA_TTL_SECONDS);
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='120' height='40'><rect width='120' height='40' fill='#eef2ff'/><text x='24' y='27' font-size='22' fill='#1f2937'>" + text + "</text></svg>";
        return Map.of(
                "captchaId", captchaId,
                "image", "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8))
        );
    }

    @Override
    public Map<String, Object> profile(Long userId, List<String> roles, List<String> permissions) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("user", publicUser(authMapper.findPublicUserById(userId)));
        payload.put("roles", roles);
        payload.put("permissions", permissions);
        payload.put("serverTime", Instant.now());
        return payload;
    }

    @Override
    public void logout(String token) {
        tokenSessionService.revoke(jwtService.tokenId(token));
    }

    private Map<String, Object> publicUser(Map<String, Object> user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.get("id"));
        result.put("username", user.get("username"));
        result.put("display_name", user.get("display_name"));
        result.put("email", user.get("email"));
        result.put("phone", user.get("phone"));
        result.put("avatar", user.get("avatar"));
        result.put("status", user.get("status"));
        result.put("tenant_id", user.get("tenant_id"));
        return result;
    }

    private void writeLoginLog(String username, String status, HttpServletRequest request) {
        authMapper.insertLoginLog(
                username,
                request.getRemoteAddr(),
                truncate(request.getHeader("User-Agent"), 255),
                status,
                "login " + status.toLowerCase());
    }

    private boolean validCaptcha(String captchaId, String captchaCode) {
        if (captchaId == null || captchaId.isBlank() || captchaCode == null || captchaCode.isBlank()) {
            return false;
        }
        String key = CAPTCHA_KEY_PREFIX + captchaId;
        String expected = redisClient.get(key);
        redisClient.delete(key);
        return expected != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                captchaCode.trim().getBytes(StandardCharsets.UTF_8));
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
