package com.example.usermanagement.system;

import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.common.service.CrudService;
import com.example.usermanagement.common.web.CrudControllerSupport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequirePermission("notification:manage")
@RestController
@RequestMapping("/notifications")
public class NotificationController extends CrudControllerSupport {
    public NotificationController(@Qualifier("notificationServiceImpl") CrudService notificationServiceImpl) {
        super(notificationServiceImpl);
    }
}
