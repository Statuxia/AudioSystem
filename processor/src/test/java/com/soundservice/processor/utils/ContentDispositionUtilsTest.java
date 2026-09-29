package com.soundservice.processor.utils;

import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentDispositionUtilsTest {

    @Test
    void testValidFilename() {
        final String result = ContentDispositionUtils.getContentDisposition("song", "mp3", "default");

        assertEquals("attachment; filename=\"song.mp3\"; filename*=UTF-8''song.mp3", result);
    }

    @Test
    void testBlankFilenameFallsBackToDefault() {
        assertEquals(
            "attachment; filename=\"default.mp3\"; filename*=UTF-8''default.mp3",
            ContentDispositionUtils.getContentDisposition(null, "mp3", "default")
        );
        assertEquals(
            "attachment; filename=\"default.mp3\"; filename*=UTF-8''default.mp3",
            ContentDispositionUtils.getContentDisposition("   ", "mp3", "default")
        );
    }

    @Test
    void testControlCharsStripped() {
        final String result = ContentDispositionUtils.getContentDisposition("so\nng\r", "mp3", "default");

        assertEquals("attachment; filename=\"song.mp3\"; filename*=UTF-8''song.mp3", result);
    }

    @Test
    void testNonAsciiFilenameEncoded() {
        final String result = ContentDispositionUtils.getContentDisposition("файл", "mp3", "default");

        assertTrue(result.startsWith("attachment; filename=\"____.mp3\"; filename*=UTF-8''"));

        final String encodedPart = result.substring(result.indexOf("UTF-8''") + "UTF-8''".length());
        assertEquals("файл.mp3", URLDecoder.decode(encodedPart, StandardCharsets.UTF_8));
    }

    @Test
    void testQuoteAndBackslashEscapedInAsciiPart() {
        final String result = ContentDispositionUtils.getContentDisposition("a\"b\\c", "mp3", "default");

        assertTrue(result.contains("filename=\"a\\\"b\\\\c.mp3\""));
    }
}
