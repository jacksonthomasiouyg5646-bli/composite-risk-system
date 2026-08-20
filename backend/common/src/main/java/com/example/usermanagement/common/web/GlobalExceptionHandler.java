package com.example.usermanagement.common.web;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.mapper.CommonLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final CommonLogMapper commonLogMapper;

    public GlobalExceptionHandler(CommonLogMapper commonLogMapper) {
        this.commonLogMapper = commonLogMapper;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handle(Exception ex, HttpServletRequest request) {
        try {
            commonLogMapper.insertErrorLog(
                    "backend-service",
                    request.getHeader("X-Request-Id"),
                    ex.getMessage(),
                    ex.toString());
        } catch (Exception ignored) {
            // Error logging must not hide the original failure.
        }
        return ApiResponse.fail(500, ex.getMessage());
    }
}
