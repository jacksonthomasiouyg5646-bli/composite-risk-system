package com.example.usermanagement.system;

import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.common.service.CrudService;
import com.example.usermanagement.common.web.CrudControllerSupport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequirePermission("tenant:manage")
@RestController
@RequestMapping("/tenants")
public class TenantController extends CrudControllerSupport {
    public TenantController(@Qualifier("tenantServiceImpl") CrudService tenantServiceImpl) {
        super(tenantServiceImpl);
    }
}
