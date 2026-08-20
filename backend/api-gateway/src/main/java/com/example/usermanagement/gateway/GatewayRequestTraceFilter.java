package com.example.usermanagement.gateway;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class GatewayRequestTraceFilter implements WebFilter, Ordered {
    private static final Logger log = LogManager.getLogger(GatewayRequestTraceFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String txId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
        if (txId == null || txId.isBlank()) {
            txId = UUID.randomUUID().toString().replace("-", "");
        }
        String path = exchange.getRequest().getURI().getPath();
        String query = exchange.getRequest().getURI().getRawQuery();
        long started = System.currentTimeMillis();
        String finalTxId = txId;
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-Request-Id", finalTxId)
                .build();
        ServerWebExchange tracedExchange = exchange.mutate().request(request).build();
        tracedExchange.getResponse().getHeaders().set("X-Request-Id", finalTxId);

        ThreadContext.put("txId", finalTxId);
        ThreadContext.put("requestUri", path);
        log.info("GATEWAY_TX_START txId={} threadId={} method={} uri={} query={}",
                finalTxId, Thread.currentThread().getId(), request.getMethod(), path, query == null ? "" : query);
        ThreadContext.clearMap();

        return chain.filter(tracedExchange)
                .doFinally(signalType -> {
                    ThreadContext.put("txId", finalTxId);
                    ThreadContext.put("requestUri", path);
                    long duration = System.currentTimeMillis() - started;
                    Integer status = tracedExchange.getResponse().getStatusCode() == null
                            ? null
                            : tracedExchange.getResponse().getStatusCode().value();
                    log.info("GATEWAY_TX_END txId={} threadId={} method={} uri={} status={} durationMs={} signal={}",
                            finalTxId, Thread.currentThread().getId(), request.getMethod(), path, status, duration, signalType);
                    ThreadContext.clearMap();
                });
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
