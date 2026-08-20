package com.example.usermanagement.system;

import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.common.service.CrudService;
import com.example.usermanagement.common.web.CrudControllerSupport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequirePermission("risk:treat")
@RestController
@RequestMapping("/risks/treatments")
public class TreatmentPlanController extends CrudControllerSupport {
    public TreatmentPlanController(@Qualifier("treatmentPlanServiceImpl") CrudService treatmentPlanServiceImpl) {
        super(treatmentPlanServiceImpl);
    }
}
