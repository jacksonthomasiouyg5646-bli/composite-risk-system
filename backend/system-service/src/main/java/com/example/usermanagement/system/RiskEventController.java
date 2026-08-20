package com.example.usermanagement.system;

import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.common.service.CrudService;
import com.example.usermanagement.common.web.CrudControllerSupport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequirePermission("risk:event")
@RestController
@RequestMapping("/risks/events")
public class RiskEventController extends CrudControllerSupport {
    public RiskEventController(@Qualifier("riskEventServiceImpl") CrudService riskEventServiceImpl) {
        super(riskEventServiceImpl);
    }
}
