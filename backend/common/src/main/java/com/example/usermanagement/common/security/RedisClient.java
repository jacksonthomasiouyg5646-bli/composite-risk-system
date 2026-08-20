package com.example.usermanagement.common.security;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RedisClient {
    private final String host;
    private final int port;
    private final String password;
    private final int database;

    public RedisClient(String host, int port, String password, int database) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.database = database;
    }

    public void setEx(String key, String value, long ttlSeconds) {
        command("SET", key, value, "EX", String.valueOf(ttlSeconds));
    }

    public String get(String key) {
        Object result = command("GET", key);
        return result instanceof String value ? value : null;
    }

    public void expire(String key, long ttlSeconds) {
        command("EXPIRE", key, String.valueOf(ttlSeconds));
    }

    public void delete(String key) {
        command("DEL", key);
    }

    private Object command(String... parts) {
        try (Socket socket = new Socket(host, port);
             BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
             BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream())) {
            if (password != null && !password.isBlank()) {
                writeCommand(output, "AUTH", password);
                readReply(input);
            }
            if (database > 0) {
                writeCommand(output, "SELECT", String.valueOf(database));
                readReply(input);
            }
            writeCommand(output, parts);
            return readReply(input);
        } catch (IOException ex) {
            throw new IllegalStateException("Redis request failed", ex);
        }
    }

    private void writeCommand(BufferedOutputStream output, String... parts) throws IOException {
        output.write(("*" + parts.length + "\r\n").getBytes(StandardCharsets.UTF_8));
        for (String part : parts) {
            byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
            output.write(("$" + bytes.length + "\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(bytes);
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        output.flush();
    }

    private Object readReply(BufferedInputStream input) throws IOException {
        int type = input.read();
        if (type == -1) {
            throw new IOException("Redis closed connection");
        }
        return switch ((char) type) {
            case '+' -> readLine(input);
            case '-' -> throw new IOException("Redis error: " + readLine(input));
            case ':' -> Long.parseLong(readLine(input));
            case '$' -> readBulkString(input);
            case '*' -> readArray(input);
            default -> throw new IOException("Unsupported Redis reply type: " + (char) type);
        };
    }

    private String readBulkString(BufferedInputStream input) throws IOException {
        int length = Integer.parseInt(readLine(input));
        if (length < 0) {
            return null;
        }
        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) {
            throw new IOException("Incomplete Redis bulk string");
        }
        input.read();
        input.read();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private List<Object> readArray(BufferedInputStream input) throws IOException {
        int length = Integer.parseInt(readLine(input));
        List<Object> items = new ArrayList<>(Math.max(length, 0));
        for (int i = 0; i < length; i++) {
            items.add(readReply(input));
        }
        return items;
    }

    private String readLine(BufferedInputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int previous = -1;
        int current;
        while ((current = input.read()) != -1) {
            if (previous == '\r' && current == '\n') {
                byte[] bytes = buffer.toByteArray();
                return new String(bytes, 0, bytes.length - 1, StandardCharsets.UTF_8);
            }
            buffer.write(current);
            previous = current;
        }
        throw new IOException("Redis reply line was not terminated");
    }
}
