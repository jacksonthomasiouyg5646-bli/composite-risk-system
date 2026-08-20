package com.example.usermanagement.user;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.user.service.AssignmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AssignmentController {
    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @RequirePermission("user:manage")
    @GetMapping("/users/{id}/roles")
    public ApiResponse<List<Long>> userRoles(@PathVariable Long id) {
        return ApiResponse.ok(assignmentService.userRoles(id));
    }

    @RequirePermission("user:manage")
    @PutMapping("/users/{id}/roles")
    public ApiResponse<Void> saveUserRoles(@PathVariable Long id, @RequestBody AssignmentRequest request) {
        assignmentService.saveUserRoles(id, request.safeIds());
        return ApiResponse.ok();
    }

    @RequirePermission("role:manage")
    @GetMapping("/roles/{id}/permissions")
    public ApiResponse<List<Long>> rolePermissions(@PathVariable Long id) {
        return ApiResponse.ok(assignmentService.rolePermissions(id));
    }

    @RequirePermission("role:manage")
    @PutMapping("/roles/{id}/permissions")
    public ApiResponse<Void> saveRolePermissions(@PathVariable Long id, @RequestBody AssignmentRequest request) {
        assignmentService.saveRolePermissions(id, request.safeIds());
        return ApiResponse.ok();
    }

    public record AssignmentRequest(List<Long> ids) {
        public List<Long> safeIds() {
            return ids == null ? List.of() : ids;
        }
    }
}
