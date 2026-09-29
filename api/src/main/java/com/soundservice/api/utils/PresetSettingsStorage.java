package com.soundservice.api.utils;

import com.soundservice.api.dto.PresetSettings;

import java.util.List;

public final class PresetSettingsStorage {

    public static final PresetSettings DEFAULT
        = new PresetSettings("DEFAULT", 1.0f, 1.0f);
    public static final PresetSettings NIGHTCORE
        = new PresetSettings("NIGHTCORE", 1.5f, 4.0f);
    public static final PresetSettings DAYCORE
        = new PresetSettings("DAYCORE", 0.75f, -4.0f);
    public static final PresetSettings DOUBLE_TIME
        = new PresetSettings("DOUBLE_TIME", 1.5f, 0.0f);
    public static final PresetSettings HALF_TIME
        = new PresetSettings("HALF_TIME", 0.75f, 0.0f);
    public static final List<PresetSettings> PRESETS = List.of(NIGHTCORE, DAYCORE, DOUBLE_TIME, HALF_TIME);

    private PresetSettingsStorage() {
    }
}
