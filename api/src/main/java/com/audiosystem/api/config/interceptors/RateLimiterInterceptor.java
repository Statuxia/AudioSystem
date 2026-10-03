package com.audiosystem.api.config.interceptors;

import com.audiosystem.api.annotations.RateLimit;
import com.audiosystem.api.dto.RateLimitKey;
import com.audiosystem.api.exception.RateLimitException;
import com.audiosystem.api.service.RateLimitService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@RequiredArgsConstructor
@Component
@Log4j2
public class RateLimiterInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            final RateLimit annotation = handlerMethod.getMethod().getAnnotation(RateLimit.class);
            if (annotation != null) {
                final String identifier = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                final String clientAddr = request.getRemoteAddr();

                final RateLimitKey key = new RateLimitKey(identifier, clientAddr);
                final Bucket bucket = rateLimitService.resolveBucket(key, annotation);

                final boolean consumed;
                try {
                    consumed = bucket.tryConsume(1L);
                } catch (Exception e) {
                    log.error("failed to consume", e);
                    throw new RateLimitException(HttpStatus.SERVICE_UNAVAILABLE, "Unavailable to process limits.");
                }

                if (!consumed) {
                    throw new RateLimitException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Try again later.");
                }
            }
        }
        return true;
    }
}
