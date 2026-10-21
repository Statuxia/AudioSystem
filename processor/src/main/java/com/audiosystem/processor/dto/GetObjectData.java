package com.audiosystem.processor.dto;

import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.file.Path;

public record GetObjectData(Path srcPath, GetObjectResponse response) {
}
