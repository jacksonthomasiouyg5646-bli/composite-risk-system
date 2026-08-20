package com.example.usermanagement.common.config;

import com.example.usermanagement.common.security.JwtService;
import com.example.usermanagement.common.security.PasswordHashService;
import com.example.usermanagement.common.security.PermissionInterceptor;
import com.example.usermanagement.common.security.RedisClient;
import com.example.usermanagement.common.security.ServletAuthFilter;
import com.example.usermanagement.common.security.TokenSessionService;
import com.example.usermanagement.common.mapper.CommonLogMapper;
import com.example.usermanagement.common.web.OperationLogFilter;
import com.example.usermanagement.common.web.RequestTraceFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class CommonSecurityConfig implements WebMvcConfigurer {
    private static final List<String> PUBLIC_PATHS = List.of(
            "/actuator",
            "/error",
            "/auth/login",
            "/auth/captcha"
    );

    @Bean
    public JwtService jwtService(
            @Value("${app.jwt.rsa.private-key:}") String privateKey,
            @Value("${app.jwt.rsa.public-key}") String publicKey,
            @Value("${app.jwt.ttl-seconds:86400}") long ttlSeconds,
            @Value("${app.jwt.rsa.encryption-enabled:true}") boolean encryptionEnabled) {
        return new JwtService(privateKey, publicKey, ttlSeconds, encryptionEnabled);
    }

    @Bean
    public RedisClient redisClient(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password,
            @Value("${spring.data.redis.database:0}") int database) {
        return new RedisClient(host, port, password, database);
    }

    @Bean
    public TokenSessionService tokenSessionService(
            RedisClient redisClient,
            @Value("${app.session.ttl-seconds:900}") long ttlSeconds) {
        return new TokenSessionService(redisClient, ttlSeconds);
    }

    @Bean
    public PasswordHashService passwordHashService() {
        return new PasswordHashService();
    }

    @Bean
    public FilterRegistrationBean<RequestTraceFilter> requestTraceFilter() {
        FilterRegistrationBean<RequestTraceFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RequestTraceFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(0);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<ServletAuthFilter> servletAuthFilter(
            JwtService jwtService,
            TokenSessionService tokenSessionService,
            @Value("${app.security.internal-service-key:}") String internalServiceKey) {
        FilterRegistrationBean<ServletAuthFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ServletAuthFilter(jwtService, tokenSessionService, PUBLIC_PATHS, internalServiceKey));
        bean.addUrlPatterns("/*");
        bean.setOrder(1);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<OperationLogFilter> operationLogFilter(CommonLogMapper commonLogMapper) {
        FilterRegistrationBean<OperationLogFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new OperationLogFilter(commonLogMapper));
        bean.addUrlPatterns("/*");
        bean.setOrder(2);
        return bean;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PermissionInterceptor())
                .excludePathPatterns(
                        "/actuator/**",
                        "/error",
                        "/auth/login",
                        "/auth/captcha"
                );
    }
}
