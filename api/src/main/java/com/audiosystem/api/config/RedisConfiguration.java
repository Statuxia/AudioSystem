package com.audiosystem.api.config;

import com.audiosystem.api.dto.JobStateItem;
import com.audiosystem.api.serializer.JobStateItemSerializer;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfiguration {

    @Value("${spring.data.redis.host}")
    private String hostName;
    @Value("${spring.data.redis.port}")
    private Integer port;
    @Value("${spring.data.redis.timeout}")
    private Long timeout;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        final RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(hostName, port);
        return new LettuceConnectionFactory(
            configuration, LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofMillis(timeout)).build()
        );
    }

    @Bean
    public RedisTemplate<String, JobStateItem> redisTemplate() {
        final RedisTemplate<String, JobStateItem> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JobStateItemSerializer());
        template.setConnectionFactory(redisConnectionFactory());
        return template;
    }

    @Bean(value = "redisRateLimitClient", defaultCandidate = false)
    public RedisClient redisRateLimitClient() {
        final RedisURI redisURI = RedisURI.create(hostName, port);
        redisURI.setTimeout(Duration.ofMillis(100));
        return RedisClient.create(redisURI);
    }

    @Bean
    @Lazy
    public ProxyManager<byte[]> bucket4jLettuce() {
        return Bucket4jLettuce.casBasedBuilder(redisRateLimitClient())
            .maxRetries(5)
            .requestTimeout(Duration.ofMillis(100))
            .expirationAfterWrite(ExpirationAfterWriteStrategy
                .basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(5)))
            .build();
    }
}
