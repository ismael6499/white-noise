package com.agustin.whitenoise.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import java.util.LinkedHashMap;
import java.util.Map;

public class Preset {
    private String id;
    private @StringRes int nameResId;
    private @DrawableRes int iconResId;
    private String name; // Used for custom user-created presets
    private Map<String, Integer> soundVolumes = new LinkedHashMap<>();

    public Preset() {
    }

    public Preset(String id, @StringRes int nameResId, @DrawableRes int iconResId) {
        this.id = id;
        this.nameResId = nameResId;
        this.iconResId = iconResId;
    }

    public Preset(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getNameResId() {
        return nameResId;
    }

    public void setNameResId(int nameResId) {
        this.nameResId = nameResId;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Integer> getSoundVolumes() {
        return soundVolumes;
    }

    public void setSoundVolumes(Map<String, Integer> soundVolumes) {
        this.soundVolumes = soundVolumes;
    }

    public Preset addSound(String soundId, int volume) {
        this.soundVolumes.put(soundId, volume);
        return this;
    }
}
