package com.audiosystem.processor.dto;


import java.nio.file.Path;

public record UploadFileDTO(
    String contentType,
    String contentDisposition,
    Path resultFilePath
) {
}
