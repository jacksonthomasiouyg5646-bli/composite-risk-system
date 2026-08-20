package com.example.usermanagement.gateway.apollo;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class ApolloStartupGuard {
    private static final int RETRY_TIMES = 3;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private ApolloStartupGuard() {
    }

    public static void require(String appId, String defaultNamespaces) {
        boolean enabled = readBoolean("APOLLO_ENABLED", "apollo.bootstrap.enabled", true);
        boolean required = readBoolean("APOLLO_REQUIRED", "apollo.required", false);
        if (required && !enabled) {
            throw new IllegalStateException("Apollo is required but APOLLO_ENABLED is false: appId=" + appId);
        }
        if (!enabled) {
            return;
        }
        String meta = readValue("APOLLO_META", "apollo.meta", "http://localhost:8080");
        String namespaces = readValue("APOLLO_NAMESPACES", "apollo.bootstrap.namespaces", defaultNamespaces);
        System.setProperty("app.id", appId);
        System.setProperty("apollo.meta", meta);
        System.setProperty("apollo.bootstrap.enabled", "true");
        System.setProperty("apollo.bootstrap.eagerLoad.enabled", "true");
        System.setProperty("apollo.bootstrap.namespaces", namespaces);
        if (!required) {
            return;
        }
        for (String namespace : namespaces.split(",")) {
            String trimmedNamespace = namespace.trim();
            if (!trimmedNamespace.isEmpty()) {
                verifyNamespace(appId, trimmedNamespace, meta);
            }
        }
    }

    private static void verifyNamespace(String appId, String namespace, String meta) {
        RuntimeException lastFailure = null;
        for (int i = 1; i <= RETRY_TIMES; i++) {
            try {
                fetchNamespace(appId, namespace, meta);
                return;
            } catch (RuntimeException ex) {
                lastFailure = ex;
                if (i < RETRY_TIMES) {
                    sleep();
                }
            }
        }
        throw new IllegalStateException("Apollo namespace is required but unavailable: appId=" + appId
                + ", namespace=" + namespace + ", meta=" + meta, lastFailure);
    }

    private static void fetchNamespace(String appId, String namespace, String meta) {
        String url = trimTrailingSlash(meta) + "/configs/" + encode(appId) + "/default/" + encode(namespace);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(REQUEST_TIMEOUT).GET().build();
        try {
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Apollo returned HTTP " + response.statusCode());
            }
            String compactBody = response.body().replaceAll("\\s+", "");
            if (!compactBody.contains("\"configurations\"") || compactBody.contains("\"configurations\":{}")) {
                throw new IllegalStateException("Apollo namespace has no published configurations");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot connect to Apollo", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Apollo check was interrupted", ex);
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Apollo check was interrupted", ex);
        }
    }

    private static boolean readBoolean(String envKey, String propertyKey, boolean defaultValue) {
        return Boolean.parseBoolean(readValue(envKey, propertyKey, Boolean.toString(defaultValue)));
    }

    private static String readValue(String envKey, String propertyKey, String defaultValue) {
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return defaultValue;
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
