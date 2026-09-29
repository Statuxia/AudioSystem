package com.soundservice.processor.dto;

import com.soundservice.processor.exception.JobSettingsBuildException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;
import java.util.Objects;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class JobSettings {

    private final String jobId;
    private final String format;
    private final String contentType;
    private final InputStream inputStream;
    private final Float speed;
    private final Float pitchSemitones;

    public static class Builder {

        private String jobId;
        private String format;
        private String contentType;
        private InputStream inputStream;
        private Float speed;
        private Float pitchSemitones;

        public Builder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder inputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public Builder speed(Float speed) {
            this.speed = speed == null ? 1 : Math.clamp(speed, 0.1f, 3f);
            return this;
        }

        public Builder pitchSemitones(Float pitchSemitones) {
            this.pitchSemitones = pitchSemitones == null ? 0 : Math.clamp(pitchSemitones, -10, 10);
            return this;
        }

        public JobSettings build() {
            try {
                Objects.requireNonNull(jobId, "jobId is null");
                Objects.requireNonNull(format, "format is null");
                Objects.requireNonNull(contentType, "contentType is null");
                Objects.requireNonNull(inputStream, "inputStream is null");
                Objects.requireNonNull(speed, "speed is null");
                Objects.requireNonNull(pitchSemitones, "pitchSemitones is null");
            } catch (NullPointerException e) {
                throw new JobSettingsBuildException("one of arguments is null", e);
            }
            return new JobSettings(jobId, format, contentType, inputStream, speed, pitchSemitones);
        }
    }
}
