package com.agustin.whitenoise.model;

import com.agustin.whitenoise.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SoundCatalog {

    // Presets Constants
    public static final String PRESET_SAILING_SEA = "sailing_on_sea";
    public static final String PRESET_SUMMER_NIGHT = "summer_night";
    public static final String PRESET_IN_FOREST = "in_forest";
    public static final String PRESET_RAINING_FROGS = "raining_frogs";
    public static final String PRESET_RAIN = "rain";
    public static final String PRESET_FIREPLACE = "fireplace";
    public static final String PRESET_WIND_CHIMES = "wind_chimes";
    public static final String PRESET_WHITE_NOISE = "white_noise";
    public static final String PRESET_PINK_NOISE = "pink_noise";
    public static final String PRESET_BROWN_NOISE = "brown_noise";
    public static final String PRESET_SEA_SIDE = "sea_side";
    public static final String PRESET_DEEP_FOCUS = "deep_focus";
    public static final String PRESET_LULLABY = "lullaby";

    private static final Map<String, SoundItem> SOUNDS_MAP = new LinkedHashMap<>();
    private static final Map<String, Preset> PRESETS_MAP = new LinkedHashMap<>();

    static {
        // --- 31 Sounds Catalog ---
        registerSound("sailing", R.string.sound_sailing, R.drawable.sailing, R.raw.sailing);
        registerSound("sea_side", R.string.sound_sea_side, R.drawable.sea_side, R.raw.sea_side);
        registerSound("rain", R.string.sound_rain, R.drawable.rain, R.raw.rain);
        registerSound("thunder", R.string.sound_thunder, R.drawable.thunder, R.raw.thunder);
        registerSound("wind", R.string.sound_wind, R.drawable.wind, R.raw.wind);
        registerSound("water_stream", R.string.sound_water_stream, R.drawable.water_stream, R.raw.water_stream);
        registerSound("fireplace", R.string.sound_fireplace, R.drawable.fireplace, R.raw.fireplace);
        registerSound("leaves", R.string.sound_leaves, R.drawable.leaves, R.raw.leaves);
        registerSound("fountain", R.string.sound_fountain, R.drawable.fountain, R.raw.fountain);
        registerSound("shower", R.string.sound_shower, R.drawable.shower, R.raw.shower);
        registerSound("summer_night", R.string.sound_summer_night, R.drawable.summer_night, R.raw.summer_night);
        registerSound("cicadas", R.string.sound_cicadas, R.drawable.cicadas, R.raw.cicadas);
        registerSound("bird", R.string.sound_bird, R.drawable.bird, R.raw.bird);
        registerSound("frogs", R.string.sound_frogs, R.drawable.frogs, R.raw.frogs);
        registerSound("whales", R.string.sound_whales, R.drawable.whales, R.raw.whales);
        registerSound("wolves", R.string.sound_wolves, R.drawable.wolves, R.raw.wolves);
        registerSound("white_noise", R.string.sound_white_noise, R.drawable.white_noise, R.raw.white_noise);
        registerSound("pink_noise", R.string.sound_pink_noise, R.drawable.pink_noise, R.raw.pink_noise);
        registerSound("brown_noise", R.string.sound_brown_noise, R.drawable.brown_noise, R.raw.brown_noise);
        registerSound("fan", R.string.sound_fan, R.drawable.fan, R.raw.fan);
        registerSound("music_box", R.string.sound_music_box, R.drawable.music_box, R.raw.music_box);
        registerSound("piano", R.string.sound_piano, R.drawable.piano, R.raw.piano);
        registerSound("wind_chimes", R.string.sound_wind_chimes, R.drawable.wind_chimes, R.raw.wind_chimes);
        registerSound("train", R.string.sound_train, R.drawable.train, R.raw.train);
        registerSound("car", R.string.sound_car, R.drawable.car, R.raw.car);
        registerSound("crowd", R.string.sound_crowd, R.drawable.crowd, R.raw.crowd);
        registerSound("keyboard", R.string.sound_keyboard, R.drawable.keyboard, R.raw.keyboard);
        registerSound("washing_machine", R.string.sound_washing_machine, R.drawable.washing_machine, R.raw.washing_machine);
        registerSound("vacuum_cleaner", R.string.sound_vacuum_cleaner, R.drawable.vacuum_cleaner, R.raw.vacuum_cleaner);
        registerSound("hair_dryer", R.string.sound_hair_dryer, R.drawable.hair_dryer, R.raw.hair_dryer);
        registerSound("shush", R.string.sound_shush, R.drawable.shush, R.raw.shush);

        // --- All Original App Presets & Templates ---
        registerPreset(new Preset(PRESET_SAILING_SEA, R.string.preset_sailing_on_sea, R.drawable.sailing)
                .addSound("sailing", 80)
                .addSound("sea_side", 65));

        registerPreset(new Preset(PRESET_SUMMER_NIGHT, R.string.preset_summer_night, R.drawable.summer_night)
                .addSound("summer_night", 80)
                .addSound("cicadas", 55)
                .addSound("frogs", 35)
                .addSound("wind", 30));

        registerPreset(new Preset(PRESET_IN_FOREST, R.string.preset_in_forest, R.drawable.water_stream)
                .addSound("water_stream", 75)
                .addSound("bird", 65)
                .addSound("leaves", 50)
                .addSound("wind", 35));

        registerPreset(new Preset(PRESET_RAINING_FROGS, R.string.preset_raining_frogs, R.drawable.frogs)
                .addSound("rain", 75)
                .addSound("frogs", 60)
                .addSound("thunder", 40));

        registerPreset(new Preset(PRESET_RAIN, R.string.preset_rain, R.drawable.rain)
                .addSound("rain", 80)
                .addSound("thunder", 50)
                .addSound("wind", 35));

        registerPreset(new Preset(PRESET_FIREPLACE, R.string.preset_fireplace, R.drawable.fireplace)
                .addSound("fireplace", 85)
                .addSound("wind", 40));

        registerPreset(new Preset(PRESET_WIND_CHIMES, R.string.preset_wind_chimes, R.drawable.wind_chimes)
                .addSound("wind_chimes", 80)
                .addSound("wind", 55)
                .addSound("water_stream", 45));

        registerPreset(new Preset(PRESET_WHITE_NOISE, R.string.preset_white_noise, R.drawable.white_noise)
                .addSound("white_noise", 75)
                .addSound("fan", 50));

        registerPreset(new Preset(PRESET_PINK_NOISE, R.string.preset_pink_noise, R.drawable.pink_noise)
                .addSound("pink_noise", 75)
                .addSound("fan", 50));

        registerPreset(new Preset(PRESET_BROWN_NOISE, R.string.preset_brown_noise, R.drawable.brown_noise)
                .addSound("brown_noise", 80)
                .addSound("fan", 45));

        registerPreset(new Preset(PRESET_SEA_SIDE, R.string.preset_sea_side, R.drawable.sea_side)
                .addSound("sea_side", 85)
                .addSound("wind", 40)
                .addSound("whales", 30));

        registerPreset(new Preset(PRESET_DEEP_FOCUS, R.string.preset_deep_focus, R.drawable.keyboard)
                .addSound("keyboard", 60)
                .addSound("crowd", 45)
                .addSound("rain", 40));

        registerPreset(new Preset(PRESET_LULLABY, R.string.preset_lullaby, R.drawable.music_box)
                .addSound("music_box", 85)
                .addSound("wind_chimes", 40));
    }

    private static void registerSound(String id, int nameResId, int iconResId, int rawResId) {
        SOUNDS_MAP.put(id, new SoundItem(id, nameResId, iconResId, rawResId));
    }

    private static void registerPreset(Preset preset) {
        PRESETS_MAP.put(preset.getId(), preset);
    }

    public static List<SoundItem> createFreshSoundList() {
        List<SoundItem> list = new ArrayList<>();
        for (SoundItem item : SOUNDS_MAP.values()) {
            list.add(new SoundItem(item.getId(), item.getNameResId(), item.getIconResId(), item.getRawResId()));
        }
        return list;
    }

    public static SoundItem getSoundById(String id) {
        return SOUNDS_MAP.get(id);
    }

    public static Map<String, SoundItem> getAllSoundsMap() {
        return Collections.unmodifiableMap(SOUNDS_MAP);
    }

    public static List<Preset> getBuiltInPresets() {
        return new ArrayList<>(PRESETS_MAP.values());
    }

    public static Preset getBuiltInPreset(String presetId) {
        // Also support aliases (like "sailing_sea", "cozy_rain", etc.)
        if ("sailing_sea".equalsIgnoreCase(presetId)) {
            presetId = PRESET_SAILING_SEA;
        } else if ("cozy_rain".equalsIgnoreCase(presetId)) {
            presetId = PRESET_RAIN;
        } else if ("cabin_fire".equalsIgnoreCase(presetId)) {
            presetId = PRESET_FIREPLACE;
        }
        return PRESETS_MAP.get(presetId);
    }
}
