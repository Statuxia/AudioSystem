package com.soundservice.wsgateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

public interface JobResponse {

    @JsonIgnore
    String getDestination(UUID jobId);
}
