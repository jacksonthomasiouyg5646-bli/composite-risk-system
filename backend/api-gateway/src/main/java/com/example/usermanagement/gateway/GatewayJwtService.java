package com.example.usermanagement.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.security.PublicKey;

final class GatewayJwtService {
    private final PublicKey publicKey;

    GatewayJwtService(String publicKey) {
        this.publicKey = GatewayRsaKeyUtils.parsePublicKey(publicKey);
    }

    String verifyAndReturnSignedToken(String token) {
        if (token == null || token.chars().filter(ch -> ch == '.').count() != 2) {
            throw new IllegalArgumentException("Only signed JWT tokens are accepted by the gateway.");
        }
        Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token);
        return token;
    }

    String tokenId(String token) {
        return parseClaims(token).getId();
    }

    String username(String token) {
        return parseClaims(token).getSubject();
    }

    private Claims parseClaims(String token) {
        verifyAndReturnSignedToken(token);
        return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();
    }
}
