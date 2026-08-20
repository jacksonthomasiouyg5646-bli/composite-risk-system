package com.example.usermanagement.user;

import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.common.service.CrudService;
import com.example.usermanagement.common.web.CrudControllerSupport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequirePermission("permission:manage")
@RestController
@RequestMapping("/permissions")
public class PermissionController extends CrudControllerSupport {
    public PermissionController(@Qualifier("permissionServiceImpl") CrudService permissionServiceImpl) {
        super(permissionServiceImpl);
    }
}
