package com.soundservice.processor.utils;

import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Log4j2
public class FormatUtils {

    private static final Map<String, String> FORMAT_CONTENT_TYPE_MAP = new HashMap<>();
    private static final Map<String, String> FORMAT_MUXER_MAP = new HashMap<>();
    private static final Set<String> COVER_ART_SUPPORTED_FORMATS = Set.of("mp3", "m4a", "flac");
    private static final String DEFAULT_CONTENT_TYPE = "audio/mpeg";
    private static final String DEFAULT_MUXER = "mp3";

    static {
        FORMAT_CONTENT_TYPE_MAP.put("mp3", DEFAULT_CONTENT_TYPE);
        FORMAT_CONTENT_TYPE_MAP.put("wav", "audio/wav");
        FORMAT_CONTENT_TYPE_MAP.put("ogg", "audio/ogg");
        FORMAT_CONTENT_TYPE_MAP.put("opus", "audio/opus");
        FORMAT_CONTENT_TYPE_MAP.put("m4a", "audio/mp4");
        FORMAT_CONTENT_TYPE_MAP.put("aac", "audio/aac");
        FORMAT_CONTENT_TYPE_MAP.put("flac", "audio/flac");
        FORMAT_CONTENT_TYPE_MAP.put("webm", "audio/webm");
        FORMAT_CONTENT_TYPE_MAP.put("aiff", "audio/aiff");
        FORMAT_CONTENT_TYPE_MAP.put("aif", "audio/aiff");

        FORMAT_MUXER_MAP.put("mp3", "mp3");
        FORMAT_MUXER_MAP.put("wav", "wav");
        FORMAT_MUXER_MAP.put("ogg", "ogg");
        FORMAT_MUXER_MAP.put("opus", "opus");
        FORMAT_MUXER_MAP.put("m4a", "ipod");
        FORMAT_MUXER_MAP.put("aac", "adts");
        FORMAT_MUXER_MAP.put("flac", "flac");
        FORMAT_MUXER_MAP.put("webm", "webm");
        FORMAT_MUXER_MAP.put("aiff", "aiff");
        FORMAT_MUXER_MAP.put("aif", "aiff");
    }

    private FormatUtils() {
    }

    public static String getContentType(String format) {
        final String contentType = FORMAT_CONTENT_TYPE_MAP.get(format);
        if (contentType == null) {
            log.warn("unknown format: {}", format);
            return DEFAULT_CONTENT_TYPE;
        }
        return contentType;
    }

    public static String getMuxer(String format) {
        final String muxer = FORMAT_MUXER_MAP.get(format);
        if (muxer == null) {
            log.warn("unknown format: {}", format);
            return DEFAULT_MUXER;
        }
        return muxer;
    }

    public static boolean supportsCoverArt(String format) {
        return COVER_ART_SUPPORTED_FORMATS.contains(format);
    }
}
