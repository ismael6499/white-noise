package com.agustin.whitenoise.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;

public class SoundItem {
    private final String id;
    private final @StringRes int nameResId;
    private final @DrawableRes int iconResId;
    private final @RawRes int rawResId;
    private int volume; // 0 to 100
    private boolean isPlaying;

    public SoundItem(String id, @StringRes int nameResId, @DrawableRes int iconResId, @RawRes int rawResId) {
        this(id, nameResId, iconResId, rawResId, 70, false);
    }

    public SoundItem(String id, @StringRes int nameResId, @DrawableRes int iconResId, @RawRes int rawResId, int volume, boolean isPlaying) {
        this.id = id;
        this.nameResId = nameResId;
        this.iconResId = iconResId;
        this.rawResId = rawResId;
        this.volume = volume;
        this.isPlaying = isPlaying;
    }

    public String getId() {
        return id;
    }

    public int getNameResId() {
        return nameResId;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getRawResId() {
        return rawResId;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }
}
