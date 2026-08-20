package com.example.usermanagement.user;

import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.common.service.CrudService;
import com.example.usermanagement.common.web.CrudControllerSupport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequirePermission("post:manage")
@RestController
@RequestMapping("/posts")
public class PostController extends CrudControllerSupport {
    public PostController(@Qualifier("postServiceImpl") CrudService postServiceImpl) {
        super(postServiceImpl);
    }
}
