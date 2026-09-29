package com.soundservice.processor.service;

import com.soundservice.processor.dto.JobSettings;
import com.soundservice.processor.exception.AudioProcessorException;
import com.soundservice.processor.utils.FormatUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Log4j2
public class AudioProcessorService {

    public Path process(JobSettings settings) {
        final String jobId = settings.getJobId();
        log.debug("[{}] start processing source file with settings: {}", jobId, settings);

        final Path srcPath = FileUtils.buildSrcPath(jobId);
        final Path resultPath = FileUtils.buildPath(jobId, settings.getFormat());

        try {
            Files.copy(settings.getInputStream(), srcPath, StandardCopyOption.REPLACE_EXISTING);

            final ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(settings, srcPath, resultPath));
            processBuilder.redirectErrorStream(true);

            final Process process = processBuilder.start();

            final List<String> stderr = new ArrayList<>();
            final Thread stderrReader = new Thread(() -> {
                try (BufferedReader reader = process.inputReader()) {
                    reader.lines().forEach(stderr::add);
                } catch (IOException e) {
                    log.warn("[{}] failed reading ffmpeg output", jobId, e);
                }
            });

            stderrReader.setDaemon(true);
            stderrReader.start();

            final boolean processExited = process.waitFor(5, TimeUnit.MINUTES);
            stderrReader.join(Duration.ofSeconds(5));

            if (!processExited) {
                throw new TimeoutException("file processing took longer then expected");
            }
            if (process.exitValue() != 0) {
                throw new AudioProcessorException(
                    "ffmpeg returns with error.\n" + StringUtils.collectionToDelimitedString(stderr, "\n")
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AudioProcessorException("thread interrupted");
        } catch (AudioProcessorException e) {
            throw e; // return as is
        } catch (Exception e) {
            throw new AudioProcessorException("failed to process source file", e);
        }

        return resultPath;
    }

    private List<String> buildCommand(JobSettings settings, Path srcPath, Path resultPath) {
        final double pitchFactor = Math.pow(2, settings.getPitchSemitones() / 12.0);
        final List<String> command = new ArrayList<>();

        command.add("ffmpeg");
        command.add("-y");
        command.add("-i");
        command.add(srcPath.toString());
        command.add("-map");
        command.add("0:a");
        if (FormatUtils.supportsCoverArt(settings.getFormat())) {
            command.add("-map");
            command.add("0:v?");
            command.add("-c:v");
            command.add("copy");
        }
        command.add("-af");
        command.add("rubberband=tempo=%s:pitch=%s".formatted(settings.getSpeed(), pitchFactor));
        command.add("-f");
        command.add(FormatUtils.getMuxer(settings.getFormat()));
        command.add(resultPath.toString());

        return command;
    }
}
