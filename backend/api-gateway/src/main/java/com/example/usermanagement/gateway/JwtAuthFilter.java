package com.example.usermanagement.gateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Component
public class JwtAuthFilter implements WebFilter, Ordered {
    private final GatewayJwtService jwtService;
    private final GatewayTokenSessionService tokenSessionService;
    private final List<String> publicPaths = List.of(
            "/api/auth/login",
            "/api/auth/captcha",
            "/actuator"
    );

    public JwtAuthFilter(
            @Value("${app.jwt.rsa.public-key}") String publicKey,
            GatewayTokenSessionService tokenSessionService) {
        this.jwtService = new GatewayJwtService(publicKey);
        this.tokenSessionService = tokenSessionService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name()) || isPublic(path)) {
            return chain.filter(exchange);
        }
        if (!path.startsWith("/api/")) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(exchange, "missing token");
        }

        try {
            String token = header.substring(7);
            String tokenId = jwtService.tokenId(token);
            String username = jwtService.username(token);
            String signedToken = jwtService.verifyAndReturnSignedToken(token);
            return tokenSessionService.validateAndRefresh(tokenId)
                    .flatMap(valid -> {
                        if (!valid) {
                            return unauthorized(exchange, "session expired");
                        }
                        ServerWebExchange mutatedExchange = exchange.mutate()
                                .request(builder -> builder.headers(headers -> {
                                    headers.remove("X-Auth-Username");
                                    headers.remove("X-Internal-Service-Key");
                                    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + signedToken);
                                    headers.set("X-Auth-Username", username);
                                }))
                                .build();
                        return chain.filter(mutatedExchange);
                    });
        } catch (Exception ex) {
            return unauthorized(exchange, "invalid token");
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPublic(String path) {
        return publicPaths.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null,\"timestamp\":\"" + Instant.now() + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }
}
