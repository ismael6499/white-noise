package com.agustin.whitenoise.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.agustin.whitenoise.R;
import com.agustin.whitenoise.model.Preset;
import com.agustin.whitenoise.model.SoundCatalog;
import com.agustin.whitenoise.service.AudioPlayerService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Exported control & state provider so SleepRemote can read live White Noise state,
 * retrieve built-in + custom presets, and dispatch playback commands reliably.
 */
public class WhiteNoiseControlProvider extends ContentProvider {

    public static final String AUTHORITY = "com.agustin.whitenoise.control";
    public static final String METHOD_GET_STATE = "get_state";
    public static final String METHOD_COMMAND = "command";

    public static final String KEY_OK = "ok";
    public static final String KEY_NEEDS_FOREGROUND_LAUNCH = "needs_foreground_launch";
    public static final String KEY_PRESETS_JSON = "presets_json";

    private static final Gson GSON = new Gson();

    private static class PresetSummary {
        String id;
        String name;
        boolean custom;

        PresetSummary(String id, String name, boolean custom) {
            this.id = id;
            this.name = name;
            this.custom = custom;
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        Context context = getContext();
        if (context == null) return null;

        if (METHOD_GET_STATE.equals(method)) {
            return buildStateBundle(context);
        }

        if (METHOD_COMMAND.equals(method)) {
            return executeCommand(context, arg, extras);
        }

        return null;
    }

    private Bundle buildStateBundle(Context context) {
        Bundle out = new Bundle();
        AudioPlayerService service = AudioPlayerService.getInstance();
        if (service != null) {
            out.putBoolean(AudioPlayerService.EXTRA_STATE_PLAYING, service.isPlaying());
            out.putBoolean(AudioPlayerService.EXTRA_STATE_HAS_ACTIVE, service.hasActiveTracks());
            out.putInt(AudioPlayerService.EXTRA_STATE_ACTIVE_COUNT, service.getActiveSoundsWithVolume().size());
            out.putString(AudioPlayerService.EXTRA_STATE_SUMMARY, service.getActiveSoundsSummary());
            out.putString(AudioPlayerService.EXTRA_STATE_PRESET_ID,
                    service.getActivePresetId() != null ? service.getActivePresetId() : "");
            out.putBoolean(AudioPlayerService.EXTRA_STATE_TIMER_ACTIVE, service.isTimerActive());
            out.putLong(AudioPlayerService.EXTRA_STATE_TIMER_MS, service.getTimerRemainingMs());
            out.putInt(AudioPlayerService.EXTRA_STATE_MASTER_VOLUME, service.getMasterVolume());
        } else {
            out.putBoolean(AudioPlayerService.EXTRA_STATE_PLAYING, false);
            out.putBoolean(AudioPlayerService.EXTRA_STATE_HAS_ACTIVE, false);
            out.putInt(AudioPlayerService.EXTRA_STATE_ACTIVE_COUNT, 0);
            out.putString(AudioPlayerService.EXTRA_STATE_SUMMARY, context.getString(R.string.active_sounds_none));
            out.putString(AudioPlayerService.EXTRA_STATE_PRESET_ID, "");
            out.putBoolean(AudioPlayerService.EXTRA_STATE_TIMER_ACTIVE, false);
            out.putLong(AudioPlayerService.EXTRA_STATE_TIMER_MS, 0L);
            out.putInt(AudioPlayerService.EXTRA_STATE_MASTER_VOLUME, AudioPlayerService.loadMasterVolume(context));
        }
        out.putString(KEY_PRESETS_JSON, GSON.toJson(buildPresetsList(context)));
        return out;
    }

