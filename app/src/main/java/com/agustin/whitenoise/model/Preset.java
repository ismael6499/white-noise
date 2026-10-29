package com.agustin.whitenoise.model;

import java.util.HashMap;
import java.util.Map;

public class Preset {
    private String id;
    private String name;
    private Map<String, Integer> soundVolumes = new HashMap<>();

    public Preset() {
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
