import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DockerFrontendServer {
    private static final Path ROOT = Path.of("/app");
    private static final String GATEWAY_HOST = System.getenv().getOrDefault("GATEWAY_HOST", "api-gateway");
    private static final String GATEWAY_PORT = System.getenv().getOrDefault("GATEWAY_PORT", "8088");
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "host",
            "content-length"
    );

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
        server.createContext("/", DockerFrontendServer::handle);
        server.start();
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String rawPath = exchange.getRequestURI().getRawPath();
        if (rawPath.startsWith("/api/")) {
            proxy(exchange);
            return;
        }
        Path file = ROOT.resolve(rawPath.substring(1)).normalize();
        if (!file.startsWith(ROOT) || !Files.exists(file) || Files.isDirectory(file)) {
            file = ROOT.resolve("index.html");
        }
        byte[] body = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", contentType(file));
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static void proxy(HttpExchange exchange) throws IOException {
        URI requestUri = exchange.getRequestURI();
        URL url = URI.create("http://" + GATEWAY_HOST + ":" + GATEWAY_PORT + requestUri).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod(exchange.getRequestMethod());
        for (Map.Entry<String, List<String>> header : exchange.getRequestHeaders().entrySet()) {
            if (isHopByHopHeader(header.getKey())) {
                continue;
            }
            for (String value : header.getValue()) {
                connection.addRequestProperty(header.getKey(), value);
            }
        }
        if (exchange.getRequestBody() != null && allowsBody(exchange.getRequestMethod())) {
            connection.setDoOutput(true);
            exchange.getRequestBody().transferTo(connection.getOutputStream());
        }
        int status = connection.getResponseCode();
        connection.getHeaderFields().forEach((key, values) -> {
            if (key != null && values != null && !isHopByHopHeader(key)) {
                exchange.getResponseHeaders().put(key, values);
            }
        });
        String responseContentType = connection.getContentType();
        if (responseContentType != null && responseContentType.toLowerCase().startsWith("application/json")) {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        }
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        byte[] body = stream == null ? new byte[0] : stream.readAllBytes();
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static boolean isHopByHopHeader(String name) {
        return name != null && HOP_BY_HOP_HEADERS.contains(name.toLowerCase());
    }

    private static boolean allowsBody(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
