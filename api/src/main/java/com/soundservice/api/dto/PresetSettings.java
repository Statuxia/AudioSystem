package com.soundservice.api.dto;

public record PresetSettings(
    String mode,
    Float speed,
    Float pitchSemitones,
    Boolean preservePitch
) {
}
