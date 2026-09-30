package com.audiosystem.api.service;

import com.audiosystem.api.config.properties.S3ConfigurationProperties;
import com.audiosystem.api.exception.JobCreationException;
import com.audiosystem.api.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3UploadServiceTest {

    @InjectMocks
    private S3UploadService service;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3ConfigurationProperties properties;

    @Spy
    private AudioFileService audioFileService;

    @Captor
    private ArgumentCaptor<Consumer<PutObjectRequest.Builder>> consumerArgumentCaptor;

    @Test
    @DisplayName("tests invalid audio type fetched from file")
    void testUploadThrowsJobException() throws IOException {
        final MultipartFile multipartFile = new MockMultipartFile(
            "name",
            "/S3UploadServiceTest/test1.webp",
            "image/webp",
            new ClassPathResource("/S3UploadServiceTest/test1.webp").getInputStream()
        );
        final UUID key = UUID.randomUUID();

        assertThrows(ValidationException.class, () -> service.upload(multipartFile, key), "invalid file type");
    }

    @Test
    @DisplayName("tests sdk exception causes internal exception")
    void testUploadThrowsSdkException() throws IOException {
        final MultipartFile multipartFile = new MockMultipartFile(
            "name",
            "/S3UploadServiceTest/test2.mp3",
            "audio/mpeg",
            new ClassPathResource("/S3UploadServiceTest/test2.mp3").getInputStream()
        );
        final UUID key = UUID.randomUUID();

        BDDMockito.doThrow(SdkException.class).when(s3Client).putObject(any(Consumer.class), any(RequestBody.class));

        assertThrows(JobCreationException.class, () -> service.upload(multipartFile, key), "failed to save file");
        verify(s3Client).putObject(any(Consumer.class), any(RequestBody.class));
    }

    @Test
    void testUploadSuccess() throws IOException {
        final MultipartFile multipartFile = new MockMultipartFile(
            "name",
            "/S3UploadServiceTest/test2.mp3",
            "audio/mpeg",
            new ClassPathResource("/S3UploadServiceTest/test2.mp3").getInputStream()
        );
        final UUID key = UUID.randomUUID();

        BDDMockito.doReturn(null).when(s3Client).putObject(any(Consumer.class), any(RequestBody.class));
        BDDMockito.given(properties.getBucket())
            .willReturn(Map.of("client", new S3ConfigurationProperties.S3BucketProperties("client-bucket")));

        assertDoesNotThrow(() -> service.upload(multipartFile, key));
        verify(s3Client).putObject(consumerArgumentCaptor.capture(), any(RequestBody.class));

        final PutObjectRequest.Builder builder = PutObjectRequest.builder();
        consumerArgumentCaptor.getValue().accept(builder);
        final PutObjectRequest actual = builder.build();

        assertEquals("client-bucket", actual.bucket());
        assertEquals(key.toString(), actual.key());
        assertEquals("audio/mpeg", actual.contentType());
        assertEquals("test2", actual.metadata().get("original-filename"));

    }

    @Test
    void testRollbackThrowsException() {
        BDDMockito.doThrow(SdkException.class).when(s3Client).deleteObject(any(Consumer.class));

        assertFalse(service.rollback(UUID.randomUUID()));
        verify(s3Client).deleteObject(any(Consumer.class));
    }

    @Test
    void testRollbackValid() {
        BDDMockito.doReturn(null).when(s3Client).deleteObject(any(Consumer.class));

        assertTrue(service.rollback(UUID.randomUUID()));
        verify(s3Client).deleteObject(any(Consumer.class));
    }
}