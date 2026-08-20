package com.example.usermanagement.user;

import com.example.usermanagement.user.service.impl.UserServiceImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/users")
public class UserDirectoryController {
    private final UserServiceImpl userServiceImpl;

    public UserDirectoryController(UserServiceImpl userServiceImpl) {
        this.userServiceImpl = userServiceImpl;
    }

    @GetMapping("/enabled")
    public List<Map<String, Object>> listEnabledUsers() {
        return userServiceImpl.listEnabledUsers();
    }
}
