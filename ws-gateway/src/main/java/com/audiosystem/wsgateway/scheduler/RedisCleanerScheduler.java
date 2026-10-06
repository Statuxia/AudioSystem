package com.audiosystem.wsgateway.scheduler;

import com.audiosystem.wsgateway.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Log4j2
@RequiredArgsConstructor
public class RedisCleanerScheduler {

    private final RedisService redisService;

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void process() {
        try {
            final Long totalRemoved = redisService.removeJobQueueByOutdatedScore();
            log.debug("outdated jobs removed: {}", totalRemoved);
        } catch (Exception e) {
            log.error("caught exception on job queue removing", e);
        }
    }
}
