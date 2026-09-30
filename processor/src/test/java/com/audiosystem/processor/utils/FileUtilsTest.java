package com.audiosystem.processor.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {

    @Test
    void testBuildPath() {
        assertEquals(Path.of("/tmp/key.mp3"), FileUtils.buildPath("key", "mp3"));
    }

    @Test
    void testBuildSrcPath() {
        assertEquals(Path.of("/tmp/key"), FileUtils.buildSrcPath("key"));
    }

    @Test
    void testDeleteRemovesExistingResultFile() throws IOException {
        final UUID key = UUID.randomUUID();
        final Path path = FileUtils.buildPath(key.toString(), "mp3");
        Files.createFile(path);

        FileUtils.delete(key, "mp3");

        assertFalse(Files.exists(path));
    }

    @Test
    void testDeleteSrcRemovesExistingSourceFile() throws IOException {
        final UUID key = UUID.randomUUID();
        final Path path = FileUtils.buildSrcPath(key.toString());
        Files.createFile(path);

        FileUtils.deleteSrc(key);

        assertFalse(Files.exists(path));
    }

    @Test
    void testDeleteOnMissingFileDoesNotThrow() {
        final UUID key = UUID.randomUUID();

        assertDoesNotThrow(() -> FileUtils.delete(key, "mp3"));
        assertDoesNotThrow(() -> FileUtils.deleteSrc(key));
    }
}
