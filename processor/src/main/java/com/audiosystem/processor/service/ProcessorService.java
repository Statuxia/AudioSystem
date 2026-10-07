package com.audiosystem.processor.service;

import com.audiosystem.processor.dto.JobQueueMessage;
import com.audiosystem.processor.dto.JobSettings;
import com.audiosystem.processor.dto.UploadFileDTO;
import com.audiosystem.processor.exception.JobResultFileException;
import com.audiosystem.processor.exception.RedisStatusUpdateException;
import com.audiosystem.processor.utils.ContentDispositionUtils;
import com.audiosystem.processor.utils.FileUtils;
import com.audiosystem.processor.utils.FormatUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class ProcessorService {

    private final S3Service s3Service;
    private final RedisService redisService;
    private final KafkaService kafkaService;
    private final AudioProcessorService audioProcessorService;

    public void process(UUID key, JobQueueMessage settings) {
        try {
            if (!redisService.isInQueue(key)) {
                log.debug("[{}] job already processed. Skip", key);
                return;
            }
        } catch (RedisConnectionFailureException | QueryTimeoutException ex) {
            throw ex; // as is. logging in listener
        } catch (Exception e) {
            log.error("[{}] caught exception on getting job status. Skip", key, e);
            return;
        }

        try {
            log.debug("[{}] getting client source file", key);
            final ResponseInputStream<GetObjectResponse> sourceFile = s3Service.get(key);

            log.debug("[{}] processing file with settings {}", key, settings);
            final UploadFileDTO dto = processFile(key, sourceFile, settings);

            uploadAndNotify(key, dto);
        } catch (RedisStatusUpdateException ex) {
            log.error("[{}] caught exception", key, ex);

            deleteResultFile(key, settings.format());
            deleteSourceFile(key);
            rollbackFile(key);
            throw ex; // as is
        } catch (Exception e) {
            log.error("[{}] caught exception", key, e);

            rollbackFile(key);
            setErrorStatus(key);
            sendErrorMessage(key);
        }

        deleteResultFile(key, settings.format());
        deleteSourceFile(key);
        deleteSource(key);
    }

    private UploadFileDTO processFile(
        UUID key,
        ResponseInputStream<GetObjectResponse> sourceFile,
        JobQueueMessage jobMessage
    ) {
        final Map<String, String> metadata = sourceFile.response().metadata();
        final String contentType = FormatUtils.getContentType(jobMessage.format());
        final String contentDisposition = ContentDispositionUtils.getContentDisposition(
            metadata.get("original-filename"),
            jobMessage.format(),
            "audio"
        );

        final JobSettings jobSettings = new JobSettings.Builder()
            .jobId(key.toString())
            .format(jobMessage.format())
            .contentType(contentType)
            .inputStream(sourceFile)
            .speed(jobMessage.speed())
            .pitchSemitones(jobMessage.pitchSemitones())
            .build();
        final Path resultFilePath = audioProcessorService.process(jobSettings);

        try {
            return new UploadFileDTO(
                contentType,
                contentDisposition,
                Files.newInputStream(resultFilePath),
                Files.size(resultFilePath)
            );
        } catch (IOException e) {
            throw new JobResultFileException("failed to process result file", e);
        }
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

    private void deleteResultFile(UUID key, String format) {
        FileUtils.delete(key, format);
    }

    private void deleteSourceFile(UUID key) {
        FileUtils.deleteSrc(key);
    }
}
