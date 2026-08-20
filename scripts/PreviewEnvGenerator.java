import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.security.crypto.bcrypt.BCrypt;

/** Generates an ignored, local-only Docker preview environment file. */
public final class PreviewEnvGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64 = Base64.getEncoder();
    private static final Base64.Encoder URL_BASE64 = Base64.getUrlEncoder().withoutPadding();

    private PreviewEnvGenerator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected output .env path");
        }

        String adminPassword = "Preview@" + randomSecret(18);
        String adminHash = BCrypt.hashpw(adminPassword, BCrypt.gensalt(12, RANDOM));

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, RANDOM);
        KeyPair jwtKeys = generator.generateKeyPair();

        String env = String.join(System.lineSeparator(),
                "COMPOSE_PROJECT_NAME=risk-p0-preview",
                "CONTAINER_PREFIX=risk-p0-preview",
                "PUBLIC_BIND_ADDRESS=127.0.0.1",
                "GATEWAY_PORT=8088",
                "MYSQL_ROOT_PASSWORD=" + randomSecret(32),
                "BOOTSTRAP_ADMIN_PASSWORD_HASH='" + adminHash + "'",
                "REDIS_PASSWORD=" + randomSecret(32),
                "GRAFANA_ADMIN_USER=admin",
                "GRAFANA_ADMIN_PASSWORD=" + randomSecret(24),
                "JWT_RSA_PRIVATE_KEY=" + BASE64.encodeToString(jwtKeys.getPrivate().getEncoded()),
                "JWT_RSA_PUBLIC_KEY=" + BASE64.encodeToString(jwtKeys.getPublic().getEncoded()),
                "INTERNAL_SERVICE_KEY=" + randomSecret(48),
                "APOLLO_ENABLED=true",
                "APOLLO_REQUIRED=true",
                "APOLLO_META=http://host.docker.internal:8080",
                "PREVIEW_ADMIN_USERNAME=admin",
                "PREVIEW_ADMIN_PASSWORD=" + adminPassword,
                "");

        Path output = Path.of(args[0]).toAbsolutePath().normalize();
        Files.writeString(output, env, StandardCharsets.UTF_8);
        System.out.println("Created local Docker preview environment: " + output);
    }

    private static String randomSecret(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return URL_BASE64.encodeToString(value);
    }
}
