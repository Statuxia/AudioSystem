package com.soundservice.processor.utils;

import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class ContentTypeUtils {

    private static final Map<String, String> FORMAT_CONTENT_TYPE_MAP = new HashMap<>();
    private static final String DEFAULT_CONTENT_TYPE = "audio/mpeg";

    static {
        FORMAT_CONTENT_TYPE_MAP.put("mp3", DEFAULT_CONTENT_TYPE);
        FORMAT_CONTENT_TYPE_MAP.put("wav", "audio/wav");
        FORMAT_CONTENT_TYPE_MAP.put("ogg", "audio/ogg");
        FORMAT_CONTENT_TYPE_MAP.put("opus", "audio/opus");
        FORMAT_CONTENT_TYPE_MAP.put("m4a", "audio/mp4");
        FORMAT_CONTENT_TYPE_MAP.put("aac", "audio/aac");
        FORMAT_CONTENT_TYPE_MAP.put("flac", "audio/flac");
        FORMAT_CONTENT_TYPE_MAP.put("webm", "audio/webm");
        FORMAT_CONTENT_TYPE_MAP.put("mid", "audio/midi");
        FORMAT_CONTENT_TYPE_MAP.put("midi", "audio/midi");
        FORMAT_CONTENT_TYPE_MAP.put("aiff", "audio/aiff");
        FORMAT_CONTENT_TYPE_MAP.put("aif", "audio/aiff");
        FORMAT_CONTENT_TYPE_MAP.put("amr", "audio/amr");
    }

    private ContentTypeUtils() {
    }

    public static String getContentType(String format) {
        final String contentType = FORMAT_CONTENT_TYPE_MAP.get(format);
        if (contentType == null) {
            log.warn("unknown format: {}", format);
            return DEFAULT_CONTENT_TYPE;
        }
        return contentType;
    }
}
