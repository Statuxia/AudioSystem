package com.audiosystem.wsgateway.service;

import com.audiosystem.wsgateway.dto.JobQueueResponse;
import com.audiosystem.wsgateway.dto.JobResultMessageEnvelope;
import com.audiosystem.wsgateway.dto.JobStateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.annotation.RedisListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Log4j2
public class RedisConsumerService {

    private final RedisService redisService;
    private final SubscriptionRegistryService subscriptionRegistryService;
    private final WebSocketMessageService webSocketMessageService;

    @RedisListener(topic = RedisService.RECEIVED_JOB_RESULTS_TOPIC)
    public void consumeResult(@Payload JobResultMessageEnvelope jobResultMessage) {
        log.debug("[{}] message: {}", jobResultMessage.jobId(), jobResultMessage);

        final UUID jobId = jobResultMessage.jobId();

        webSocketMessageService.sendMessage(
            jobId, new JobStateResponse(
                jobId,
                jobResultMessage.resultMessage().status(),
                jobResultMessage.resultMessage().expireAt()
            )
        );

        notifyInstanceQueue();
    }

    private void notifyInstanceQueue() {
        final Set<UUID> subscribedJobs = subscriptionRegistryService.getAllSubscribedJobs();
        final Map<UUID, Long> queuePositions = redisService.getJobIdsRank(new ArrayList<>(subscribedJobs));

        for (Map.Entry<UUID, Long> entry : queuePositions.entrySet()) {
            final UUID jobId = entry.getKey();
            final Long rank = entry.getValue();

            final OptionalLong queuePosition = subscriptionRegistryService.decreaseQueuePosition(jobId, rank);
            if (queuePosition.isEmpty()) {
                continue;
            }

            final JobQueueResponse response = new JobQueueResponse(jobId, queuePosition.getAsLong());
            try {
                webSocketMessageService.sendMessage(jobId, response);
            } catch (Exception e) {
                log.warn("[{}] failed to send message: {}", jobId, response, e);
            }
        }
    }
}
