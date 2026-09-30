package com.audiosystem.processor.service;

import com.audiosystem.processor.config.properties.S3ConfigurationProperties;
import com.audiosystem.processor.dto.UploadFileDTO;
import com.audiosystem.processor.exception.SourceDeleteException;
import com.audiosystem.processor.exception.SourceDownloadException;
import com.audiosystem.processor.exception.SourceUploadException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3ConfigurationProperties s3ConfigurationProperties;
    private final S3Client s3Client;

    public ResponseInputStream<GetObjectResponse> get(UUID key) {
        try {
            return s3Client.getObject(builder -> builder.key(key.toString())
                .bucket(s3ConfigurationProperties.getBucket().get("client").getName())
                .build());
        } catch (SdkException e) {
            throw new SourceDownloadException("failed to get object", e);
        }
    }

    public void upload(UUID key, UploadFileDTO dto) {
        try {
            s3Client.putObject(
                builder -> builder
                    .key(key.toString())
                    .bucket(s3ConfigurationProperties.getBucket().get("result").getName())
                    .contentType(dto.contentType())
                    .contentDisposition(dto.contentDisposition())
                    .build(),
                RequestBody.fromInputStream(dto.inputStream(), dto.contentLength())
            );
        } catch (SdkException e) {
            throw new SourceUploadException("failed to upload object", e);
        }
    }

    public void deleteSource(UUID key) {
        delete(key, s3ConfigurationProperties.getBucket().get("client").getName());
    }

    public void rollbackResult(UUID key) {
        delete(key, s3ConfigurationProperties.getBucket().get("result").getName());
    }

    private void delete(UUID key, String bucket) {
        try {
            s3Client.deleteObject(builder -> builder.key(key.toString()).bucket(bucket).build());
        } catch (SdkException e) {
            throw new SourceDeleteException("failed to delete object", e);
        }
    }
}
