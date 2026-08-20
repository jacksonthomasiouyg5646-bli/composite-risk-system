package com.example.usermanagement.auth;

import com.example.usermanagement.common.apollo.ApolloStartupGuard;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan({
        "com.example.usermanagement.common.mapper",
        "com.example.usermanagement.auth.mapper"
})
@SpringBootApplication(scanBasePackages = "com.example.usermanagement")
public class AuthServiceApplication {
    public static void main(String[] args) {
        ApolloStartupGuard.require("auth-service", "application,database,security");
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
