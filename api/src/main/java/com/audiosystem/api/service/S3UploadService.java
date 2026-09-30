package com.audiosystem.api.service;

import com.audiosystem.api.config.properties.S3ConfigurationProperties;
import com.audiosystem.api.exception.JobCreationException;
import com.audiosystem.api.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.io.TikaInputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class S3UploadService {

    private final S3ConfigurationProperties s3ConfigurationProperties;
    private final S3Client s3Client;
    private final AudioFileService audioFileService;

    public void upload(MultipartFile multipartFile, UUID key) {
        final TikaInputStream inputStream = audioFileService.getInputStream(multipartFile);
        final String contentType = audioFileService.getContentType(inputStream);
        final String name = audioFileService.getFileName(multipartFile, key.toString());

        if (!audioFileService.isValidAudioType(contentType)) {
            log.debug("[{}] invalid file type: {}", key, contentType);
            throw new ValidationException("invalid file type: " + contentType);
        }

        try {
            s3Client.putObject(
                builder -> builder
                    .bucket(s3ConfigurationProperties.getBucket().get("client").getName())
                    .key(key.toString())
                    .metadata(Map.of("original-filename", URLEncoder.encode(name, StandardCharsets.UTF_8)))
                    .contentType(contentType),
                RequestBody.fromInputStream(inputStream.unwrap(), multipartFile.getSize())
            );
        } catch (SdkException e) {
            log.error("[{}] failed to save file.", key, e);
            throw new JobCreationException("failed to save file");
        }
    }

    public boolean rollback(UUID key) {
        try {
            s3Client.deleteObject(builder -> builder
                .bucket(s3ConfigurationProperties.getBucket().get("client").getName())
                .key(key.toString())
            );
            return true;
        } catch (SdkException e) {
            log.error("[{}] failed to delete file.", key, e);
            return false;
        }
    }
}
