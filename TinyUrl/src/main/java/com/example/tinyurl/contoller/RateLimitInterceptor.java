package com.example.tinyurl.contoller;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.tinyurl.exceptions.RateLimitExceededException;
import com.example.tinyurl.service.RateLimiterService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimiterService rateLimiterService;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler)
            throws RateLimitExceededException {
        String clientIp = getClientIp(req);

        boolean allowed = rateLimiterService.tryConsume(clientIp);
        if (!allowed) {
            throw new RateLimitExceededException("limit of requests exceeded, try again later");
        }
        return true;
    }

    private String getClientIp(HttpServletRequest req) {
        String xfHeader = req.getHeader("X-Forwardd-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return req.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

}
