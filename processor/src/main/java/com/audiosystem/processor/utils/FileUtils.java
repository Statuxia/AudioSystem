package com.audiosystem.processor.utils;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Log4j2
public class FileUtils {

    private FileUtils() {
    }

    public static void delete(UUID key, String format) {
        delete(key, buildPath(key.toString(), format));
    }

    public static void deleteSrc(UUID key) {
        delete(key, buildSrcPath(key.toString()));
    }

    public static Path buildPath(String key, String format) {
        return Path.of("/tmp/%s.%s".formatted(key, format));
    }

    public static Path buildSrcPath(String key) {
        return Path.of("/tmp/" + key);
    }

    private static void delete(UUID key, Path path) {
        try {
            log.debug("[{}] deleting file {}", key, path.toString());
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("[{}] failed to delete file {}", key, path.toString(), e);
        }
    }
}
