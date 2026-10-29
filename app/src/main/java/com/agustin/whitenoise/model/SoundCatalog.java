package com.agustin.whitenoise.model;

import com.agustin.whitenoise.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SoundCatalog {

    public static final String PRESET_SAILING_SEA = "sailing_sea";
    public static final String PRESET_COZY_RAIN = "cozy_rain";
    public static final String PRESET_DEEP_FOCUS = "deep_focus";
    public static final String PRESET_MYSTIC_NIGHT = "mystic_night";
    public static final String PRESET_CABIN_FIRE = "cabin_fire";

    private static final Map<String, SoundItem> SOUNDS_MAP = new LinkedHashMap<>();

    static {
        // Priority Top Favorites: Sailing Ship & Ocean Waves
        register("sailing", R.string.sound_sailing, R.drawable.sailing, R.raw.sailing);
        register("sea_side", R.string.sound_sea_side, R.drawable.sea_side, R.raw.sea_side);

        // Nature & Water
        register("rain", R.string.sound_rain, R.drawable.rain, R.raw.rain);
        register("thunder", R.string.sound_thunder, R.drawable.thunder, R.raw.thunder);
        register("wind", R.string.sound_wind, R.drawable.wind, R.raw.wind);
        register("water_stream", R.string.sound_water_stream, R.drawable.water_stream, R.raw.water_stream);
        register("fireplace", R.string.sound_fireplace, R.drawable.fireplace, R.raw.fireplace);
        register("leaves", R.string.sound_leaves, R.drawable.leaves, R.raw.leaves);
        register("fountain", R.string.sound_fountain, R.drawable.fountain, R.raw.fountain);
        register("shower", R.string.sound_shower, R.drawable.shower, R.raw.shower);

        // Night & Wildlife
        register("summer_night", R.string.sound_summer_night, R.drawable.summer_night, R.raw.summer_night);
        register("cicadas", R.string.sound_cicadas, R.drawable.cicadas, R.raw.cicadas);
        register("bird", R.string.sound_bird, R.drawable.bird, R.raw.bird);
        register("frogs", R.string.sound_frogs, R.drawable.frogs, R.raw.frogs);
        register("whales", R.string.sound_whales, R.drawable.whales, R.raw.whales);
        register("wolves", R.string.sound_wolves, R.drawable.wolves, R.raw.wolves);

        // Colors of Noise & Atmosphere
        register("white_noise", R.string.sound_white_noise, R.drawable.white_noise, R.raw.white_noise);
        register("pink_noise", R.string.sound_pink_noise, R.drawable.pink_noise, R.raw.pink_noise);
        register("brown_noise", R.string.sound_brown_noise, R.drawable.brown_noise, R.raw.brown_noise);
        register("fan", R.string.sound_fan, R.drawable.fan, R.raw.fan);

        // Ambient & Relaxing Music
        register("music_box", R.string.sound_music_box, R.drawable.music_box, R.raw.music_box);
        register("piano", R.string.sound_piano, R.drawable.piano, R.raw.piano);
        register("wind_chimes", R.string.sound_wind_chimes, R.drawable.wind_chimes, R.raw.wind_chimes);

        // Urban & Domestic Life
        register("train", R.string.sound_train, R.drawable.train, R.raw.train);
        register("car", R.string.sound_car, R.drawable.car, R.raw.car);
        register("crowd", R.string.sound_crowd, R.drawable.crowd, R.raw.crowd);
        register("keyboard", R.string.sound_keyboard, R.drawable.keyboard, R.raw.keyboard);
        register("washing_machine", R.string.sound_washing_machine, R.drawable.washing_machine, R.raw.washing_machine);
        register("vacuum_cleaner", R.string.sound_vacuum_cleaner, R.drawable.vacuum_cleaner, R.raw.vacuum_cleaner);
        register("hair_dryer", R.string.sound_hair_dryer, R.drawable.hair_dryer, R.raw.hair_dryer);
        register("shush", R.string.sound_shush, R.drawable.shush, R.raw.shush);
    }

    private static void register(String id, int nameResId, int iconResId, int rawResId) {
        SOUNDS_MAP.put(id, new SoundItem(id, nameResId, iconResId, rawResId));
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

    public static Preset getBuiltInPreset(String presetId) {
        Preset preset = new Preset();
        preset.setId(presetId);

        if (PRESET_SAILING_SEA.equalsIgnoreCase(presetId)) {
            preset.setName("Sailing & Ocean");
            preset.addSound("sailing", 80);
            preset.addSound("sea_side", 65);
        } else if (PRESET_COZY_RAIN.equalsIgnoreCase(presetId)) {
            preset.setName("Cozy Rain");
            preset.addSound("rain", 75);
            preset.addSound("thunder", 45);
            preset.addSound("wind", 30);
        } else if (PRESET_DEEP_FOCUS.equalsIgnoreCase(presetId)) {
            preset.setName("Deep Sleep");
            preset.addSound("brown_noise", 60);
            preset.addSound("fan", 50);
        } else if (PRESET_MYSTIC_NIGHT.equalsIgnoreCase(presetId)) {
            preset.setName("Mystic Night");
            preset.addSound("summer_night", 70);
            preset.addSound("cicadas", 40);
            preset.addSound("whales", 35);
        } else if (PRESET_CABIN_FIRE.equalsIgnoreCase(presetId)) {
            preset.setName("Cabin Fireplace");
            preset.addSound("fireplace", 80);
            preset.addSound("wind", 40);
        }
        return preset;
    }
}
