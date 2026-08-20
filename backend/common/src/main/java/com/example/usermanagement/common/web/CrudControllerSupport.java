package com.example.usermanagement.common.web;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.api.PageResult;
import com.example.usermanagement.common.service.CrudService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

public abstract class CrudControllerSupport {
    private final CrudService service;

    protected CrudControllerSupport(CrudService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.ok(service.list(page, size, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable("id") Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(service.create(body));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ApiResponse.ok();
    }
}
