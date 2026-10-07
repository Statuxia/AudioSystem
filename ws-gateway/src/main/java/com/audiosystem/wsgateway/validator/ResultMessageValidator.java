package com.audiosystem.wsgateway.validator;

import com.audiosystem.wsgateway.dto.JobResultMessage;
import com.audiosystem.wsgateway.dto.JobStatus;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class ResultMessageValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return JobResultMessage.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        final JobResultMessage message = (JobResultMessage) target;

        validateTerminalStatus(message.status(), errors);
        validateExpireAt(message.expireAt(), errors);
    }

    private void validateTerminalStatus(JobStatus status, Errors errors) {
        if (status != JobStatus.DONE && status != JobStatus.ERROR) {
            errors.reject("JobResultMessage.error.status.notTerminal", "result status is not terminal");
        }
    }

    private void validateExpireAt(Long expireAt, Errors errors) {
        if (expireAt == null) {
            errors.reject("JobResultMessage.error.expireAt.null", "expireAt is null in message");
            return;
        }
        if (expireAt < 0) {
            errors.reject("JobResultMessage.error.expireAt.lessThanZero", "expireAt should be positive or zero");
        }
    }
}
