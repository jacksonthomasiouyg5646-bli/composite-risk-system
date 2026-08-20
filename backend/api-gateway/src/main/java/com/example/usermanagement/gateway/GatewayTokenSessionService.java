package com.example.usermanagement.gateway;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Objects;

@Service
public class GatewayTokenSessionService {
    private static final String KEY_PREFIX = "risk:session:";

    private final GatewayRedisClient redisClient;
    private final Duration ttl;
    public GatewayTokenSessionService(Environment environment) {
        long ttlSeconds = environment.getProperty("app.session.ttl-seconds", Long.class, 900L);
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.redisClient = new GatewayRedisClient(
                environment.getProperty("spring.data.redis.host", "localhost"),
                environment.getProperty("spring.data.redis.port", Integer.class, 6379),
                environment.getProperty("spring.data.redis.password", ""),
                environment.getProperty("spring.data.redis.database", Integer.class, 0));
    }

    public Mono<Boolean> validateAndRefresh(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return Mono.just(false);
        }
        String key = key(tokenId);
        return Mono.fromCallable(() -> {
            String value = redisClient.get(key);
            if (value == null) {
                return false;
            }
            redisClient.expire(key, ttl.toSeconds());
            return true;
        }).onErrorReturn(false).subscribeOn(Schedulers.boundedElastic());
    }

    private String key(String tokenId) {
        return KEY_PREFIX + Objects.requireNonNull(tokenId, "tokenId");
    }

}
