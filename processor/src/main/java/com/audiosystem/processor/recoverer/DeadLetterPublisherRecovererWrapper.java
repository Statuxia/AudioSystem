package com.audiosystem.processor.recoverer;

import com.audiosystem.processor.service.KafkaService;
import com.audiosystem.processor.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import java.util.UUID;

@RequiredArgsConstructor
@Log4j2
public class DeadLetterPublisherRecovererWrapper implements ConsumerRecordRecoverer {

    private final DeadLetterPublishingRecoverer dlt;
    private final KafkaService kafkaService;
    private final RedisService redisService;

    @Override
    public void accept(ConsumerRecord<?, ?> consumerRecord, Exception exception) {
        dlt.accept(consumerRecord, exception);
        if (consumerRecord.key() instanceof UUID jobId) {
            try {
                redisService.setError(jobId);
            } catch (Exception e) {
                log.error("[{}] caught exception on updating job state", jobId, e);
            }

            try {
                kafkaService.sendErrorMessage(jobId);
            } catch (Exception e) {
                log.error("[{}] caught exception on sending message to result topic", jobId, e);
            }
        }
    }
}
