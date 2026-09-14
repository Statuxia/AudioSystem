package com.soundservice.api.controller;

import com.soundservice.api.annotations.RateLimit;
import com.soundservice.api.dto.*;
import com.soundservice.api.utils.PresetSettingsStorage;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(JobController.PREFIX)
public class JobController {

    public static final String PREFIX = "/v1/job";

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
        @RequestPart("file") MultipartFile file,
        @RequestPart("settings") AudioSettingsRequest request
    ) {
        return ResponseEntity.ok(new JobResponse(UUID.randomUUID()));
    }

    @GetMapping(value = "/{job_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(requestsPerMinute = 60)
    public ResponseEntity<JobStateResponse> forceCheck(@PathVariable("job_id") UUID jobId) {
        return ResponseEntity.ok(new JobStateResponse(jobId, JobStatus.IN_QUEUE, System.currentTimeMillis()));
    }

    @GetMapping(value = "/{job_id}/download")
    @RateLimit(requestsPerMinute = 6)
    public ResponseEntity<byte[]> download(@PathVariable("job_id") UUID jobId) {
        final byte[] content = "stub".getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename("stub-" + jobId).build().toString()
            )
            .contentType(MediaType.TEXT_PLAIN)
            .contentLength(content.length)
            .body(content);
    }
}
