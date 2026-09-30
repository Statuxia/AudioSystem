package com.audiosystem.api.config.interceptors;

import com.audiosystem.api.annotations.RateLimit;
import com.audiosystem.api.dto.ApiResponse;
import com.audiosystem.api.dto.RateLimitKey;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.local.LocalBucketBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class RateLimiterInterceptor implements HandlerInterceptor {

    private final Map<RateLimitKey, Bucket> cache = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

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
                final Bucket bucket = cache.computeIfAbsent(
                    new RateLimitKey(identifier, clientAddr),
                    key -> buildBucket(annotation)
                );

                if (!bucket.tryConsume(1L)) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setCharacterEncoding(StandardCharsets.UTF_8);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(mapper.writeValueAsString(
                        new ApiResponse("Too many requests. Try again later.")
                    ));
                    response.getWriter().flush();

                    return false;
                }
            }
        }
        return true;
    }

    private Bucket buildBucket(RateLimit rateLimit) {
        final LocalBucketBuilder bucketBuilder = Bucket.builder();

        bucketBuilder.addLimit(Bandwidth.builder()
            .capacity(rateLimit.requestsPerMinute())
            .refillIntervally(rateLimit.requestsPerMinute(), Duration.ofMinutes(1L))
            .build());

        if (rateLimit.requestsPerSecond() > 0) {
            bucketBuilder.addLimit(Bandwidth.builder()
                .capacity(rateLimit.requestsPerSecond())
                .refillIntervally(rateLimit.requestsPerSecond(), Duration.ofSeconds(1L))
                .build()
            );
        }

        return bucketBuilder.build();
    }
}
