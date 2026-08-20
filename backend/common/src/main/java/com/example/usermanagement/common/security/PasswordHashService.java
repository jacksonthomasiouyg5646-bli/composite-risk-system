package com.example.usermanagement.common.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class PasswordHashService {
    private static final int BCRYPT_STRENGTH = 12;
    private static final int LEGACY_SHA256_LENGTH = 64;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);

    public String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("password must not be blank");
        }
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        if (encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$")) {
            return encoder.matches(rawPassword, encodedPassword);
        }
        if (encodedPassword.length() == LEGACY_SHA256_LENGTH && encodedPassword.matches("[0-9a-fA-F]+")) {
            return MessageDigest.isEqual(
                    legacySha256(rawPassword).getBytes(StandardCharsets.US_ASCII),
                    encodedPassword.toLowerCase().getBytes(StandardCharsets.US_ASCII));
        }
        return false;
    }

    public boolean needsUpgrade(String encodedPassword) {
        return encodedPassword == null || !encodedPassword.startsWith("$2");
    }

    private String legacySha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
