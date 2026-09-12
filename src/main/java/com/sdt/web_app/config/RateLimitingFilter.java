package com.sdt.web_app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L; // 15 minutes in milliseconds
    private static Boolean enabledForTesting = null;

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    @Value("${app.security.rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;

    public static void setEnabledForTesting(Boolean enabled) {
        enabledForTesting = enabled;
    }

    public void clear() {
        requestCounts.clear();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (enabledForTesting != null) {
            if (!enabledForTesting) return true;
        } else if (!rateLimitingEnabled) {
            return true;
        }

        String path = request.getRequestURI();
        return !(path.equals("/api/public/auth/login") ||
                 path.equals("/api/public/auth/register") ||
                 path.equals("/api/public/auth/forgot-password"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIP(request);
        String key = clientIp + ":" + request.getRequestURI();
        long now = Instant.now().toEpochMilli();

        RequestCounter counter = requestCounts.compute(key, (k, existing) -> {
            if (existing == null || (now - existing.windowStart) > WINDOW_MS) {
                return new RequestCounter(now, 1);
            } else {
                existing.count++;
                return existing;
            }
        });

        if (counter.count > MAX_REQUESTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "900");
            response.getWriter().write("""
                {
                    "status": 429,
                    "error": "Too Many Requests",
                    "message": "Rate limit exceeded. Maximum 5 attempts per 15 minutes allowed. Please try again later."
                }
                """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RequestCounter {
        final long windowStart;
        int count;

        RequestCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
