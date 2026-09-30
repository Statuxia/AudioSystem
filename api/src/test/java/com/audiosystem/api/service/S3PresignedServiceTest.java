package com.audiosystem.api.service;

import com.audiosystem.api.config.properties.S3ConfigurationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class S3PresignedServiceTest {
    public static final String HOST = "localhost:8080"; // MockHttpServletRequest removes port and :
    public static final S3ConfigurationProperties.S3BucketProperties BUCKET_PROPERTIES
        = new S3ConfigurationProperties.S3BucketProperties("result");

    @Test
    void testValidGetS3Presigner() {
        final S3ConfigurationProperties properties = BDDMockito.mock(S3ConfigurationProperties.class);
        final AwsCredentials credentials = AwsBasicCredentials.create("presignedUser", "presignedPassword");
        final S3PresignedService service = new S3PresignedService(properties, credentials);
        final String key = new UUID(0, 1).toString();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.HOST, HOST);

        BDDMockito.given(properties.getPort()).willReturn(8000);
        BDDMockito.given(properties.getBucket()).willReturn(Map.of("result", BUCKET_PROPERTIES));

        final S3Presigner s3Presigner = service.getS3Presigner(request);
        final PresignedGetObjectRequest objectRequest = s3Presigner.presignGetObject(builder -> builder
            .getObjectRequest(resultBuilder -> resultBuilder.key(key)
                .bucket(properties.getBucket().get("result").getName()))
            .signatureDuration(Duration.ofMinutes(5))
            .build());

        assertTrue(objectRequest.url().toString().startsWith("http://%s:%s".formatted(
            request.getServerName(), properties.getPort()
        )));
        assertTrue(objectRequest.isBrowserExecutable());
        assertEquals(
            "/%s/%s".formatted(properties.getBucket().get("result").getName(), key),
            objectRequest.url().getPath()
        );
        assertEquals(SdkHttpMethod.GET, objectRequest.httpRequest().method());

        // 3 seconds for reduce flacky behavior
        assertTrue(
            Instant.now().plus(5, ChronoUnit.MINUTES).plusSeconds(3)
                .isAfter(objectRequest.expiration())
        );
        assertTrue(
            Instant.now().plus(5, ChronoUnit.MINUTES).minusSeconds(3)
                .isBefore(objectRequest.expiration())
        );
    }

    @Test
    void testValidGetPresignedUrl() {
        final S3ConfigurationProperties properties = BDDMockito.mock(S3ConfigurationProperties.class);
        final AwsCredentials credentials = AwsBasicCredentials.create("presignedUser", "presignedPassword");
        final S3PresignedService service = new S3PresignedService(properties, credentials);
        final UUID key = new UUID(0, 1);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.HOST, HOST);

        BDDMockito.given(properties.getPort()).willReturn(8000);
        BDDMockito.given(properties.getBucket()).willReturn(Map.of("result", BUCKET_PROPERTIES));

        final PresignedGetObjectRequest objectRequest = service.getPresignedUrl(request, key);
        assertTrue(objectRequest.url().toString().startsWith("http://%s:%s".formatted(
            request.getServerName(), properties.getPort()
        )));
        assertTrue(objectRequest.isBrowserExecutable());
        assertEquals(
            "/%s/%s".formatted(properties.getBucket().get("result").getName(), key),
            objectRequest.url().getPath()
        );
        assertEquals(SdkHttpMethod.GET, objectRequest.httpRequest().method());

        // 3 seconds for reduce flacky behavior
        assertTrue(
            Instant.now().plus(5, ChronoUnit.MINUTES).plusSeconds(3)
                .isAfter(objectRequest.expiration())
        );
        assertTrue(
            Instant.now().plus(5, ChronoUnit.MINUTES).minusSeconds(3)
                .isBefore(objectRequest.expiration())
        );
    }
}