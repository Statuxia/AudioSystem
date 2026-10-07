package com.audiosystem.processor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobQueueMessage(String format, Float speed, Float pitchSemitones) {
}
