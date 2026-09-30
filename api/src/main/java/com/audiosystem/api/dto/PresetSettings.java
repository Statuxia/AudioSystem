package com.audiosystem.api.dto;

public record PresetSettings(
    String mode,
    Float speed,
    Float pitchSemitones
) {
}
