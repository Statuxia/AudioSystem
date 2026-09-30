package com.audiosystem.processor.service;

import com.audiosystem.processor.dto.JobQueueMessage;
import com.audiosystem.processor.utils.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProcessorServiceTest {

    private static final String FORMAT = "mp3";
    private static final JobQueueMessage SETTINGS = new JobQueueMessage(FORMAT, 1.0F, 0F);

    @InjectMocks
    private ProcessorService processorService;

    @Mock
    private S3Service s3Service;

    @Mock
    private RedisService redisService;

    @Mock
    private KafkaService kafkaService;

    @Mock
    private AudioProcessorService audioProcessorService;

    private UUID key;
    private Path resultPath;
    private Path srcPath;

    @AfterEach
    void cleanup() throws IOException {
        if (resultPath != null) {
            Files.deleteIfExists(resultPath);
        }
        if (srcPath != null) {
            Files.deleteIfExists(srcPath);
        }
    }

    private ResponseInputStream<GetObjectResponse> mockSourceFile() {
        final GetObjectResponse response = GetObjectResponse.builder()
            .metadata(Map.of("original-filename", "song"))
            .build();
        return new ResponseInputStream<>(response, AbortableInputStream.create(new ByteArrayInputStream(new byte[0])));
    }

    private void createResultFile() throws IOException {
        resultPath = FileUtils.buildPath(key.toString(), FORMAT);
        Files.write(resultPath, new byte[]{1, 2, 3});
    }

    private void createSourceFile() throws IOException {
        srcPath = FileUtils.buildSrcPath(key.toString());
        Files.write(srcPath, new byte[]{1, 2, 3});
    }

    @Test
    void testProcessHappyPath() throws IOException {
        key = UUID.randomUUID();
        createResultFile();
        createSourceFile();

        BDDMockito.given(s3Service.get(key)).willReturn(mockSourceFile());
        BDDMockito.given(audioProcessorService.process(any())).willReturn(resultPath);

        assertDoesNotThrow(() -> processorService.process(key, SETTINGS));

        verify(s3Service).upload(eq(key), any());
        verify(redisService).setDone(key);
        verify(kafkaService).sendDoneMessage(key);
        verify(s3Service).deleteSource(key);

        verify(s3Service, never()).rollbackResult(any());
        verify(redisService, never()).setError(any());
        verify(kafkaService, never()).sendErrorMessage(any());

        assertFalse(Files.exists(resultPath), "result file should be cleaned up");
        assertFalse(Files.exists(srcPath), "source file should be cleaned up");
    }

    @Test
    void testProcessFailsOnS3Get() {
        key = UUID.randomUUID();
        BDDMockito.given(s3Service.get(key)).willThrow(RuntimeException.class);

        assertDoesNotThrow(() -> processorService.process(key, SETTINGS));

        verify(s3Service).rollbackResult(key);
        verify(redisService).setError(key);
        verify(kafkaService).sendErrorMessage(key);
        verify(s3Service).deleteSource(key);

        verify(s3Service, never()).upload(any(), any());
        verify(redisService, never()).setDone(any());
        verify(kafkaService, never()).sendDoneMessage(any());
    }

    @Test
    void testProcessFailsOnAudioProcessing() {
        key = UUID.randomUUID();
        BDDMockito.given(s3Service.get(key)).willReturn(mockSourceFile());
        BDDMockito.given(audioProcessorService.process(any())).willThrow(RuntimeException.class);

        assertDoesNotThrow(() -> processorService.process(key, SETTINGS));

        verify(s3Service).rollbackResult(key);
        verify(redisService).setError(key);
        verify(kafkaService).sendErrorMessage(key);

        verify(s3Service, never()).upload(any(), any());
        verify(redisService, never()).setDone(any());
        verify(kafkaService, never()).sendDoneMessage(any());
    }

    @Test
    void testProcessFailsOnS3Upload() throws IOException {
        key = UUID.randomUUID();
        createResultFile();

        BDDMockito.given(s3Service.get(key)).willReturn(mockSourceFile());
        BDDMockito.given(audioProcessorService.process(any())).willReturn(resultPath);
        BDDMockito.willThrow(RuntimeException.class).given(s3Service).upload(any(), any());

        assertDoesNotThrow(() -> processorService.process(key, SETTINGS));

        verify(s3Service).rollbackResult(key);
        verify(redisService).setError(key);
        verify(kafkaService).sendErrorMessage(key);

        verify(redisService, never()).setDone(any());
        verify(kafkaService, never()).sendDoneMessage(any());

        assertFalse(Files.exists(resultPath), "result file should be cleaned up even on failure");
    }

    @Test
    void testCleanupStepsAreIndependent() {
        key = UUID.randomUUID();
        BDDMockito.given(s3Service.get(key)).willThrow(RuntimeException.class);
        BDDMockito.willThrow(RuntimeException.class).given(s3Service).rollbackResult(key);

        assertDoesNotThrow(() -> processorService.process(key, SETTINGS));

        verify(redisService).setError(key);
        verify(kafkaService).sendErrorMessage(key);
    }
}
