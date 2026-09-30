package com.audiosystem.processor.dto;


import java.io.InputStream;

public record UploadFileDTO(
    String contentType,
    String contentDisposition,
    InputStream inputStream,
    Long contentLength
) {
}
