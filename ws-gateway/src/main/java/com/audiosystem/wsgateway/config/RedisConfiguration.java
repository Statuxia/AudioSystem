package com.audiosystem.wsgateway.config;

import com.audiosystem.wsgateway.dto.JobResultMessageEnvelope;
import com.audiosystem.wsgateway.dto.JobStateItem;
import com.audiosystem.wsgateway.serializer.JobResultMessageEnvelopeSerializer;
import com.audiosystem.wsgateway.serializer.JobStateItemSerializer;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.data.redis.autoconfigure.RedisMessageListenerContainerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.annotation.EnableRedisListeners;
import org.springframework.data.redis.config.RedisListenerConfigurer;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisMessageConverters;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@Log4j2
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

    @Bean(defaultCandidate = false)
    public ThreadPoolExecutor redisTaskExecutor() {
        return new ThreadPoolExecutor(
            4,
            8,
            10,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            Thread.ofPlatform().name("redis-listener-", 1).daemon().factory(),
            (task, executor) -> {
                if (executor.isShutdown()) {
                    return;
                }
                log.warn("Redis listener queue is full: {}. reject oldest message", executor.getQueue().size());
                executor.getQueue().poll();
                executor.execute(task);
            }
        );
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
        RedisMessageListenerContainerConfigurer configurer,
        @Qualifier("redisTaskExecutor") ThreadPoolExecutor redisTaskExecutor
    ) {
        final RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        final RedisConnectionFactory connectionFactory = redisConnectionFactory();

        container.setTaskExecutor(redisTaskExecutor);
        configurer.configure(container, connectionFactory);

        return container;
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

    @Bean
    public RedisScript<Long> addToQueueIfNotFinishedScript() {
        final RedisScript<Long> script = RedisScript.of(
            new ClassPathResource("scripts/AddToQueueIfNotFinished.lua"),
            Long.class
        );

        // eager initialization
        script.getSha1();
        return script;
    }

    @Override
    public void configureMessageConverters(RedisMessageConverters.Builder builder) {
        builder.addCustomConverter(jobResultMessageEnvelopeSerializer);
    }
}
