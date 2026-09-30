package com.audiosystem.processor.utils;

import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class ContentDispositionUtils {

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\r\\n\\x00-\\x1F]");

    private ContentDispositionUtils() {
    }

    public static String getContentDisposition(String filename, String format, String defaultName) {
        if (!StringUtils.hasText(filename)) {
            filename = defaultName;
        }

        final String cleanName = processFilename(filename, format);

        return "attachment; filename=\"%s\"; filename*=UTF-8''%s".formatted(
            asciiSupported(cleanName), encoded(cleanName)
        );
    }

    private static String processFilename(String raw, String format) {
        raw = URLDecoder.decode(raw, StandardCharsets.UTF_8);
        return CONTROL_CHARS.matcher(raw).replaceAll("") + "." + format;
    }

    private static String asciiSupported(String raw) {
        return raw.replaceAll("[^\\x20-\\x7E]", "_")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    private static String encoded(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
