package com.soundservice.processor.config;

import com.soundservice.processor.config.properties.S3ConfigurationProperties;
import com.soundservice.processor.config.properties.S3ConfigurationProperties.S3PropertiesProfile;
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
@EnableConfigurationProperties(S3ConfigurationProperties.class)
@RequiredArgsConstructor
public class S3Configuration {

    private final S3ConfigurationProperties s3ConfigurationProperties;

    @Bean
    public AwsCredentials s3Credentials() {
        final S3PropertiesProfile profile = s3ConfigurationProperties.getProfile().get("root");
        return AwsBasicCredentials.create(profile.getUser(), profile.getPassword());
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .region(Region.EU_CENTRAL_1)
            .credentialsProvider(StaticCredentialsProvider.create(s3Credentials()))
            .endpointOverride(URI.create("http://" + s3ConfigurationProperties.getServer()))
            .serviceConfiguration(builder -> builder.pathStyleAccessEnabled(true))
            .build();
    }
}
