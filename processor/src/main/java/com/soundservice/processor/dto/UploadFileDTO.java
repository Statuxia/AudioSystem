package com.soundservice.processor.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;

@AllArgsConstructor
@Getter
@Setter
public class UploadFileDTO {

    private final String contentType;
    private final String contentDisposition;
    private byte[] bytes;
    private InputStream inputStream;
    private Long contentLength;

    public UploadFileDTO(String contentType, String contentDisposition, byte[] bytes) {
        this(contentType, contentDisposition, bytes, null, null);
    }

    public UploadFileDTO(
        String contentType,
        String contentDisposition,
        InputStream inputStream,
        long contentLength
    ) {
        this(contentType, contentDisposition, null, inputStream, contentLength);
    }
}
