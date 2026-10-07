package com.audiosystem.processor.validator;

import com.audiosystem.processor.dto.JobQueueMessage;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueueMessageValidator implements Validator {

    public static final Pattern PATTERN = Pattern.compile("mp3|wav|ogg|opus|m4a|aac|flac|webm|aiff|aif");
    public static final Float MIN_SPEED = 0.1F;
    public static final Float MAX_SPEED = 3F;
    public static final Float MIN_PITCH_SEMITONES = -12F;
    public static final Float MAX_PITCH_SEMITONES = 12F;
    public static final Float DELTA = 0.01F;

    @Override
    public boolean supports(Class<?> clazz) {
        return JobQueueMessage.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        final JobQueueMessage message = (JobQueueMessage) target;

        validateFormat(message.format(), errors);
        validateSpeedRange(message.speed(), errors);
        validatePitchSemitonesRange(message.pitchSemitones(), errors);
    }

    private void validateFormat(String format, Errors errors) {
        if (!StringUtils.hasText(format)) {
            errors.reject("JobQueueMessage.error.format.isEmpty", "format is empty");
            return;
        }

        final Matcher matcher = PATTERN.matcher(format);
        if (!matcher.matches()) {
            errors.reject("JobQueueMessage.error.format.unavailable", "unavailable format");
        }
    }

    private void validateSpeedRange(Float number, Errors errors) {
        validateRange(number, errors, "speed", MIN_SPEED, MAX_SPEED, DELTA);
    }

    private void validatePitchSemitonesRange(Float number, Errors errors) {
        validateRange(number, errors, "pitchSemitones", MIN_PITCH_SEMITONES, MAX_PITCH_SEMITONES, DELTA);
    }

    private void validateRange(Float number, Errors errors, String key, Float min, Float max, Float delta) {
        if (number == null || number.isNaN() || number.isInfinite()) {
            errors.reject(
                "JobQueueMessage.error.%s.notSupported".formatted(key),
                "%s value is not supported".formatted(key)
            );
            return;
        }

        if (number - min <= -delta || number - max >= delta) {
            errors.reject(
                "JobQueueMessage.error.%s.notInRange".formatted(key),
                "%s value not in range".formatted(key)
            );
        }
    }
}
