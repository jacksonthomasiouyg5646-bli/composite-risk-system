package com.example.usermanagement.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class RequestTraceFilter extends OncePerRequestFilter {
    public static final String TX_ID = "txId";
    public static final String REQUEST_URI = "requestUri";
    private static final Logger log = LogManager.getLogger(RequestTraceFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String txId = request.getHeader("X-Request-Id");
        if (txId == null || txId.isBlank()) {
            txId = UUID.randomUUID().toString().replace("-", "");
        }
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        long started = System.currentTimeMillis();
        ThreadContext.put(TX_ID, txId);
        ThreadContext.put(REQUEST_URI, uri);
        response.setHeader("X-Request-Id", txId);
        log.info("TX_START txId={} threadId={} method={} uri={} query={}",
                txId, Thread.currentThread().getId(), request.getMethod(), uri, query == null ? "" : query);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - started;
            log.info("TX_END txId={} threadId={} method={} uri={} status={} durationMs={}",
                    txId, Thread.currentThread().getId(), request.getMethod(), uri, response.getStatus(), duration);
            ThreadContext.remove(TX_ID);
            ThreadContext.remove(REQUEST_URI);
        }
    }
}
