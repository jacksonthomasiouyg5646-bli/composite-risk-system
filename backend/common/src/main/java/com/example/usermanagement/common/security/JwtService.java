package com.example.usermanagement.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JwtService {
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long ttlSeconds;
    private final boolean encryptionEnabled;

    public JwtService(String privateKey, String publicKey, long ttlSeconds, boolean encryptionEnabled) {
        this.privateKey = privateKey == null || privateKey.isBlank() ? null : RsaKeyUtils.parsePrivateKey(privateKey);
        this.publicKey = RsaKeyUtils.parsePublicKey(publicKey);
        this.ttlSeconds = ttlSeconds;
        this.encryptionEnabled = encryptionEnabled;
    }

    public String createToken(Long userId, String username, List<String> roles, List<String> permissions, Long tenantId) {
        return createToken(UUID.randomUUID().toString(), userId, username, roles, permissions, tenantId);
    }

    public String createToken(String jti, Long userId, String username, List<String> roles, List<String> permissions, Long tenantId) {
        if (privateKey == null) {
            throw new IllegalStateException("RSA private key is required to create JWT tokens.");
        }
        Instant now = Instant.now();
        String signedToken = Jwts.builder()
                .id(jti)
                .subject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("tenantId", tenantId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
        if (!encryptionEnabled) {
            return signedToken;
        }
        return Jwts.builder()
                .content(signedToken, "JWT")
                .encryptWith(publicKey, Jwts.KEY.RSA_OAEP_256, Jwts.ENC.A256GCM)
                .compact();
    }

    public AuthContext parse(String token) {
        Claims claims = parseClaims(token);
        Long userId = asLong(claims.get("userId"));
        Long tenantId = asLong(claims.get("tenantId"));
        List<String> roles = asStringList(claims.get("roles"));
        List<String> permissions = asStringList(claims.get("permissions"));
        return new AuthContext(userId, claims.getSubject(), roles, permissions, tenantId);
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    public Map<String, Object> claims(String token) {
        Claims claims = parseClaims(token);
        return Map.copyOf(claims);
    }

    public String tokenId(String token) {
        return parseClaims(token).getId();
    }

    private Claims parseClaims(String token) {
        String signedToken = isEncryptedToken(token)
                ? decryptToSignedToken(token)
                : token;
        return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(signedToken).getPayload();
    }

    private String decryptToSignedToken(String token) {
        if (privateKey == null) {
            throw new IllegalStateException("RSA private key is required to decrypt JWT tokens.");
        }
        return new String(Jwts.parser()
                .decryptWith(privateKey)
                .build()
                .parseEncryptedContent(token)
                .getPayload(), StandardCharsets.UTF_8);
    }

    private boolean isEncryptedToken(String token) {
        return token != null && token.chars().filter(ch -> ch == '.').count() == 4;
    }
}
