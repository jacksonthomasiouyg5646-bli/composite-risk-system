package com.example.usermanagement.common.security;

import java.time.Duration;
import java.util.Objects;

public class TokenSessionService {
    private static final String KEY_PREFIX = "risk:session:";

    private final RedisClient redisClient;
    private final Duration ttl;
    public TokenSessionService(RedisClient redisClient, long ttlSeconds) {
        this.redisClient = redisClient;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public void create(String tokenId, Long userId, String username) {
        String value = userId + "|" + username + "|" + java.time.Instant.now();
        redisClient.setEx(key(tokenId), value, ttl.toSeconds());
    }

    public boolean validateAndRefresh(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }
        String key = key(tokenId);
        String value = redisClient.get(key);
        if (value == null) {
            return false;
        }
        redisClient.expire(key, ttl.toSeconds());
        return true;
    }

    public void revoke(String tokenId) {
        if (tokenId != null && !tokenId.isBlank()) {
            redisClient.delete(key(tokenId));
        }
    }

    public long ttlSeconds() {
        return ttl.toSeconds();
    }

    private String key(String tokenId) {
        return KEY_PREFIX + Objects.requireNonNull(tokenId, "tokenId");
    }

}
