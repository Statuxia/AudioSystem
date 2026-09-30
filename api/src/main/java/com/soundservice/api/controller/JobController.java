package com.soundservice.api.controller;

import com.fasterxml.uuid.Generators;
import com.soundservice.api.annotations.RateLimit;
import com.soundservice.api.dto.*;
import com.soundservice.api.exception.JobCreationException;
import com.soundservice.api.exception.JobStateException;
import com.soundservice.api.service.KafkaProducerService;
import com.soundservice.api.service.RedisService;
import com.soundservice.api.service.S3PresignedService;
import com.soundservice.api.service.S3UploadService;
import com.soundservice.api.utils.PresetSettingsStorage;
import com.soundservice.api.validation.NotEmptyFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(JobController.PREFIX)
@RequiredArgsConstructor
@Validated
@Log4j2
@CrossOrigin("*")
public class JobController {

    public static final String PREFIX = "/v1/job";

    private final S3UploadService s3UploadService;
    private final S3PresignedService s3PresignedService;
    private final KafkaProducerService kafkaProducerService;
    private final RedisService redisService;

    @GetMapping(value = "/presets", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PresetSettings>> presets() {
        return ResponseEntity.ok(PresetSettingsStorage.PRESETS);
    }

    @PostMapping(
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @RateLimit(requestsPerMinute = 3L)
    public ResponseEntity<JobResponse> create(
        @Valid @NotEmptyFile @RequestPart("file") MultipartFile file,
        @Valid @RequestPart("settings") AudioSettingsRequest request
    ) {
        final UUID jobId = Generators.timeBasedEpochGenerator().generate(); // UUIDv7 for jobId

        s3UploadService.upload(file, jobId);

        final boolean kafkaMessageUploaded = kafkaProducerService.sendMessage(
            jobId, new JobQueueMessage(
                request.getFormat(),
                request.getSpeed(),
                request.getPitchSemitones()
            )
        );

        if (!kafkaMessageUploaded) {
            s3UploadService.rollback(jobId);
            throw new JobCreationException("failed to enqueue job");
        }

        try {
            redisService.saveInQueueJobState(jobId);
        } catch (Exception e) {
            log.error("caught exception on saving job status", e);
        }

        return ResponseEntity.ok(new JobResponse(jobId));
    }

    @GetMapping(value = "/{job_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(requestsPerMinute = 60, requestsPerSecond = 5)
    public ResponseEntity<JobStateResponse> forceCheck(@PathVariable("job_id") UUID jobId) {
        final JobStateItem state = redisService.getJobState(jobId);

        if (state == null) {
            throw new JobStateException(HttpStatus.NOT_FOUND, "job not found");
        }

        return ResponseEntity.ok(new JobStateResponse(jobId, state.status(), state.expireAt()));
    }

    @GetMapping(value = "/{job_id}/download")
    @RateLimit(requestsPerMinute = 6)
    public ResponseEntity<Void> download(
        HttpServletRequest request,
        @Valid @NotNull @PathVariable("job_id") UUID jobId
    ) {
        final PresignedGetObjectRequest presignedUrl = s3PresignedService.getPresignedUrl(request, jobId);
        return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
            .location(presignedUrl.httpRequest().getUri()).build();
    }
}
