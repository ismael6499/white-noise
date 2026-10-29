package com.agustin.whitenoise.service;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

/**
 * Manages gapless, seamless looping for an individual audio track using dual-player equal-power crossfading.
 * Prevents noticeable audio cuts or clicks at the loop boundary.
 */
public class SmoothAudioTrack {

    private static final int DEFAULT_CROSSFADE_MS = 2500; // 2.5 seconds smooth transition
    private static final int MONITOR_INTERVAL_MS = 100;
    private static final int CROSSFADE_TICK_MS = 40;

    private final Context context;
    private final String soundId;
    private final int rawResId;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private MediaPlayer playerA;
    private MediaPlayer playerB;
    private boolean isPlayerACurrent = true;
    private boolean isPlaying = false;
    private int volumePercent = 70;
    private int durationMs = 0;
    private int crossfadeMs = DEFAULT_CROSSFADE_MS;

    private boolean isCrossfading = false;
    private long crossfadeStartTime = 0;

    private final Runnable loopMonitor = new Runnable() {
        @Override
        public void run() {
            if (!isPlaying) return;

            MediaPlayer current = isPlayerACurrent ? playerA : playerB;
            if (current != null) {
                try {
                    if (current.isPlaying()) {
                        int currentPos = current.getCurrentPosition();
                        int remaining = durationMs - currentPos;

                        if (!isCrossfading && remaining <= crossfadeMs && remaining > 0) {
                            startCrossfade();
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            handler.postDelayed(this, MONITOR_INTERVAL_MS);
        }
    };

    private final Runnable crossfadeUpdater = new Runnable() {
        @Override
        public void run() {
            if (!isPlaying || !isCrossfading) return;

            long elapsed = System.currentTimeMillis() - crossfadeStartTime;
            float progress = (float) elapsed / crossfadeMs;

            if (progress >= 1.0f) {
                finishCrossfade();
            } else {
                applyCrossfadeVolumes(progress);
                handler.postDelayed(this, CROSSFADE_TICK_MS);
            }
        }
    };

    public SmoothAudioTrack(Context context, String soundId, int rawResId, int initialVolume) {
        this.context = context.getApplicationContext();
        this.soundId = soundId;
        this.rawResId = rawResId;
        this.volumePercent = initialVolume;
    }

    public synchronized void start() {
        initPlayersIfNeeded();

        MediaPlayer current = isPlayerACurrent ? playerA : playerB;
        if (current != null) {
            float baseVol = getScaledVolume(volumePercent);
            setPlayerVolume(current, baseVol);
            try {
                if (!current.isPlaying()) {
                    current.start();
                }
            } catch (Exception ignored) {
            }
        }
        isPlaying = true;
        handler.removeCallbacks(loopMonitor);
        handler.post(loopMonitor);
    }

    public synchronized void pause() {
        isPlaying = false;
        handler.removeCallbacks(loopMonitor);
        handler.removeCallbacks(crossfadeUpdater);
        isCrossfading = false;
        try {
            if (playerA != null && playerA.isPlaying()) playerA.pause();
            if (playerB != null && playerB.isPlaying()) playerB.pause();
        } catch (Exception ignored) {
        }
    }

    public synchronized void setVolume(int volume) {
        this.volumePercent = Math.max(0, Math.min(100, volume));
        if (!isPlaying) return;

        if (isCrossfading) {
            long elapsed = System.currentTimeMillis() - crossfadeStartTime;
            float progress = Math.min(1.0f, (float) elapsed / crossfadeMs);
            applyCrossfadeVolumes(progress);
        } else {
            MediaPlayer current = isPlayerACurrent ? playerA : playerB;
            if (current != null) {
                setPlayerVolume(current, getScaledVolume(volumePercent));
            }
        }
    }

    public synchronized void applyExternalFadeFactor(float factor) {
        float targetScaled = getScaledVolume(Math.round(volumePercent * factor));
        if (isCrossfading) {
            long elapsed = System.currentTimeMillis() - crossfadeStartTime;
            float progress = Math.min(1.0f, (float) elapsed / crossfadeMs);
            applyCrossfadeVolumesWithBase(progress, targetScaled);
        } else {
            MediaPlayer current = isPlayerACurrent ? playerA : playerB;
            if (current != null) {
                setPlayerVolume(current, targetScaled);
            }
        }
    }

    public synchronized void release() {
        isPlaying = false;
        isCrossfading = false;
        handler.removeCallbacks(loopMonitor);
        handler.removeCallbacks(crossfadeUpdater);

        if (playerA != null) {
            try {
                if (playerA.isPlaying()) playerA.stop();
                playerA.release();
            } catch (Exception ignored) {
            }
            playerA = null;
        }

        if (playerB != null) {
            try {
                if (playerB.isPlaying()) playerB.stop();
                playerB.release();
            } catch (Exception ignored) {
            }
            playerB = null;
        }
    }

    private synchronized void startCrossfade() {
        MediaPlayer incoming = isPlayerACurrent ? playerB : playerA;
        if (incoming == null) {
            incoming = createPlayer();
            if (isPlayerACurrent) {
                playerB = incoming;
            } else {
                playerA = incoming;
            }
        }

        if (incoming == null) return;

        try {
            setPlayerVolume(incoming, 0f);
            incoming.seekTo(0);
            incoming.start();
        } catch (Exception ignored) {
            return;
        }

        isCrossfading = true;
        crossfadeStartTime = System.currentTimeMillis();
        handler.removeCallbacks(crossfadeUpdater);
        handler.post(crossfadeUpdater);
    }

    private synchronized void finishCrossfade() {
        isCrossfading = false;
        handler.removeCallbacks(crossfadeUpdater);

        MediaPlayer outgoing = isPlayerACurrent ? playerA : playerB;
        MediaPlayer incoming = isPlayerACurrent ? playerB : playerA;

        float fullVol = getScaledVolume(volumePercent);
        if (incoming != null) {
            setPlayerVolume(incoming, fullVol);
        }

        if (outgoing != null) {
            try {
                outgoing.pause();
                outgoing.seekTo(0);
                setPlayerVolume(outgoing, 0f);
            } catch (Exception ignored) {
            }
        }

        isPlayerACurrent = !isPlayerACurrent;
    }

    private void applyCrossfadeVolumes(float progress) {
        applyCrossfadeVolumesWithBase(progress, getScaledVolume(volumePercent));
    }

    private void applyCrossfadeVolumesWithBase(float progress, float baseVol) {
        // Equal-power crossfade curve for constant perceived volume
        float p = Math.max(0f, Math.min(1.0f, progress));
        float outGain = baseVol * (float) Math.cos(p * Math.PI * 0.5);
        float inGain = baseVol * (float) Math.sin(p * Math.PI * 0.5);

        MediaPlayer outgoing = isPlayerACurrent ? playerA : playerB;
        MediaPlayer incoming = isPlayerACurrent ? playerB : playerA;

        setPlayerVolume(outgoing, outGain);
        setPlayerVolume(incoming, inGain);
    }

    private void initPlayersIfNeeded() {
        if (playerA == null) {
            playerA = createPlayer();
            if (playerA != null) {
                durationMs = playerA.getDuration();
                if (durationMs > 0) {
                    crossfadeMs = Math.min(DEFAULT_CROSSFADE_MS, Math.max(600, durationMs / 5));
                }
            }
        }
        if (playerB == null) {
            playerB = createPlayer();
        }
    }

    private MediaPlayer createPlayer() {
        try {
            MediaPlayer mp = MediaPlayer.create(context, rawResId);
            if (mp != null) {
                mp.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());

                // Safety fallback if crossfade monitor misses
                mp.setOnCompletionListener(completedPlayer -> {
                    handler.post(() -> {
                        if (isPlaying && !isCrossfading) {
                            startCrossfade();
                        }
                    });
                });
            }
            return mp;
        } catch (Exception e) {
            return null;
        }
    }

    private float getScaledVolume(int percent) {
        float linear = Math.max(0f, Math.min(100f, percent)) / 100f;
        return linear * linear;
    }

    private void setPlayerVolume(MediaPlayer mp, float vol) {
        if (mp != null) {
            try {
                mp.setVolume(vol, vol);
            } catch (Exception ignored) {
            }
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public int getVolumePercent() {
        return volumePercent;
    }

    public String getSoundId() {
        return soundId;
    }
}
