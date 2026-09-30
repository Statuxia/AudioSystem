package com.audiosystem.wsgateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobStateResponse(UUID jobId, JobStatus status, Long expireAt) implements JobResponse {
    public static final String DESTINATION_TEMPLATE = "/topic/job/%s";

    @JsonIgnore
    @Override
    public String getDestination(UUID jobId) {
        return DESTINATION_TEMPLATE.formatted(jobId);
    }
}
