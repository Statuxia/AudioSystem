package com.audiosystem.wsgateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobResultMessage(JobStatus status, Long expireAt) {
}
