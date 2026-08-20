package com.example.usermanagement.system;

import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.common.service.CrudService;
import com.example.usermanagement.common.web.CrudControllerSupport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequirePermission("risk:assess")
@RestController
@RequestMapping("/risks/assessments")
public class RiskAssessmentController extends CrudControllerSupport {
    public RiskAssessmentController(@Qualifier("riskAssessmentServiceImpl") CrudService riskAssessmentServiceImpl) {
        super(riskAssessmentServiceImpl);
    }
}
