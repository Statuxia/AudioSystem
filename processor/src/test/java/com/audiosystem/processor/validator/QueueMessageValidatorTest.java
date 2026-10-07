package com.audiosystem.processor.validator;

import com.audiosystem.processor.dto.JobQueueMessage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueueMessageValidatorTest {

    private final QueueMessageValidator queueMessageValidator = new QueueMessageValidator();

    @ParameterizedTest
    @MethodSource("validMessageDataSource")
    void validateValid(JobQueueMessage message) {
        final MapBindingResult errors = new MapBindingResult(new HashMap<>(), JobQueueMessage.class.getSimpleName());

        queueMessageValidator.validate(message, errors);

        assertFalse(errors.hasErrors());
    }

    @ParameterizedTest
    @MethodSource("invalidMessageDataSource")
    void validateInvalid(JobQueueMessage message) {
        final MapBindingResult errors = new MapBindingResult(new HashMap<>(), JobQueueMessage.class.getSimpleName());

        queueMessageValidator.validate(message, errors);

        assertTrue(errors.hasErrors());
    }

    private static Stream<Arguments> validMessageDataSource() {
        return Stream.of(
            Arguments.argumentSet("common", new JobQueueMessage("mp3", 1.0F, 1.0F)),
            Arguments.argumentSet("speed min range", new JobQueueMessage("mp3", 0.1F, 1.0F)),
            Arguments.argumentSet("speed max range", new JobQueueMessage("mp3", 3.0F, 1.0F)),
            Arguments.argumentSet("pitchSemitones min range", new JobQueueMessage("mp3", 1.0F, -12.0F)),
            Arguments.argumentSet("pitchSemitones max range", new JobQueueMessage("mp3", 1.0F, 12.0F))
        );
    }

    private static Stream<Arguments> invalidMessageDataSource() {
        return Stream.of(
            Arguments.argumentSet("null format", new JobQueueMessage(null, 1.0F, 1.0F)),
            Arguments.argumentSet("empty format", new JobQueueMessage("", 1.0F, 1.0F)),
            Arguments.argumentSet("wrong format", new JobQueueMessage("mp4", 1.0F, 1.0F)),
            Arguments.argumentSet("no speed", new JobQueueMessage("mp3", null, 1.0F)),
            Arguments.argumentSet("speed reaches min limit", new JobQueueMessage("mp3", 0.08F, 1.0F)),
            Arguments.argumentSet("speed is NaN", new JobQueueMessage("mp3", Float.NaN, 1.0F)),
            Arguments.argumentSet("speed is inf", new JobQueueMessage("mp3", Float.NEGATIVE_INFINITY, 1.0F)),
            Arguments.argumentSet("speed reaches max limit", new JobQueueMessage("mp3", 3.1F, 1.0F)),
            Arguments.argumentSet("pitchSemitones reaches min limit", new JobQueueMessage("mp3", 1.0F, -12.1F)),
            Arguments.argumentSet("no pitchSemitones", new JobQueueMessage("mp3", 1.0F, null)),
            Arguments.argumentSet("pitchSemitones is NaN", new JobQueueMessage("mp3", 1.0F, Float.NaN)),
            Arguments.argumentSet("pitchSemitones is inf", new JobQueueMessage("mp3", 1.0F, Float.POSITIVE_INFINITY)),
            Arguments.argumentSet("pitchSemitones reaches max limit", new JobQueueMessage("mp3", 1.0F, 12.1F))
        );
    }
}