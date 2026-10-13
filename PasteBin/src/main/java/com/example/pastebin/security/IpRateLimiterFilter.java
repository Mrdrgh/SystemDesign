package com.example.pastebin.security;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.pastebin.Exception.RateLimitExceededException;
import com.example.pastebin.Service.RateLimitingService;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IpRateLimiterFilter extends OncePerRequestFilter {
    private final RateLimitingService rateLimitingService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String identifier = extractClientIp(request);
        ConsumptionProbe cp = rateLimitingService.consumeToken(identifier);
        if (!cp.isConsumed()) {
            // rate limited
            long waitSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(cp.getNanosToWaitForRefill()));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(waitSeconds));
            response.getWriter().write(String.format(
                    "{\"status\": 429, \"message\": \"Too many requests, try again in %d seconds\"}",
                    waitSeconds));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest req) {
        String xForwardedFor = req.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim(); // client ip, proxy 1, proxy2 -> client ip
        }
        return req.getRemoteAddr();
    }
}
