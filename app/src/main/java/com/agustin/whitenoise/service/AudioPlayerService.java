package com.agustin.whitenoise.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.agustin.whitenoise.MainActivity;
import com.agustin.whitenoise.R;
import com.agustin.whitenoise.WhiteNoiseApp;
import com.agustin.whitenoise.model.Preset;
import com.agustin.whitenoise.model.SoundCatalog;
import com.agustin.whitenoise.model.SoundItem;
import java.util.Collections;
import java.util.HashMap;
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

    public static final String BROADCAST_STATE_CHANGED = "com.agustin.whitenoise.STATE_CHANGED";

    public static final String EXTRA_TRACK = "track";
    public static final String EXTRA_ENABLED = "enabled";
    public static final String EXTRA_VOLUME = "volume";
    public static final String EXTRA_PRESET = "preset";
    public static final String EXTRA_MINUTES = "minutes";

    public static final String EXTRA_STATE_PLAYING = "is_playing";
    public static final String EXTRA_STATE_ACTIVE_COUNT = "active_count";
    public static final String EXTRA_STATE_SUMMARY = "active_summary";
    public static final String EXTRA_STATE_TIMER_MS = "timer_ms";
    public static final String EXTRA_STATE_TIMER_ACTIVE = "timer_active";

    private static final int NOTIFICATION_ID = 101;
    private static final long FADEOUT_DURATION_MS = 10_000L;

    private final IBinder binder = new LocalBinder();
    private final Map<String, MediaPlayer> players = new HashMap<>();
    private final Map<String, Integer> volumes = new HashMap<>();

    private boolean isPlaying = false;
    private CountDownTimer sleepTimer;
    private long timerRemainingMs = 0;
    private long timerTotalMs = 0;
    private boolean isTimerActive = false;

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
        }
    }

    public synchronized void setTrackState(String soundId, boolean enabled, int volume) {
        volumes.put(soundId, volume);
        if (enabled) {
            MediaPlayer mp = players.get(soundId);
            if (mp == null) {
                mp = createPlayer(soundId);
                if (mp != null) {
                    players.put(soundId, mp);
                }
            }
            if (mp != null) {
                applyVolume(mp, volume);
                if (!mp.isPlaying()) {
                    mp.start();
                }
            }
            isPlaying = true;
        } else {
            MediaPlayer mp = players.remove(soundId);
            if (mp != null) {
                try {
                    if (mp.isPlaying()) {
                        mp.stop();
                    }
                    mp.release();
                } catch (Exception ignored) {
                }
            }
            if (players.isEmpty()) {
                isPlaying = false;
            }
        }
        updateForegroundState();
        broadcastState();
    }

    public synchronized void updateTrackVolume(String soundId, int volume) {
        volumes.put(soundId, volume);
        MediaPlayer mp = players.get(soundId);
        if (mp != null) {
            applyVolume(mp, volume);
        }
        broadcastState();
    }

    public synchronized void resumeOrStartAll() {
        if (players.isEmpty()) {
            // Default favorite if nothing is selected: Sailing & Sea Side
            loadPresetAndPlay(SoundCatalog.PRESET_SAILING_SEA);
            return;
        }
        for (Map.Entry<String, MediaPlayer> entry : players.entrySet()) {
            MediaPlayer mp = entry.getValue();
            if (mp != null && !mp.isPlaying()) {
                int vol = volumes.getOrDefault(entry.getKey(), 70);
                applyVolume(mp, vol);
                mp.start();
            }
        }
        isPlaying = true;
        updateForegroundState();
        broadcastState();
    }

    public synchronized void pauseAll() {
        for (MediaPlayer mp : players.values()) {
            if (mp != null && mp.isPlaying()) {
                mp.pause();
            }
        }
        isPlaying = false;
        updateForegroundState();
        broadcastState();
    }

    public synchronized void stopAll() {
        cancelSleepTimer();
        for (MediaPlayer mp : players.values()) {
            if (mp != null) {
                try {
                    if (mp.isPlaying()) {
                        mp.stop();
                    }
                    mp.release();
                } catch (Exception ignored) {
                }
            }
        }
        players.clear();
        isPlaying = false;
        stopForeground(true);
        stopSelf();
        broadcastState();
    }

    public synchronized void loadPresetAndPlay(String presetId) {
        Preset preset = SoundCatalog.getBuiltInPreset(presetId);
        if (preset == null || preset.getSoundVolumes().isEmpty()) {
            return;
        }
        // Stop current sounds
        for (MediaPlayer mp : players.values()) {
            try {
                if (mp.isPlaying()) mp.stop();
                mp.release();
            } catch (Exception ignored) {
            }
        }
        players.clear();

        // Start preset sounds
        for (Map.Entry<String, Integer> entry : preset.getSoundVolumes().entrySet()) {
            String sid = entry.getKey();
            int vol = entry.getValue();
            volumes.put(sid, vol);
            MediaPlayer mp = createPlayer(sid);
            if (mp != null) {
                applyVolume(mp, vol);
                mp.start();
                players.put(sid, mp);
            }
        }
        isPlaying = !players.isEmpty();
        updateForegroundState();
        broadcastState();
    }

    private MediaPlayer createPlayer(String soundId) {
        SoundItem item = SoundCatalog.getSoundById(soundId);
        if (item == null) return null;

        try {
            MediaPlayer mp = MediaPlayer.create(this, item.getRawResId());
            if (mp != null) {
                mp.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
                mp.setLooping(true);
            }
            return mp;
        } catch (Exception e) {
            return null;
        }
    }

    private void applyVolume(MediaPlayer mp, int volumePercent) {
        if (mp == null) return;
        float linear = Math.max(0f, Math.min(100f, volumePercent)) / 100f;
        // Natural audio curve
        float curve = linear * linear;
        try {
            mp.setVolume(curve, curve);
        } catch (Exception ignored) {
        }
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
        for (Map.Entry<String, MediaPlayer> entry : players.entrySet()) {
            int vol = volumes.getOrDefault(entry.getKey(), 70);
            applyVolume(entry.getValue(), vol);
        }
        updateForegroundState();
        broadcastState();
    }

    private void applyFadeoutFactor(float factor) {
        for (Map.Entry<String, MediaPlayer> entry : players.entrySet()) {
            int originalVol = volumes.getOrDefault(entry.getKey(), 70);
            int fadedVol = Math.round(originalVol * factor);
            applyVolume(entry.getValue(), fadedVol);
        }
    }

    private void updateForegroundState() {
        if (players.isEmpty() && !isPlaying) {
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
        if (players.isEmpty()) {
            return getString(R.string.active_sounds_none);
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String id : players.keySet()) {
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
        for (String id : players.keySet()) {
            map.put(id, volumes.getOrDefault(id, 70));
        }
        return map;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isTrackActive(String soundId) {
        return players.containsKey(soundId);
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
        Intent intent = new Intent(BROADCAST_STATE_CHANGED);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_STATE_PLAYING, isPlaying);
        intent.putExtra(EXTRA_STATE_ACTIVE_COUNT, players.size());
        intent.putExtra(EXTRA_STATE_SUMMARY, getActiveSoundsSummary());
        intent.putExtra(EXTRA_STATE_TIMER_MS, timerRemainingMs);
        intent.putExtra(EXTRA_STATE_TIMER_ACTIVE, isTimerActive);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        stopAll();
        super.onDestroy();
    }
}
