package com.audiosystem.wsgateway.listener;

import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jspecify.annotations.Nullable;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.listener.RetryListener;

@Log4j2
public class LoggingRetryListener implements RetryListener {

    @Override
    public void failedDelivery(ConsumerRecord<?, ?> record, @Nullable Exception ex, int deliveryAttempt) {
        log.warn(
            "[failedDelivery] attempt: {} topic: {}, partition: {}, offset: {}, key: {}, value: {}, exMessage: {}",
            deliveryAttempt,
            record.topic(),
            record.partition(),
            record.offset(),
            record.key(),
            record.value(),
            ex == null ? null : NestedExceptionUtils.getMostSpecificCause(ex)
        );
    }

    @Override
    public void recovered(ConsumerRecord<?, ?> record, @Nullable Exception ex) {
        log.info(
            "[recovered] topic: {}, partition: {}, offset: {}, key: {}, value: {}, exMessage: {}",
            record.topic(),
            record.partition(),
            record.offset(),
            record.key(),
            record.value(),
            ex == null ? null : NestedExceptionUtils.getMostSpecificCause(ex)
        );
    }

    @Override
    public void recoveryFailed(ConsumerRecord<?, ?> record, @Nullable Exception original, Exception failure) {
        log.error(
            "[recoveryFailed] topic: {}, partition: {}, offset: {}, key: {}, value: {}, exMessage: {}",
            record.topic(),
            record.partition(),
            record.offset(),
            record.key(),
            record.value(),
            original == null ? null : NestedExceptionUtils.getMostSpecificCause(original),
            failure
        );
    }
}
