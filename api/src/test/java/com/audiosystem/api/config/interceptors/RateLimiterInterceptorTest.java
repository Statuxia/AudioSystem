package com.audiosystem.api.config.interceptors;

import com.audiosystem.api.annotations.RateLimit;
import com.audiosystem.api.exception.RateLimitException;
import com.audiosystem.api.service.RateLimitService;
import io.github.bucket4j.distributed.proxy.DefaultBucketProxy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterInterceptorTest {

    @InjectMocks
    private RateLimiterInterceptor rateLimiterInterceptor;

    @Mock
    private RateLimitService rateLimitService;

    @Test
    void testPreHandleValid() {
        final HttpServletRequest request = Mockito.mock();
        final HttpServletResponse response = Mockito.mock();
        final HandlerMethod handlerMethod = Mockito.mock();
        final Method method = Mockito.mock(Method.class);
        final DefaultBucketProxy bucket = Mockito.mock(DefaultBucketProxy.class);

        BDDMockito.doReturn(method).when(handlerMethod).getMethod();
        BDDMockito.doReturn(Mockito.mock(RateLimit.class)).when(method).getAnnotation(eq(RateLimit.class));
        BDDMockito.doReturn(bucket).when(rateLimitService).resolveBucket(any(), any());
        BDDMockito.doReturn(true).when(bucket).tryConsume(any(long.class));

        assertDoesNotThrow(() -> rateLimiterInterceptor.preHandle(request, response, handlerMethod));
        verify(handlerMethod).getMethod();
        verify(method).getAnnotation(RateLimit.class);
        verify(request).getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        verify(request).getRemoteAddr();
        verify(rateLimitService).resolveBucket(any(), any());
        verify(bucket).tryConsume(any(long.class));
    }


    @Test
    void testPreHandleTooManyRequest() {
        final HttpServletRequest request = Mockito.mock();
        final HttpServletResponse response = Mockito.mock();
        final HandlerMethod handlerMethod = Mockito.mock();
        final Method method = Mockito.mock(Method.class);
        final DefaultBucketProxy bucket = Mockito.mock(DefaultBucketProxy.class);

        BDDMockito.doReturn(method).when(handlerMethod).getMethod();
        BDDMockito.doReturn(Mockito.mock(RateLimit.class)).when(method).getAnnotation(eq(RateLimit.class));
        BDDMockito.doReturn(bucket).when(rateLimitService).resolveBucket(any(), any());
        BDDMockito.doReturn(false).when(bucket).tryConsume(any(long.class));

        final RateLimitException rateLimitException
            = assertThrows(RateLimitException.class, () -> rateLimiterInterceptor.preHandle(request, response, handlerMethod));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, rateLimitException.getStatus());

        verify(handlerMethod).getMethod();
        verify(method).getAnnotation(RateLimit.class);
        verify(request).getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        verify(request).getRemoteAddr();
        verify(rateLimitService).resolveBucket(any(), any());
        verify(bucket).tryConsume(any(long.class));
    }

    @Test
    void testPreHandleNoHandlerMethod() {
        final HttpServletRequest request = Mockito.mock();
        final HttpServletResponse response = Mockito.mock();

        assertDoesNotThrow(() -> rateLimiterInterceptor.preHandle(request, response, new Object()));
        verify(request, never()).getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        verify(request, never()).getRemoteAddr();
        verify(rateLimitService, never()).resolveBucket(any(), any());
    }

    @Test
    void testPreHandleNoRateLimitAnnotation() {
        final HttpServletRequest request = Mockito.mock();
        final HttpServletResponse response = Mockito.mock();
        final HandlerMethod handlerMethod = Mockito.mock();
        final Method method = Mockito.mock(Method.class);

        BDDMockito.doReturn(method).when(handlerMethod).getMethod();

        assertDoesNotThrow(() -> rateLimiterInterceptor.preHandle(request, response, handlerMethod));
        verify(handlerMethod).getMethod();
        verify(method).getAnnotation(RateLimit.class);
        verify(request, never()).getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        verify(request, never()).getRemoteAddr();
        verify(rateLimitService, never()).resolveBucket(any(), any());
    }

    /**
     * Возможно при двух сценариях:
     * 1. Ошибка при составлении строкового ключа через RateLimitKey#toStringKey
     * 2. Ошибка при получении BucketProxy, например из-за инициализации бина, приведший к провалу коннекта к redis
     */
    @Test
    void testPreHandleRateLimitException() {
        final HttpServletRequest request = Mockito.mock();
        final HttpServletResponse response = Mockito.mock();
        final HandlerMethod handlerMethod = Mockito.mock();
        final Method method = Mockito.mock(Method.class);

        BDDMockito.doReturn(method).when(handlerMethod).getMethod();
        BDDMockito.doReturn(Mockito.mock(RateLimit.class)).when(method).getAnnotation(eq(RateLimit.class));
        BDDMockito.doThrow(
                new RateLimitException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to identify user."),
                new RateLimitException(HttpStatus.SERVICE_UNAVAILABLE, "Unavailable to process limits.")
            )
            .when(rateLimitService).resolveBucket(any(), any());


        RateLimitException rateLimitException
            = assertThrows(RateLimitException.class, () -> rateLimiterInterceptor.preHandle(request, response, handlerMethod));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, rateLimitException.getStatus());
        rateLimitException
            = assertThrows(RateLimitException.class, () -> rateLimiterInterceptor.preHandle(request, response, handlerMethod));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, rateLimitException.getStatus());

        verify(handlerMethod, times(2)).getMethod();
        verify(method, times(2)).getAnnotation(RateLimit.class);
        verify(request, times(2)).getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        verify(request, times(2)).getRemoteAddr();
        verify(rateLimitService, times(2)).resolveBucket(any(), any());
    }

    @Test
    void testPreHandleConsumeException() {
        final HttpServletRequest request = Mockito.mock();
        final HttpServletResponse response = Mockito.mock();
        final HandlerMethod handlerMethod = Mockito.mock();
        final Method method = Mockito.mock(Method.class);
        final DefaultBucketProxy bucket = Mockito.mock(DefaultBucketProxy.class);

        BDDMockito.doReturn(method).when(handlerMethod).getMethod();
        BDDMockito.doReturn(Mockito.mock(RateLimit.class)).when(method).getAnnotation(eq(RateLimit.class));
        BDDMockito.doReturn(bucket).when(rateLimitService).resolveBucket(any(), any());
        BDDMockito.doThrow(new RuntimeException()).when(bucket).tryConsume(any(long.class));

        final RateLimitException rateLimitException
            = assertThrows(RateLimitException.class, () -> rateLimiterInterceptor.preHandle(request, response, handlerMethod));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, rateLimitException.getStatus());

        verify(handlerMethod).getMethod();
        verify(method).getAnnotation(RateLimit.class);
        verify(request).getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        verify(request).getRemoteAddr();
        verify(rateLimitService).resolveBucket(any(), any());
        verify(bucket).tryConsume(any(long.class));
    }
}