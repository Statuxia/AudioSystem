package com.soundservice.processor.service;

import com.soundservice.processor.dto.JobSettings;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AudioProcessorServiceTest {

    private final AudioProcessorService service = new AudioProcessorService();

    @Test
    void testBuildCommandNonCoverArtFormat() {
        final Path srcPath = Path.of("/tmp/src");
        final Path resultPath = Path.of("/tmp/result.wav");
        final JobSettings settings = new JobSettings.Builder()
            .jobId("job-1")
            .format("wav")
            .contentType("audio/wav")
            .inputStream(InputStream.nullInputStream())
            .speed(1.5F)
            .pitchSemitones(4F)
            .build();
        final double pitchFactor = Math.pow(2, settings.getPitchSemitones() / 12.0);

        final String audioFilter = "rubberband=tempo=%s:pitch=%s".formatted(1.5F, pitchFactor);

        final List<String> actual = service.buildCommand(settings, srcPath, resultPath);

        assertEquals(
            List.of(
                "ffmpeg", "-y", "-i", srcPath.toString(),
                "-map", "0:a",
                "-af", audioFilter,
                "-f", "wav",
                resultPath.toString()
            ),
            actual
        );
    }

    @Test
    void testBuildCommandCoverArtFormat() {
        final Path srcPath = Path.of("/tmp/src");
        final Path resultPath = Path.of("/tmp/result.mp3");
        final JobSettings settings = new JobSettings.Builder()
            .jobId("job-2")
            .format("mp3")
            .contentType("audio/mpeg")
            .inputStream(InputStream.nullInputStream())
            .speed(0.75F)
            .pitchSemitones(-4F)
            .build();
        final double pitchFactor = Math.pow(2, settings.getPitchSemitones() / 12.0);

        final String audioFilter = "rubberband=tempo=%s:pitch=%s".formatted(0.75F, pitchFactor);

        final List<String> actual = service.buildCommand(settings, srcPath, resultPath);

        assertEquals(
            List.of(
                "ffmpeg", "-y", "-i", srcPath.toString(),
                "-map", "0:a",
                "-map", "0:v?", "-c:v", "copy",
                "-af", audioFilter,
                "-f", "mp3",
                resultPath.toString()
            ),
            actual
        );
    }
}
