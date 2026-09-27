package com.soundservice.api.config;

import com.soundservice.api.config.properties.S3ConfigurationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(S3ConfigurationProperties.class)
public class S3Configuration {

    private final S3ConfigurationProperties s3ConfigurationProperties;

    @Bean
    public AwsCredentials awsCredentials() {
        return getCredentials("root");
    }

    @Bean("presignedUrlCredentials")
    public AwsCredentials presignedUrlCredentials() {
        return getCredentials("presigned");
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .region(Region.EU_CENTRAL_1)
            .credentialsProvider(StaticCredentialsProvider.create(awsCredentials()))
            .endpointOverride(URI.create("http://" + s3ConfigurationProperties.getServer()))
            .serviceConfiguration(builder -> builder.pathStyleAccessEnabled(true).build())
            .build();
    }

    private AwsBasicCredentials getCredentials(String profileName) {
        final S3ConfigurationProperties.S3ProfileProperties profileProperties
            = s3ConfigurationProperties.getProfile().get(profileName);
        return AwsBasicCredentials.create(profileProperties.getUser(), profileProperties.getPassword());
    }
}
