package com.example.usermanagement.auth.service;

import com.example.usermanagement.auth.AuthController;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface AuthService {
    Map<String, Object> login(AuthController.LoginRequest request, HttpServletRequest httpRequest);

    Map<String, Object> captcha();

    Map<String, Object> profile(Long userId, java.util.List<String> roles, java.util.List<String> permissions);

    void logout(String token);
}
