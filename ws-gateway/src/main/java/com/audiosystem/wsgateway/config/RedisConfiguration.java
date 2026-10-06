package com.audiosystem.wsgateway.config;

import com.audiosystem.wsgateway.dto.JobResultMessageEnvelope;
import com.audiosystem.wsgateway.dto.JobStateItem;
import com.audiosystem.wsgateway.serializer.JobResultMessageEnvelopeSerializer;
import com.audiosystem.wsgateway.serializer.JobStateItemSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.annotation.EnableRedisListeners;
import org.springframework.data.redis.config.RedisListenerConfigurer;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisMessageConverters;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableRedisListeners
public class RedisConfiguration implements RedisListenerConfigurer {

    private final JobResultMessageEnvelopeSerializer jobResultMessageEnvelopeSerializer
        = new JobResultMessageEnvelopeSerializer();
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
            .commandTimeout(Duration.ofMillis(timeout))
            .build()
        );
    }

    @Bean("jobStatesTemplate")
    public RedisTemplate<String, JobStateItem> jobStatesTemplate() {
        final RedisTemplate<String, JobStateItem> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JobStateItemSerializer());
        return template;
    }

    @Bean(value = "queuePositionsTemplate", defaultCandidate = false)
    public RedisTemplate<String, String> queuePositionsTemplate() {
        final RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean(value = "receivedJobResultsTemplate", defaultCandidate = false)
    public RedisTemplate<String, JobResultMessageEnvelope> receivedJobResultsTemplate() {
        final RedisTemplate<String, JobResultMessageEnvelope> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jobResultMessageEnvelopeSerializer);
        return template;
    }

    @Override
    public void configureMessageConverters(RedisMessageConverters.Builder builder) {
        builder.addCustomConverter(jobResultMessageEnvelopeSerializer);
    }
}
