package com.soundservice.api.service;

import com.soundservice.api.config.properties.S3ConfigurationProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3PresignedService {

    private final S3ConfigurationProperties s3ConfigurationProperties;
    @Qualifier("presignedUrlCredentials")
    private final AwsCredentials presignedUrlCredentials;

    public PresignedGetObjectRequest getPresignedUrl(HttpServletRequest request, UUID key) {
        try (S3Presigner s3Presigner = getS3Presigner(request)) {
            return s3Presigner.presignGetObject(builder -> builder
                .getObjectRequest(resultBuilder -> resultBuilder.key(key.toString())
                    .bucket(s3ConfigurationProperties.getBucket().get("result").getName()))
                .signatureDuration(Duration.ofMinutes(5))
                .build());
        }
    }

    public S3Presigner getS3Presigner(HttpServletRequest request) {
        return S3Presigner.builder()
            .region(Region.EU_CENTRAL_1)
            .credentialsProvider(StaticCredentialsProvider.create(presignedUrlCredentials))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .endpointOverride(URI.create(buildEndpoint(request)))
            .build();
    }

    private String buildEndpoint(HttpServletRequest request) {
        return "http://%s:%d".formatted(request.getServerName(), s3ConfigurationProperties.getPort());
    }
}
