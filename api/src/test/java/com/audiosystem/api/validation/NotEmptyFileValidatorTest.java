package com.audiosystem.api.validation;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotEmptyFileValidatorTest {

    private final NotEmptyFileValidator validator = new NotEmptyFileValidator();

    @Test
    void testNullMultipartFile() {
        assertFalse(validator.isValid(null, null));
    }

    @Test
    void testEmptyMultipartFile() {
        final MockMultipartFile file = new MockMultipartFile("file", new byte[0]);
        assertFalse(validator.isValid(file, null));
    }

    @Test
    void testValidMultipartFile() {
        final MultipartFile file = new MockMultipartFile("file", new byte[]{1, 2, 3});
        assertTrue(validator.isValid(file, null));
    }
}