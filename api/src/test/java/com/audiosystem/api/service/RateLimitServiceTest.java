package com.audiosystem.api.service;

import com.audiosystem.api.annotations.RateLimit;
import com.audiosystem.api.dto.RateLimitKey;
import com.audiosystem.api.exception.RateLimitException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.DefaultBucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @InjectMocks
    private RateLimitService rateLimitService;

    @Mock
    private ProxyManager<byte[]> proxyManager;

    @Captor
    private ArgumentCaptor<byte[]> keyCaptor;

    @Test
    void testResolveBucketValid() {
        final RateLimit rateLimit = getRateLimit(30L, 3L);

        BDDMockito.doReturn(Mockito.mock(DefaultBucketProxy.class)).when(proxyManager).getProxy(any(), any());

        assertDoesNotThrow(
            () -> rateLimitService.resolveBucket(new RateLimitKey("source", "id"), rateLimit)
        );
        verify(proxyManager).getProxy(keyCaptor.capture(), any());
        final byte[] key = keyCaptor.getValue();
        assertEquals(RateLimitService.REDIS_PREFIX + "source:id", new String(key, StandardCharsets.UTF_8));
    }

    @Test
    void testResolveBucketBadKey() {
        final RateLimitException rateLimitException = assertThrows(
            RateLimitException.class,
            () -> rateLimitService.resolveBucket(new RateLimitKey(null, null), Mockito.mock(RateLimit.class))
        );
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, rateLimitException.getStatus());
    }

    @Test
    void testResolveBucketProxyManagerInitializationFailed() {
        final RateLimit rateLimit = getRateLimit(30L, 3L);

        BDDMockito.doThrow(BeanCreationException.class).when(proxyManager).getProxy(any(), any()); // any exception on proxyManager call

        final RateLimitException rateLimitException = assertThrows(
            RateLimitException.class,
            () -> rateLimitService.resolveBucket(new RateLimitKey("source", "id"), rateLimit)
        );
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, rateLimitException.getStatus());
    }

    @Test
    void testBuildBucketConfigurationAllArgs() {
        final RateLimit rateLimit = getRateLimit(30L, 3L);

        final Supplier<BucketConfiguration> supplier = rateLimitService.buildBucketConfiguration(rateLimit);
        final BucketConfiguration configuration = supplier.get();
        final Bandwidth[] bandwidths = configuration.getBandwidths();

        assertEquals(2, bandwidths.length);
        assertEquals(30, bandwidths[0].getCapacity());
        assertEquals(30, bandwidths[0].getInitialTokens());
        assertEquals(Duration.ofSeconds(60).toNanos(), bandwidths[0].getRefillPeriodNanos());
        assertEquals(3, bandwidths[1].getCapacity());
        assertEquals(3, bandwidths[1].getInitialTokens());
        assertEquals(Duration.ofSeconds(1).toNanos(), bandwidths[1].getRefillPeriodNanos());
    }

    @Test
    void testBuildBucketConfigurationOnlyMinutes() {
        final RateLimit rateLimit = getRateLimit(20L, 0L);

        final Supplier<BucketConfiguration> supplier = rateLimitService.buildBucketConfiguration(rateLimit);
        final BucketConfiguration configuration = supplier.get();
        final Bandwidth[] bandwidths = configuration.getBandwidths();

        assertEquals(1, bandwidths.length);
        assertEquals(20, bandwidths[0].getCapacity());
        assertEquals(20, bandwidths[0].getInitialTokens());
        assertEquals(Duration.ofSeconds(60).toNanos(), bandwidths[0].getRefillPeriodNanos());
    }

    @Test
    void testBuildBucketConfigurationLessThanMinArgs() {
        final RateLimit rateLimit = getRateLimit(0L, -1L);

        final Supplier<BucketConfiguration> supplier = rateLimitService.buildBucketConfiguration(rateLimit);
        final BucketConfiguration configuration = supplier.get();
        final Bandwidth[] bandwidths = configuration.getBandwidths();

        assertEquals(1, bandwidths.length);
        assertEquals(1, bandwidths[0].getCapacity());
        assertEquals(1, bandwidths[0].getInitialTokens());
        assertEquals(Duration.ofSeconds(60).toNanos(), bandwidths[0].getRefillPeriodNanos());
    }

    private RateLimit getRateLimit(long rpm, long rps) {
        RateLimit rateLimit = Mockito.mock();

        BDDMockito.doReturn(rpm).when(rateLimit).requestsPerMinute();
        BDDMockito.doReturn(rps).when(rateLimit).requestsPerSecond();
        return rateLimit;
    }
}