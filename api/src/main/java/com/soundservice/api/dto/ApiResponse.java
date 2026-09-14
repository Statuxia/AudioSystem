package com.soundservice.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse(boolean success, String message, Map<String, String> validationErrors) {

    public ApiResponse() {
        this(true, null, null);
    }

    public ApiResponse(String message) {
        this(false, message, null);
    }

    public ApiResponse(Map<String, String> validationErrors) {
        this(false, null, validationErrors);
    }

    public ApiResponse(String message, Map<String, String> validationErrors) {
        this(false, message, validationErrors);
    }
}
