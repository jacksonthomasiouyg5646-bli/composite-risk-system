package com.example.usermanagement.auth;

import com.example.usermanagement.auth.service.AuthService;
import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.security.AuthContext;
import com.example.usermanagement.common.security.ServletAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            return ApiResponse.ok(authService.login(request, httpRequest));
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(401, ex.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7));
        }
        return ApiResponse.ok();
    }

    @GetMapping("/captcha")
    public ApiResponse<Map<String, Object>> captcha() {
        return ApiResponse.ok(authService.captcha());
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile(HttpServletRequest request) {
        AuthContext context = (AuthContext) request.getAttribute(ServletAuthFilter.AUTH_CONTEXT_ATTRIBUTE);
        return ApiResponse.ok(authService.profile(context.userId(), context.roles(), context.permissions()));
    }

    public record LoginRequest(String username, String password, String captchaId, String captchaCode) {
    }

}
