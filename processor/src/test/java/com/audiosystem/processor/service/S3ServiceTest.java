package com.audiosystem.processor.service;

import com.audiosystem.processor.config.properties.S3ConfigurationProperties;
import com.audiosystem.processor.dto.UploadFileDTO;
import com.audiosystem.processor.exception.SourceDeleteException;
import com.audiosystem.processor.exception.SourceDownloadException;
import com.audiosystem.processor.exception.SourceUploadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @InjectMocks
    private S3Service service;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3ConfigurationProperties s3ConfigurationProperties;

    @Captor
    private ArgumentCaptor<Consumer<GetObjectRequest.Builder>> getConsumerCaptor;
    @Captor
    private ArgumentCaptor<Consumer<PutObjectRequest.Builder>> putConsumerCaptor;
    @Captor
    private ArgumentCaptor<Consumer<DeleteObjectRequest.Builder>> deleteConsumerCaptor;

    private void mockBuckets() {
        BDDMockito.given(s3ConfigurationProperties.getBucket()).willReturn(Map.of(
            "client", new S3ConfigurationProperties.S3PropertiesBucket("client-bucket"),
            "result", new S3ConfigurationProperties.S3PropertiesBucket("result-bucket")
        ));
    }

    @Test
    void testGetValid() {
        mockBuckets();
        final UUID key = UUID.randomUUID();
        final ResponseInputStream<GetObjectResponse> mockStream = Mockito.mock(ResponseInputStream.class);

        BDDMockito.doReturn(mockStream).when(s3Client).getObject(any(Consumer.class));

        assertEquals(mockStream, service.get(key));
        verify(s3Client).getObject(getConsumerCaptor.capture());

        final GetObjectRequest.Builder builder = GetObjectRequest.builder();
        getConsumerCaptor.getValue().accept(builder);
        final GetObjectRequest actual = builder.build();

        assertEquals("client-bucket", actual.bucket());
        assertEquals(key.toString(), actual.key());
    }

    @Test
    void testGetThrowsSourceDownloadException() {
        final UUID key = UUID.randomUUID();

        BDDMockito.doThrow(SdkException.class).when(s3Client).getObject(any(Consumer.class));

        assertThrows(SourceDownloadException.class, () -> service.get(key));
    }

    @Test
    void testUploadValid() {
        mockBuckets();
        final UUID key = UUID.randomUUID();
        final UploadFileDTO dto = new UploadFileDTO(
            "audio/mpeg",
            "attachment; filename=\"result.mp3\"",
            new ByteArrayInputStream(new byte[]{1, 2, 3}),
            3L
        );

        BDDMockito.doReturn(null).when(s3Client).putObject(any(Consumer.class), any(RequestBody.class));

        assertDoesNotThrow(() -> service.upload(key, dto));
        verify(s3Client).putObject(putConsumerCaptor.capture(), any(RequestBody.class));

        final PutObjectRequest.Builder builder = PutObjectRequest.builder();
        putConsumerCaptor.getValue().accept(builder);
        final PutObjectRequest actual = builder.build();

        assertEquals("result-bucket", actual.bucket());
        assertEquals(key.toString(), actual.key());
        assertEquals("audio/mpeg", actual.contentType());
        assertEquals("attachment; filename=\"result.mp3\"", actual.contentDisposition());
    }

    @Test
    void testUploadThrowsSourceUploadException() {
        final UUID key = UUID.randomUUID();
        final UploadFileDTO dto = new UploadFileDTO(
            "audio/mpeg", "attachment", new ByteArrayInputStream(new byte[0]), 0L
        );

        BDDMockito.doThrow(SdkException.class).when(s3Client).putObject(any(Consumer.class), any(RequestBody.class));

        assertThrows(SourceUploadException.class, () -> service.upload(key, dto));
    }

    @Test
    void testDeleteSourceUsesClientBucket() {
        mockBuckets();
        final UUID key = UUID.randomUUID();

        BDDMockito.doReturn(null).when(s3Client).deleteObject(any(Consumer.class));

        assertDoesNotThrow(() -> service.deleteSource(key));
        verify(s3Client).deleteObject(deleteConsumerCaptor.capture());

        final DeleteObjectRequest.Builder builder = DeleteObjectRequest.builder();
        deleteConsumerCaptor.getValue().accept(builder);
        final DeleteObjectRequest actual = builder.build();

        assertEquals("client-bucket", actual.bucket());
        assertEquals(key.toString(), actual.key());
    }

    @Test
    void testRollbackResultUsesResultBucket() {
        mockBuckets();
        final UUID key = UUID.randomUUID();

        BDDMockito.doReturn(null).when(s3Client).deleteObject(any(Consumer.class));

        assertDoesNotThrow(() -> service.rollbackResult(key));
        verify(s3Client).deleteObject(deleteConsumerCaptor.capture());

        final DeleteObjectRequest.Builder builder = DeleteObjectRequest.builder();
        deleteConsumerCaptor.getValue().accept(builder);
        final DeleteObjectRequest actual = builder.build();

        assertEquals("result-bucket", actual.bucket());
        assertEquals(key.toString(), actual.key());
    }

    @Test
    void testDeleteThrowsSourceDeleteException() {
        mockBuckets();
        final UUID key = UUID.randomUUID();

        BDDMockito.doThrow(SdkException.class).when(s3Client).deleteObject(any(Consumer.class));

        assertThrows(SourceDeleteException.class, () -> service.deleteSource(key));
    }
}
