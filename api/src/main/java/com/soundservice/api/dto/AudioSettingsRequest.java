package com.soundservice.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class AudioSettingsRequest {
    @NotEmpty
    @Pattern(regexp = "mp3|wav|ogg|opus|m4a|aac|flac|webm|aiff|aif")
    private String format = "mp3";
    @DecimalMin("0.1")
    @DecimalMax("3")
    private Float speed = 1F;
    @DecimalMin("-10")
    @DecimalMax("10")
    private Float pitchSemitones = 0F;
}
