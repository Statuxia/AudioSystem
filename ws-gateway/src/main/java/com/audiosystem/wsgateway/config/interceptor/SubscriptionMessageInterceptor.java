package com.audiosystem.wsgateway.config.interceptor;

import com.audiosystem.wsgateway.dto.JobQueueResponse;
import com.audiosystem.wsgateway.dto.JobStateItem;
import com.audiosystem.wsgateway.dto.JobStateResponse;
import com.audiosystem.wsgateway.dto.JobStatus;
import com.audiosystem.wsgateway.service.RedisService;
import com.audiosystem.wsgateway.service.SubscriptionRegistryService;
import com.audiosystem.wsgateway.service.WebSocketMessageService;
import com.audiosystem.wsgateway.utils.JobIdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.OptionalLong;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Log4j2
public class SubscriptionMessageInterceptor implements ExecutorChannelInterceptor {

    private final RedisService redisService;
    private final SubscriptionRegistryService subscriptionRegistryService;
    private final WebSocketMessageService webSocketMessageService;

    @Override
    public void afterMessageHandled(
        @NonNull Message<?> message,
        @NonNull MessageChannel channel,
        @NonNull MessageHandler handler,
        @Nullable Exception ex
    ) {
        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        if (accessor.getMessageType() != SimpMessageType.SUBSCRIBE  // not subscribed
            || !(handler instanceof SimpleBrokerMessageHandler)     // not broker handler
            || ex != null                                           // if exception exists
            || accessor.getDestination() == null                    // no destination
            || !accessor.getDestination().startsWith("/topic/job/") // diff topic prefix
        ) {
            return;
        }

        final String path = accessor.getDestination().substring(11);

        if (!StringUtils.hasText(path)) {
            return;
        }

        final String[] jobIdAndTopic = path.split("/", 2);
        final UUID jobId = JobIdUtils.parseJobUuid(jobIdAndTopic[0]);
        if (jobId == null) {
            return;
        }

        try {
            if (jobIdAndTopic.length > 1) {
                switch (jobIdAndTopic[1]) {
                    case "queue" ->
                        processSubscribeToQueue(accessor.getSessionId(), accessor.getSubscriptionId(), jobId);
                    case "" -> sendResultIfTerminal(jobId);
                    default -> log.debug("unknown topic: {}", jobIdAndTopic[1]);
                }
            } else {
                sendResultIfTerminal(jobId);
            }
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("redis exception", e);
        } catch (Exception e) {
            log.error("caught exception", e);
        }
    }

    void processSubscribeToQueue(String sessionId, String subscriptionId, UUID jobId) {
        subscriptionRegistryService.subscribe(sessionId, subscriptionId, jobId);

        final OptionalLong rank = redisService.getJobIdRank(jobId);
        if (rank.isEmpty()) {
            log.debug("[{}] no actual rank", jobId);
            return;
        }

        final OptionalLong queuePosition = subscriptionRegistryService.updateQueuePosition(jobId, rank.getAsLong());
        if (queuePosition.isEmpty()) {
            log.debug("[{}] no queue position", jobId);
            return;
        }

        log.debug("[{}] new queuePosition: {}", jobId, queuePosition.getAsLong());
        webSocketMessageService.sendMessage(jobId, new JobQueueResponse(jobId, queuePosition.getAsLong()));
    }

    void sendResultIfTerminal(UUID jobId) {
        final JobStateItem state = redisService.getState(jobId);

        if (state != null && (state.status() == JobStatus.DONE || state.status() == JobStatus.ERROR)) {
            log.debug("[{}] subscription with terminal state {}", jobId, state.status());
            webSocketMessageService.sendMessage(
                jobId,
                new JobStateResponse(state.jobId(), state.status(), state.expireAt())
            );
        } else {
            log.debug("[{}] subscription when state is not terminal. Skip notification", jobId);
        }
    }
}
