import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class LogViewerServer {
    private static final Path LOG_ROOT = Path.of("/logs");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", LogViewerServer::handle);
        server.start();
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.startsWith("/log/")) {
            renderLog(exchange, path.substring("/log/".length()));
            return;
        }
        renderIndex(exchange);
    }

    private static void renderIndex(HttpExchange exchange) throws IOException {
        StringBuilder body = new StringBuilder();
        body.append("<!doctype html><html><head><meta charset=\"utf-8\"><title>Risk Logs</title>");
        body.append("<style>body{font-family:Segoe UI,Arial,sans-serif;margin:24px;background:#f6f8fb;color:#172033}");
        body.append("a{display:block;padding:10px 12px;margin:8px 0;background:white;border:1px solid #dfe5ef;text-decoration:none;color:#1f5fbf;border-radius:6px}");
        body.append("</style></head><body><h2>风险管理系统日志</h2>");
        if (Files.exists(LOG_ROOT)) {
            try (var stream = Files.list(LOG_ROOT)) {
                List<Path> files = stream
                        .filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().endsWith(".log"))
                        .sorted(Comparator.comparing(file -> file.getFileName().toString()))
                        .toList();
                for (Path file : files) {
                    String name = file.getFileName().toString();
                    body.append("<a href=\"/log/").append(urlEncode(name)).append("\">").append(escape(name)).append("</a>");
                }
                if (files.isEmpty()) {
                    body.append("<p>暂无日志文件，请先启动后端服务。</p>");
                }
            }
        } else {
            body.append("<p>日志目录不存在。</p>");
        }
        body.append("</body></html>");
        send(exchange, "text/html; charset=utf-8", body.toString());
    }

    private static void renderLog(HttpExchange exchange, String encodedName) throws IOException {
        String name = URLDecoder.decode(encodedName, StandardCharsets.UTF_8);
        Path file = LOG_ROOT.resolve(name).normalize();
        if (!file.startsWith(LOG_ROOT) || !Files.exists(file) || !Files.isRegularFile(file)) {
            send(exchange, 404, "text/plain; charset=utf-8", "日志文件不存在");
            return;
        }
        byte[] bytes = Files.readAllBytes(file);
        int start = Math.max(0, bytes.length - 200_000);
        String content = new String(bytes, start, bytes.length - start, StandardCharsets.UTF_8);
        String html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>" + escape(name) + "</title>"
                + "<style>body{font-family:Consolas,monospace;margin:0;background:#0f172a;color:#dbeafe}"
                + "header{position:sticky;top:0;background:#111827;padding:12px 16px}"
                + "a{color:#93c5fd}pre{white-space:pre-wrap;margin:0;padding:16px;line-height:1.45}</style>"
                + "</head><body><header><a href=\"/\">返回日志列表</a> | " + escape(name)
                + " | <a href=\"/log/" + urlEncode(name) + "\">刷新</a></header><pre>"
                + escape(content) + "</pre></body></html>";
        send(exchange, "text/html; charset=utf-8", html);
    }

    private static void send(HttpExchange exchange, String contentType, String body) throws IOException {
        send(exchange, 200, contentType, body);
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
