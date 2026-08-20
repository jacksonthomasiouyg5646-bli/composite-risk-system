package com.example.usermanagement.gateway;

import com.example.usermanagement.gateway.apollo.ApolloStartupGuard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        ApolloStartupGuard.require("api-gateway", "application,gateway,security");
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
