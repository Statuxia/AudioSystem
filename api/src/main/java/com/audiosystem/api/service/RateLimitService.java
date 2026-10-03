package com.audiosystem.api.service;

import com.audiosystem.api.annotations.RateLimit;
import com.audiosystem.api.dto.RateLimitKey;
import com.audiosystem.api.exception.RateLimitException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConfigurationBuilder;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Log4j2
public class RateLimitService {

    public static final String REDIS_PREFIX = "ratelimit:";
    @Lazy
    private final ProxyManager<byte[]> proxyManager;

    public Bucket resolveBucket(RateLimitKey key, RateLimit rateLimit) {
        byte[] keyBytes;
        try {
            keyBytes = (REDIS_PREFIX + key.toStringKey()).getBytes(StandardCharsets.UTF_8);
        } catch (NullPointerException e) {
            log.error("failed to get key from object: {}", key, e);
            throw new RateLimitException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to identify user.");
        }

        try {
            return proxyManager.getProxy(keyBytes, buildBucketConfiguration(rateLimit));
        } catch (Exception e) {
            log.error("failed to get proxy", e);
            throw new RateLimitException(HttpStatus.SERVICE_UNAVAILABLE, "Unavailable to process limits.");
        }
    }

    Supplier<BucketConfiguration> buildBucketConfiguration(RateLimit rateLimit) {
        final ConfigurationBuilder builder = BucketConfiguration.builder();
        builder.addLimit(Bandwidth.builder()
            .capacity(Math.max(rateLimit.requestsPerMinute(), 1))
            .refillIntervally(Math.max(rateLimit.requestsPerMinute(), 1), Duration.ofMinutes(1L))
            .build());

        if (rateLimit.requestsPerSecond() > 0) {
            builder.addLimit(Bandwidth.builder()
                .capacity(rateLimit.requestsPerSecond())
                .refillIntervally(rateLimit.requestsPerSecond(), Duration.ofSeconds(1L))
                .build()
            );
        }

        return builder::build;
    }
}
