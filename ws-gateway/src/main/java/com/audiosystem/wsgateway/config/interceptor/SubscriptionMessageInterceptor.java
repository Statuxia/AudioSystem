package com.audiosystem.wsgateway.config.interceptor;

import com.audiosystem.wsgateway.dto.JobStateItem;
import com.audiosystem.wsgateway.dto.JobStateResponse;
import com.audiosystem.wsgateway.dto.JobStatus;
import com.audiosystem.wsgateway.service.RedisService;
import com.audiosystem.wsgateway.service.WebSocketMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Log4j2
public class SubscriptionMessageInterceptor implements ChannelInterceptor {

    private final RedisService redisService;
    private final WebSocketMessageService webSocketMessageService;

    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        if (accessor.getMessageType() != SimpMessageType.SUBSCRIBE) {
            return message;
        }

        if (accessor.getDestination() == null) {
            return message;
        }

        if (!accessor.getDestination().startsWith("/topic/job/")) {
            return message;
        }

        final String jobId = accessor.getDestination().substring(12);

        if (!StringUtils.hasText(jobId)) {
            return message;
        }

        try {
            final UUID uuid = UUID.fromString(jobId);
            final JobStateItem state = redisService.getState(uuid);

            if (state == null) {
                return message;
            }

            if (state.status() == JobStatus.DONE || state.status() == JobStatus.ERROR) {
                webSocketMessageService.sendMessage(
                    uuid,
                    new JobStateResponse(state.jobId(), state.status(), state.expireAt())
                );
            }

        } catch (IllegalArgumentException e) {
            log.error("wrong jobId format", e);
            return message;
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("redis exception", e);
            return message;
        }

        return message;
    }
}
