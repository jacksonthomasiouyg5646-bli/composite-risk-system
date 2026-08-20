package com.example.usermanagement.system;

import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.common.service.CrudService;
import com.example.usermanagement.common.web.CrudControllerSupport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequirePermission("log:view")
@RestController
@RequestMapping("/logs/operation")
public class OperationLogController extends CrudControllerSupport {
    public OperationLogController(@Qualifier("operationLogServiceImpl") CrudService operationLogServiceImpl) {
        super(operationLogServiceImpl);
    }
}
