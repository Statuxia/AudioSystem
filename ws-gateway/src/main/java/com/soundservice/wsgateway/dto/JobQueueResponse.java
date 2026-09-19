package com.soundservice.wsgateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobQueueResponse(UUID jobId, Long queuePosition) implements JobResponse {
    public static final String DESTINATION_TEMPLATE = "/topic/job/%s/queue";

    @JsonIgnore
    @Override
    public String getDestination(UUID uuid) {
        return DESTINATION_TEMPLATE.formatted(uuid);
    }
}
