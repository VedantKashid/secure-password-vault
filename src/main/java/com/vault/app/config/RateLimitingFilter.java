package com.vault.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // =========================================================
    // ⚙️ ADJUST THESE VALUES TO FIX YOUR RATE LIMITING
    // =========================================================
    private static final int MAX_REQUESTS = 100; // Increased threshold
    private static final long TIME_WINDOW_MS = 60000; // 60 seconds (1 minute)
    // =========================================================

    // Simple in-memory tracker: IP Address -> Client Data
    private final Map<String, ClientInfo> clients = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();
        long currentTime = System.currentTimeMillis();

        clients.putIfAbsent(clientIp, new ClientInfo(currentTime, 0));
        ClientInfo clientInfo = clients.get(clientIp);

        synchronized (clientInfo) {
            // If the time window has passed, reset the counter for this IP
            if (currentTime - clientInfo.startTime > TIME_WINDOW_MS) {
                clientInfo.startTime = currentTime;
                clientInfo.requestCount = 0;
            }

            clientInfo.requestCount++;

            // If they exceed the max requests, block them with a 429 status
            if (clientInfo.requestCount > MAX_REQUESTS) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"message\": \"Too many requests. Please wait a moment.\", \"data\": null}");
                return; // Halt the request
            }
        }

        // Allow the request to proceed to the controller
        filterChain.doFilter(request, response);
    }

    // Helper class to store timestamp and request count
    private static class ClientInfo {
        long startTime;
        int requestCount;

        ClientInfo(long startTime, int requestCount) {
            this.startTime = startTime;
            this.requestCount = requestCount;
        }
    }
}