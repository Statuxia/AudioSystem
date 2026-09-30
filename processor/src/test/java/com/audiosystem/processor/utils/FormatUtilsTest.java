package com.audiosystem.processor.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormatUtilsTest {

    @Test
    void testGetContentTypeKnownFormats() {
        assertEquals("audio/mpeg", FormatUtils.getContentType("mp3"));
        assertEquals("audio/wav", FormatUtils.getContentType("wav"));
        assertEquals("audio/mp4", FormatUtils.getContentType("m4a"));
        assertEquals("audio/aiff", FormatUtils.getContentType("aif"));
    }

    @Test
    void testGetContentTypeUnknownFormatFallsBackToDefault() {
        assertEquals("audio/mpeg", FormatUtils.getContentType("unknown"));
    }

    @Test
    void testGetMuxerKnownFormats() {
        assertEquals("mp3", FormatUtils.getMuxer("mp3"));
        assertEquals("ipod", FormatUtils.getMuxer("m4a"));
        assertEquals("adts", FormatUtils.getMuxer("aac"));
        assertEquals("aiff", FormatUtils.getMuxer("aif"));
        assertEquals("aiff", FormatUtils.getMuxer("aiff"));
    }

    @Test
    void testGetMuxerUnknownFormatFallsBackToDefault() {
        assertEquals("mp3", FormatUtils.getMuxer("unknown"));
    }

    @Test
    void testSupportsCoverArt() {
        assertTrue(FormatUtils.supportsCoverArt("mp3"));
        assertTrue(FormatUtils.supportsCoverArt("m4a"));
        assertTrue(FormatUtils.supportsCoverArt("flac"));

        assertFalse(FormatUtils.supportsCoverArt("wav"));
        assertFalse(FormatUtils.supportsCoverArt("ogg"));
        assertFalse(FormatUtils.supportsCoverArt("opus"));
        assertFalse(FormatUtils.supportsCoverArt("aac"));
        assertFalse(FormatUtils.supportsCoverArt("webm"));
        assertFalse(FormatUtils.supportsCoverArt("aiff"));
        assertFalse(FormatUtils.supportsCoverArt("aif"));
        assertFalse(FormatUtils.supportsCoverArt("unknown"));
    }
}
