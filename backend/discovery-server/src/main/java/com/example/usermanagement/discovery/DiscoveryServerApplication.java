package com.example.usermanagement.discovery;

import com.example.usermanagement.discovery.apollo.ApolloStartupGuard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class DiscoveryServerApplication {
    public static void main(String[] args) {
        ApolloStartupGuard.require("discovery-server", "application");
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
