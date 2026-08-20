package com.example.usermanagement.user;

import com.example.usermanagement.common.apollo.ApolloStartupGuard;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan({
        "com.example.usermanagement.common.mapper",
        "com.example.usermanagement.user.mapper"
})
@SpringBootApplication(scanBasePackages = "com.example.usermanagement")
public class UserServiceApplication {
    public static void main(String[] args) {
        ApolloStartupGuard.require("user-service", "application,database,security");
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
