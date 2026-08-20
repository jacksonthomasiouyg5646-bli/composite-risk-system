package com.example.usermanagement.user;

import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.common.service.CrudService;
import com.example.usermanagement.common.web.CrudControllerSupport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequirePermission("user:manage")
@RestController
@RequestMapping("/users")
public class UserController extends CrudControllerSupport {
    public UserController(@Qualifier("userServiceImpl") CrudService userServiceImpl) {
        super(userServiceImpl);
    }
}
