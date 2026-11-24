package com.agustin.whitenoise.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.agustin.whitenoise.MainActivity;
import com.agustin.whitenoise.R;
import com.agustin.whitenoise.WhiteNoiseApp;
import com.agustin.whitenoise.model.Preset;
import com.agustin.whitenoise.model.SoundCatalog;
import com.agustin.whitenoise.model.SoundItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AudioPlayerService extends Service {

    public static final String ACTION_PLAY = "com.agustin.whitenoise.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.agustin.whitenoise.ACTION_PAUSE";
    public static final String ACTION_STOP = "com.agustin.whitenoise.ACTION_STOP";
    public static final String ACTION_TOGGLE = "com.agustin.whitenoise.ACTION_TOGGLE";
    public static final String ACTION_PLAY_PRESET = "com.agustin.whitenoise.ACTION_PLAY_PRESET";
    public static final String ACTION_SET_TRACK = "com.agustin.whitenoise.ACTION_SET_TRACK";
    public static final String ACTION_SET_TIMER = "com.agustin.whitenoise.ACTION_SET_TIMER";
    public static final String ACTION_CANCEL_TIMER = "com.agustin.whitenoise.ACTION_CANCEL_TIMER";
    public static final String ACTION_SET_MASTER_VOLUME = "com.agustin.whitenoise.ACTION_SET_MASTER_VOLUME";
    public static final String ACTION_ADJUST_MASTER_VOLUME = "com.agustin.whitenoise.ACTION_ADJUST_MASTER_VOLUME";
    public static final String ACTION_SET_STOP_ON_DISMISS = "com.agustin.whitenoise.ACTION_SET_STOP_ON_DISMISS";

    public static final String ACTION_SLEEP_ALARM_DISMISS = "com.urbandroid.sleep.alarmclock.ALARM_ALERT_DISMISS";

    public static final String BROADCAST_STATE_CHANGED = "com.agustin.whitenoise.STATE_CHANGED";
    public static final String SLEEP_REMOTE_PACKAGE = "com.nitsutech.sleepremote";

    public static final String PREFS_NAME = "WhiteNoisePrefs";
    public static final String KEY_CUSTOM_PRESETS = "custom_presets";
    public static final String KEY_MASTER_VOLUME = "master_volume";
    public static final String KEY_STOP_ON_DISMISS = "stop_on_sleep_dismiss";

    public static final String EXTRA_TRACK = "track";
    public static final String EXTRA_ENABLED = "enabled";
    public static final String EXTRA_VOLUME = "volume";
    public static final String EXTRA_DELTA = "delta";
    public static final String EXTRA_PRESET = "preset";
    public static final String EXTRA_MINUTES = "minutes";

    public static final String EXTRA_STATE_PLAYING = "is_playing";
    public static final String EXTRA_STATE_HAS_ACTIVE = "has_active_tracks";
    public static final String EXTRA_STATE_ACTIVE_COUNT = "active_count";
    public static final String EXTRA_STATE_SUMMARY = "active_summary";
    public static final String EXTRA_STATE_PRESET_ID = "active_preset_id";
    public static final String EXTRA_STATE_TIMER_MS = "timer_ms";
    public static final String EXTRA_STATE_TIMER_ACTIVE = "timer_active";
    public static final String EXTRA_STATE_MASTER_VOLUME = "master_volume";

    private static final int NOTIFICATION_ID = 101;
    private static final long FADEOUT_DURATION_MS = 10_000L;

    private static volatile AudioPlayerService activeInstance;

    private final IBinder binder = new LocalBinder();
    private final Map<String, SmoothAudioTrack> activeTracks = new HashMap<>();
    private final Map<String, Integer> volumes = new HashMap<>();

    private boolean isPlaying = false;
    private int masterVolume = 100;
    private String activePresetId = "";
    private CountDownTimer sleepTimer;
    private long timerRemainingMs = 0;
    private long timerTotalMs = 0;
    private boolean isTimerActive = false;
    private boolean wasAlarmAudioPlaying = false;
    private AudioManager audioManager;
    private AudioManager.AudioPlaybackCallback alarmPlaybackCallback;

    private final BroadcastReceiver sleepDismissReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && ACTION_SLEEP_ALARM_DISMISS.equals(intent.getAction())) {
                if (loadStopOnDismiss(AudioPlayerService.this) && (isPlaying || !activeTracks.isEmpty())) {
                    stopAll();
                }
            }
        }
    };

    @Nullable
    public static AudioPlayerService getInstance() {
        return activeInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        activeInstance = this;
        masterVolume = 100;
        saveMasterVolume(this, 100);
        ContextCompat.registerReceiver(
                this,
                sleepDismissReceiver,
                new IntentFilter(ACTION_SLEEP_ALARM_DISMISS),
                ContextCompat.RECEIVER_EXPORTED
        );
        registerAlarmAudioMonitor();
    }

    private void registerAlarmAudioMonitor() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;
        alarmPlaybackCallback = new AudioManager.AudioPlaybackCallback() {
            @Override
            public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                boolean alarmActive = false;
                if (configs != null) {
                    for (AudioPlaybackConfiguration cfg : configs) {
                        if (cfg != null && cfg.getAudioAttributes() != null
                                && cfg.getAudioAttributes().getUsage() == AudioAttributes.USAGE_ALARM) {
                            alarmActive = true;
                            break;
                        }
                    }
                }
                if (alarmActive && !wasAlarmAudioPlaying && isPlaying) {
                    pauseAll();
                }
                wasAlarmAudioPlaying = alarmActive;
            }
        };
        audioManager.registerAudioPlaybackCallback(alarmPlaybackCallback, new Handler(Looper.getMainLooper()));
    }

    public class LocalBinder extends Binder {
        public AudioPlayerService getService() {
            return AudioPlayerService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handleAction(intent);
        }
        return START_NOT_STICKY;
    }

    private void handleAction(Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case ACTION_PLAY:
                resumeOrStartAll();
                break;
            case ACTION_PAUSE:
                pauseAll();
                break;
            case ACTION_STOP:
                stopAll();
                break;
            case ACTION_TOGGLE:
                if (isPlaying) {
                    pauseAll();
                } else {
                    resumeOrStartAll();
                }
                break;
            case ACTION_PLAY_PRESET:
                String presetId = intent.getStringExtra(EXTRA_PRESET);
                if (presetId != null) {
                    loadPresetAndPlay(presetId);
                }
                break;
            case ACTION_SET_TRACK:
                String trackId = intent.getStringExtra(EXTRA_TRACK);
                if (trackId != null) {
                    boolean enabled = intent.getBooleanExtra(EXTRA_ENABLED, true);
                    int vol = intent.getIntExtra(EXTRA_VOLUME, volumes.getOrDefault(trackId, 70));
                    setTrackState(trackId, enabled, vol);
                }
                break;
            case ACTION_SET_TIMER:
                int minutes = intent.getIntExtra(EXTRA_MINUTES, 30);
                startSleepTimer(minutes);
                break;
            case ACTION_CANCEL_TIMER:
                cancelSleepTimer();
                break;
            case ACTION_SET_MASTER_VOLUME:
                int targetVol = intent.getIntExtra(EXTRA_VOLUME, masterVolume);
                setMasterVolume(targetVol);
                break;
            case ACTION_ADJUST_MASTER_VOLUME:
                int delta = intent.getIntExtra(EXTRA_DELTA, 0);
                setMasterVolume(masterVolume + delta);
                break;
            case ACTION_SET_STOP_ON_DISMISS:
                boolean stopOnDismiss = intent.getBooleanExtra(EXTRA_ENABLED, false);
                saveStopOnDismiss(this, stopOnDismiss);
                break;
        }
    }

    public synchronized void setTrackState(String soundId, boolean enabled, int volume) {
        activePresetId = "";
        volumes.put(soundId, volume);
        if (enabled) {
            SmoothAudioTrack track = activeTracks.get(soundId);
            if (track == null) {
                SoundItem item = SoundCatalog.getSoundById(soundId);
                if (item != null) {
                    track = new SmoothAudioTrack(this, soundId, item.getRawResId(), volume);
                    activeTracks.put(soundId, track);
                }
            }
            if (track != null) {
                track.setMasterScale(masterVolume / 100f);
                track.setVolume(volume);
                if (!track.isPlaying()) {
                    track.start();
                }
            }
            isPlaying = true;
        } else {
            SmoothAudioTrack track = activeTracks.remove(soundId);
            if (track != null) {
                track.release();
            }
            if (activeTracks.isEmpty()) {
                isPlaying = false;
            }
        }
        updateForegroundState();
        broadcastState();
    }

    public synchronized void updateTrackVolume(String soundId, int volume) {
        volumes.put(soundId, volume);
        SmoothAudioTrack track = activeTracks.get(soundId);
        if (track != null) {
            track.setVolume(volume);
        }
        broadcastState();
    }

    public synchronized void resumeOrStartAll() {
        if (activeTracks.isEmpty()) {
            loadPresetAndPlay(SoundCatalog.PRESET_SAILING_SEA);
            return;
        }
        for (SmoothAudioTrack track : activeTracks.values()) {
            if (track != null && !track.isPlaying()) {
                track.start();
            }
        }
        isPlaying = true;
        updateForegroundState();
        broadcastState();
    }

    public synchronized void pauseAll() {
        for (SmoothAudioTrack track : activeTracks.values()) {
            if (track != null && track.isPlaying()) {
                track.pause();
            }
        }
        isPlaying = false;
        updateForegroundState();
        broadcastState();
    }

    public synchronized void stopAll() {
        cancelSleepTimer();
        for (SmoothAudioTrack track : activeTracks.values()) {
            if (track != null) {
                track.release();
            }
        }
        activeTracks.clear();
        activePresetId = "";
        isPlaying = false;
        stopForeground(true);
        stopSelf();
        broadcastState();
    }

    public synchronized void loadPresetAndPlay(String presetId) {
        Preset preset = findPresetById(presetId);
        if (preset == null || preset.getSoundVolumes().isEmpty()) {
            return;
        }
        // Stop current sounds
        for (SmoothAudioTrack track : activeTracks.values()) {
            if (track != null) {
                track.release();
            }
        }
        activeTracks.clear();
        activePresetId = preset.getId();

        // Start preset sounds with smooth loop tracks
        for (Map.Entry<String, Integer> entry : preset.getSoundVolumes().entrySet()) {
            String sid = entry.getKey();
            int vol = entry.getValue();
            volumes.put(sid, vol);
            SoundItem item = SoundCatalog.getSoundById(sid);
            if (item != null) {
                SmoothAudioTrack track = new SmoothAudioTrack(this, sid, item.getRawResId(), vol);
                track.setMasterScale(masterVolume / 100f);
                track.start();
                activeTracks.put(sid, track);
            }
        }
        isPlaying = !activeTracks.isEmpty();
        updateForegroundState();
        broadcastState();
    }

    private Preset findPresetById(String presetId) {
        if (presetId == null) return null;
        Preset builtIn = SoundCatalog.getBuiltInPreset(presetId);
        if (builtIn != null) return builtIn;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_CUSTOM_PRESETS, null);
        if (json == null || json.isEmpty()) return null;
        try {
            Type type = new TypeToken<List<Preset>>() {}.getType();
            List<Preset> customList = new Gson().fromJson(json, type);
            if (customList != null) {
                for (Preset p : customList) {
                    if (p != null && presetId.equals(p.getId())) {
                        return p;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public void startSleepTimer(int minutes) {
        cancelSleepTimer();
        if (minutes <= 0) return;

        timerTotalMs = minutes * 60_000L;
        timerRemainingMs = timerTotalMs;
        isTimerActive = true;

        sleepTimer = new CountDownTimer(timerTotalMs, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerRemainingMs = millisUntilFinished;
                if (millisUntilFinished <= FADEOUT_DURATION_MS) {
                    float factor = (float) millisUntilFinished / FADEOUT_DURATION_MS;
                    applyFadeoutFactor(factor);
                }
                broadcastState();
            }

            @Override
            public void onFinish() {
                isTimerActive = false;
                timerRemainingMs = 0;
                stopAll();
            }
        }.start();

        updateForegroundState();
        broadcastState();
    }

    public void cancelSleepTimer() {
        if (sleepTimer != null) {
            sleepTimer.cancel();
            sleepTimer = null;
        }
        isTimerActive = false;
        timerRemainingMs = 0;
        // Restore standard volume in case it was in fadeout
        for (Map.Entry<String, SmoothAudioTrack> entry : activeTracks.entrySet()) {
            entry.getValue().applyExternalFadeFactor(1.0f);
        }
        updateForegroundState();
        broadcastState();
    }

    public synchronized void setMasterVolume(int vol) {
        masterVolume = Math.max(0, Math.min(100, vol));
        saveMasterVolume(this, masterVolume);
        float scale = masterVolume / 100f;
        for (SmoothAudioTrack track : activeTracks.values()) {
            if (track != null) {
                track.setMasterScale(scale);
            }
        }
        broadcastState();
    }

    public static int loadMasterVolume(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return Math.max(0, Math.min(100, prefs.getInt(KEY_MASTER_VOLUME, 100)));
    }

    public static void saveMasterVolume(Context context, int vol) {
        int clamped = Math.max(0, Math.min(100, vol));
        context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(KEY_MASTER_VOLUME, clamped)
                .apply();
    }

    public static boolean loadStopOnDismiss(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_STOP_ON_DISMISS, false);
    }

    public static void saveStopOnDismiss(Context context, boolean enabled) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_STOP_ON_DISMISS, enabled)
                .apply();
    }

    private void applyFadeoutFactor(float factor) {
        for (SmoothAudioTrack track : activeTracks.values()) {
            if (track != null) {
                track.applyExternalFadeFactor(factor);
            }
        }
    }

    private void updateForegroundState() {
        if (activeTracks.isEmpty() && !isPlaying) {
            stopForeground(true);
            return;
        }
        Notification notification = buildNotification();
        startForeground(NOTIFICATION_ID, notification);
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pOpen = PendingIntent.getActivity(
                this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent toggleIntent = new Intent(this, AudioPlayerService.class);
        toggleIntent.setAction(ACTION_TOGGLE);
        PendingIntent pToggle = PendingIntent.getService(
                this, 1, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, AudioPlayerService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent pStop = PendingIntent.getService(
                this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String summary = getActiveSoundsSummary();
        String title = isPlaying ? getString(R.string.notification_playing) : getString(R.string.notification_paused);

        if (isTimerActive && timerRemainingMs > 0) {
            int minutesLeft = (int) Math.ceil((double) timerRemainingMs / 60_000.0);
            summary += " • " + getString(R.string.timer_active_format, minutesLeft);
        }

        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        String playPauseText = isPlaying ? getString(R.string.pause) : getString(R.string.play);

        return new NotificationCompat.Builder(this, WhiteNoiseApp.CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(summary)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentIntent(pOpen)
                .setOngoing(isPlaying)
                .setOnlyAlertOnce(true)
                .addAction(playPauseIcon, playPauseText, pToggle)
                .addAction(R.drawable.ic_stop, getString(R.string.stop), pStop)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1))
                .build();
    }

    public String getActiveSoundsSummary() {
        if (activeTracks.isEmpty()) {
            return getString(R.string.active_sounds_none);
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String id : activeTracks.keySet()) {
            SoundItem item = SoundCatalog.getSoundById(id);
            if (item != null) {
                if (count > 0) sb.append(", ");
                sb.append(getString(item.getNameResId()));
                count++;
            }
        }
        return sb.toString();
    }

    public synchronized Map<String, Integer> getActiveSoundsWithVolume() {
        Map<String, Integer> map = new HashMap<>();
        for (String id : activeTracks.keySet()) {
            map.put(id, volumes.getOrDefault(id, 70));
        }
        return map;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean hasActiveTracks() {
        return !activeTracks.isEmpty();
    }

    public String getActivePresetId() {
        return activePresetId;
    }

    public int getMasterVolume() {
        return masterVolume;
    }

    public boolean isTrackActive(String soundId) {
        return activeTracks.containsKey(soundId);
    }

    public int getTrackVolume(String soundId) {
        return volumes.getOrDefault(soundId, 70);
    }

    public boolean isTimerActive() {
        return isTimerActive;
    }

    public long getTimerRemainingMs() {
        return timerRemainingMs;
    }

    private void broadcastState() {
        Intent localIntent = buildStateIntent(getPackageName());
        sendBroadcast(localIntent);

        Intent remoteIntent = buildStateIntent(SLEEP_REMOTE_PACKAGE);
        sendBroadcast(remoteIntent);
    }

    private Intent buildStateIntent(String targetPackage) {
        Intent intent = new Intent(BROADCAST_STATE_CHANGED);
        intent.setPackage(targetPackage);
        intent.putExtra(EXTRA_STATE_PLAYING, isPlaying);
        intent.putExtra(EXTRA_STATE_HAS_ACTIVE, !activeTracks.isEmpty());
        intent.putExtra(EXTRA_STATE_ACTIVE_COUNT, activeTracks.size());
        intent.putExtra(EXTRA_STATE_SUMMARY, getActiveSoundsSummary());
        intent.putExtra(EXTRA_STATE_PRESET_ID, activePresetId != null ? activePresetId : "");
        intent.putExtra(EXTRA_STATE_TIMER_MS, timerRemainingMs);
        intent.putExtra(EXTRA_STATE_TIMER_ACTIVE, isTimerActive);
        intent.putExtra(EXTRA_STATE_MASTER_VOLUME, masterVolume);
        return intent;
    }

    public static void notifyExternalStateChanged(Context context) {
        AudioPlayerService instance = activeInstance;
        if (instance != null) {
            instance.broadcastState();
            return;
        }
        Intent remoteIntent = new Intent(BROADCAST_STATE_CHANGED);
        remoteIntent.setPackage(SLEEP_REMOTE_PACKAGE);
        remoteIntent.putExtra(EXTRA_STATE_PLAYING, false);
        remoteIntent.putExtra(EXTRA_STATE_HAS_ACTIVE, false);
        remoteIntent.putExtra(EXTRA_STATE_ACTIVE_COUNT, 0);
        remoteIntent.putExtra(EXTRA_STATE_SUMMARY, context.getString(R.string.active_sounds_none));
        remoteIntent.putExtra(EXTRA_STATE_PRESET_ID, "");
        remoteIntent.putExtra(EXTRA_STATE_TIMER_MS, 0L);
        remoteIntent.putExtra(EXTRA_STATE_TIMER_ACTIVE, false);
        remoteIntent.putExtra(EXTRA_STATE_MASTER_VOLUME, loadMasterVolume(context));
        context.sendBroadcast(remoteIntent);
    }

    @Override
    public void onDestroy() {
        if (activeInstance == this) {
            activeInstance = null;
        }
        try {
            unregisterReceiver(sleepDismissReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        if (audioManager != null && alarmPlaybackCallback != null) {
            try {
                audioManager.unregisterAudioPlaybackCallback(alarmPlaybackCallback);
            } catch (Exception ignored) {
            }
        }
        stopAll();
        super.onDestroy();
    }
}
