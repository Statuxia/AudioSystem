package com.audiosystem.wsgateway.validator;

import com.audiosystem.wsgateway.dto.JobResultMessage;
import com.audiosystem.wsgateway.dto.JobStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultMessageValidatorTest {

    private final ResultMessageValidator resultMessageValidator = new ResultMessageValidator();

    @ParameterizedTest
    @MethodSource("validMessageDataSource")
    void validateValid(JobResultMessage message) {
        final MapBindingResult errors = new MapBindingResult(new HashMap<>(), JobResultMessage.class.getSimpleName());

        resultMessageValidator.validate(message, errors);

        assertFalse(errors.hasErrors());
    }

    @ParameterizedTest
    @MethodSource("invalidMessageDataSource")
    void validateInvalid(JobResultMessage message) {
        final MapBindingResult errors = new MapBindingResult(new HashMap<>(), JobResultMessage.class.getSimpleName());

        resultMessageValidator.validate(message, errors);

        assertTrue(errors.hasErrors());
    }

    private static Stream<Arguments> validMessageDataSource() {
        return Stream.of(
            Arguments.argumentSet("done status", new JobResultMessage(JobStatus.DONE, 1L)),
            Arguments.argumentSet("error status", new JobResultMessage(JobStatus.ERROR, 0L))
        );
    }

    private static Stream<Arguments> invalidMessageDataSource() {
        return Stream.of(
            Arguments.argumentSet("no status", new JobResultMessage(null, 1L)),
            Arguments.argumentSet("not terminal status", new JobResultMessage(JobStatus.IN_QUEUE, 1L)),
            Arguments.argumentSet("no expireAt", new JobResultMessage(JobStatus.DONE, null)),
            Arguments.argumentSet("expireAt is negative", new JobResultMessage(JobStatus.DONE, -1L))
        );
    }
}