    private List<PresetSummary> buildPresetsList(Context context) {
        List<PresetSummary> list = new ArrayList<>();
        for (Preset p : SoundCatalog.getBuiltInPresets()) {
            String name = p.getNameResId() != 0 ? context.getString(p.getNameResId()) : p.getName();
            list.add(new PresetSummary(p.getId(), name != null ? name : p.getId(), false));
        }

        SharedPreferences prefs = context.getSharedPreferences(AudioPlayerService.PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(AudioPlayerService.KEY_CUSTOM_PRESETS, null);
        if (json != null && !json.isEmpty()) {
            try {
                Type type = new TypeToken<List<Preset>>() {}.getType();
                List<Preset> custom = GSON.fromJson(json, type);
                if (custom != null) {
                    for (Preset p : custom) {
                        if (p != null && p.getId() != null && p.getName() != null && !p.getName().trim().isEmpty()) {
                            list.add(new PresetSummary(p.getId(), p.getName().trim(), true));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    private Bundle executeCommand(Context context, @Nullable String action, @Nullable Bundle extras) {
        Bundle out = new Bundle();
        if (!isAllowedAction(action)) {
            out.putBoolean(KEY_OK, false);
            return out;
        }

        if (AudioPlayerService.ACTION_SET_STOP_ON_DISMISS.equals(action)) {
            boolean enabled = extras != null && extras.getBoolean(AudioPlayerService.EXTRA_ENABLED, false);
            AudioPlayerService.saveStopOnDismiss(context, enabled);
            out.putBoolean(KEY_OK, true);
            return out;
        }

        AudioPlayerService running = AudioPlayerService.getInstance();
        if (running == null) {
            if (AudioPlayerService.ACTION_PAUSE.equals(action)
                    || AudioPlayerService.ACTION_STOP.equals(action)
                    || AudioPlayerService.ACTION_CANCEL_TIMER.equals(action)) {
                out.putBoolean(KEY_OK, true);
                return out;
            }
            if (AudioPlayerService.ACTION_SET_MASTER_VOLUME.equals(action)) {
                int vol = extras != null ? extras.getInt(AudioPlayerService.EXTRA_VOLUME, 100) : 100;
                AudioPlayerService.saveMasterVolume(context, vol);
                AudioPlayerService.notifyExternalStateChanged(context);
                out.putBoolean(KEY_OK, true);
                return out;
            }
            if (AudioPlayerService.ACTION_ADJUST_MASTER_VOLUME.equals(action)) {
                int delta = extras != null ? extras.getInt(AudioPlayerService.EXTRA_DELTA, 0) : 0;
                int current = AudioPlayerService.loadMasterVolume(context);
                AudioPlayerService.saveMasterVolume(context, current + delta);
                AudioPlayerService.notifyExternalStateChanged(context);
                out.putBoolean(KEY_OK, true);
                return out;
            }
        }

        Intent serviceIntent = new Intent(context, AudioPlayerService.class);
        serviceIntent.setAction(action);
        if (extras != null) {
            serviceIntent.putExtras(extras);
        }

        try {
            if (running != null) {
                context.startService(serviceIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            out.putBoolean(KEY_OK, true);
            out.putBoolean(KEY_NEEDS_FOREGROUND_LAUNCH, false);
        } catch (RuntimeException e) {
            out.putBoolean(KEY_OK, false);
            out.putBoolean(KEY_NEEDS_FOREGROUND_LAUNCH, true);
        }
        return out;
    }

    public static boolean isAllowedAction(@Nullable String action) {
        return AudioPlayerService.ACTION_PLAY.equals(action)
                || AudioPlayerService.ACTION_PAUSE.equals(action)
                || AudioPlayerService.ACTION_STOP.equals(action)
                || AudioPlayerService.ACTION_TOGGLE.equals(action)
                || AudioPlayerService.ACTION_PLAY_PRESET.equals(action)
                || AudioPlayerService.ACTION_SET_TRACK.equals(action)
                || AudioPlayerService.ACTION_SET_TIMER.equals(action)
                || AudioPlayerService.ACTION_CANCEL_TIMER.equals(action)
                || AudioPlayerService.ACTION_SET_MASTER_VOLUME.equals(action)
                || AudioPlayerService.ACTION_ADJUST_MASTER_VOLUME.equals(action)
                || AudioPlayerService.ACTION_SET_STOP_ON_DISMISS.equals(action);
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        return 0;
    }
}
