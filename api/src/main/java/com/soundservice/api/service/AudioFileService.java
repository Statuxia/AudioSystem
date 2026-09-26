package com.soundservice.api.service;

import com.soundservice.api.exception.JobCreationException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Service
@Log4j2
public class AudioFileService {

    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of(
        "audio/mpeg",
        "audio/wav",
        "audio/x-wav",
        "audio/ogg",
        "audio/opus",
        "audio/mp4",
        "audio/aac",
        "audio/flac",
        "audio/webm",
        "audio/midi",
        "audio/x-midi",
        "audio/aiff",
        "audio/x-aiff",
        "audio/amr"
    );

    public TikaInputStream getInputStream(@NotNull MultipartFile file) {
        final TikaInputStream inputStream;
        try {
            inputStream = TikaInputStream.get(file.getInputStream());
        } catch (IOException e) {
            throw new JobCreationException("failed to read input stream");
        }

        try {
            inputStream.enableRewind();
            return inputStream;
        } catch (IOException e) {
            log.debug("input stream already read", e);
            throw new JobCreationException("failed to enable rewind for double-read stream");
        }
    }

    public String getContentType(@NotNull TikaInputStream inputStream) {
        final Tika tika = new Tika();
        final String contentType;
        try {
            contentType = tika.detect(inputStream)
                .split(";", 2)[0] // deleting random param like charset=UTF-8
                .trim()
                .toLowerCase(Locale.ROOT);
        } catch (IOException e) {
            log.error("failed to read stream", e);
            throw new JobCreationException("failed to read input stream for content type");
        }

        try {
            inputStream.rewind();
        } catch (IOException e) {
            log.error("can't rewind input stream after process contentType", e);
            throw new JobCreationException("failed to rewind input stream");
        }
        return contentType;
    }

    public boolean isValidAudioType(String contentType) {
        return ALLOWED_AUDIO_TYPES.contains(contentType);
    }

    public String getFileName(@NotNull MultipartFile multipartFile, @NotEmpty String defaultName) {
        String filename = multipartFile.getOriginalFilename();
        log.debug("filename: {}; defaultname: {}", filename, defaultName);

        if (!StringUtils.hasText(filename)) {
            return defaultName;
        }

        int directorySeparatorIndex = filename.lastIndexOf("/");
        if (directorySeparatorIndex != -1) {
            filename = filename.substring(directorySeparatorIndex + 1);
        }

        if (!StringUtils.hasText(filename)) {
            return defaultName;
        }

        final int fileTypeIndex = filename.lastIndexOf(".");
        if (fileTypeIndex == -1) {
            return filename;
        }

        final String cleanName = filename.substring(0, fileTypeIndex);
        return StringUtils.hasText(cleanName) ? cleanName : defaultName;
    }
}
