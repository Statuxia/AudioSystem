package com.audiosystem.wsgateway.service;

import com.audiosystem.wsgateway.dto.JobResultMessageEnvelope;
import org.springframework.data.redis.annotation.RedisListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class RedisConsumerService {

    @RedisListener(topic = RedisService.RECEIVED_JOB_RESULTS_TOPIC)
    public void consumeResult(@Payload JobResultMessageEnvelope jobResultMessage) {
        // todo: consuming
    }
}
