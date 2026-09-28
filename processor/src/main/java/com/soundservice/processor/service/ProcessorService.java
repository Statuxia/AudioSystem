package com.soundservice.processor.service;

import com.soundservice.processor.dto.JobQueueMessage;
import com.soundservice.processor.dto.UploadFileDTO;
import com.soundservice.processor.utils.ContentDispositionUtils;
import com.soundservice.processor.utils.ContentTypeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class ProcessorService {

    private final S3Service s3Service;
    private final RedisService redisService;
    private final KafkaService kafkaService;

    public void process(UUID key, JobQueueMessage settings) {
        try {
            log.debug("[{}] getting client source file", key);
            final ResponseInputStream<GetObjectResponse> sourceFile = s3Service.get(key);

            log.debug("[{}] processing file with settings {}", key, settings);
            final UploadFileDTO dto = processFile(sourceFile, settings);

            uploadAndNotify(key, dto);
        } catch (Exception e) {
            log.error("[{}] caught exception", key, e);

            rollbackFile(key);
            setErrorStatus(key);
            sendErrorMessage(key);
        }

        deleteSource(key);
    }

    private UploadFileDTO processFile(ResponseInputStream<GetObjectResponse> sourceFile, JobQueueMessage settings) {
        final Map<String, String> metadata = sourceFile.response().metadata();
        final String contentType = ContentTypeUtils.getContentType(settings.format());
        final String contentDisposition = ContentDispositionUtils.getContentDisposition(
            metadata.get("original-filename"),
            settings.format(),
            "audio"
        );

        // todo: process

        return new UploadFileDTO(
            contentType,
            contentDisposition,
            null,
            null,
            null
        );
    }

    /**
     * Сохраняем с приоритетом вызовов: s3 -> redis -> kafka (irreversible)
     */
    private void uploadAndNotify(UUID key, UploadFileDTO dto) {
        log.debug("[{}] uploading result file", key);
        s3Service.upload(key, dto);
        log.debug("[{}] updating redis status to done", key);
        redisService.setDone(key);
        log.debug("[{}] sending success result message", key);
        kafkaService.sendDoneMessage(key);
    }

    private void deleteSource(UUID key) {
        try {
            log.debug("[{}] deleting client source file", key);
            s3Service.deleteSource(key); // delete source from silo
        } catch (Exception e) {
            log.error("[{}] caught exception on deleting client source file", key, e);
        }
    }

    private void rollbackFile(UUID key) {
        try {
            log.debug("[{}] deleting result file", key);
            s3Service.rollbackResult(key); // rollback result in silo
        } catch (Exception e) {
            log.error("[{}] caught exception on rollback s3", key, e);
        }
    }

    private void setErrorStatus(UUID key) {
        try {
            log.debug("[{}] updating redis status to error", key);
            redisService.setError(key); // update redis to error status
        } catch (Exception e) {
            log.error("[{}] caught exception on change redis status", key, e);
        }
    }

    private void sendErrorMessage(UUID key) {
        try {
            log.debug("[{}] sending error result message", key);
            kafkaService.sendErrorMessage(key); // produce error message to kafka
        } catch (Exception e) {
            log.error("[{}] caught exception on sending message to kafka", key, e);
        }
    }
}
