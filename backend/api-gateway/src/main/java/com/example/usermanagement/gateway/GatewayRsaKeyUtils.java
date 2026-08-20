package com.example.usermanagement.gateway;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class GatewayRsaKeyUtils {
    private GatewayRsaKeyUtils() {
    }

    static PublicKey parsePublicKey(String value) {
        byte[] bytes = decodeKey(value, "PUBLIC KEY");
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid RSA public key. Use X.509 PEM or base64 DER.", ex);
        }
    }

    private static byte[] decodeKey(String value, String marker) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing RSA " + marker.toLowerCase());
        }
        String normalized = value.trim()
                .replace("\\n", "\n")
                .replace("-----BEGIN " + marker + "-----", "")
                .replace("-----END " + marker + "-----", "")
                .replaceAll("\\s", "");
        try {
            return Base64.getDecoder().decode(normalized.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("RSA " + marker.toLowerCase() + " must be PEM or base64 DER.", ex);
        }
    }
}
