package com.agustin.whitenoise;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.AlarmClock;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.agustin.whitenoise.model.Preset;
import com.agustin.whitenoise.model.SoundCatalog;
import com.agustin.whitenoise.model.SoundItem;
import com.agustin.whitenoise.service.AudioPlayerService;
import com.agustin.whitenoise.ui.SoundAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SoundAdapter.OnSoundActionListener {

    private static final int PERMISSION_REQ_NOTIFICATIONS = 1001;
    private static final String PREFS_NAME = "WhiteNoisePrefs";
    private static final String KEY_CUSTOM_PRESETS = "custom_presets";
    private static final String KEY_TIMER_SLEEP_ALARM = "timer_with_sleep_alarm";
    private static final String KEY_SLEEP_ALARM_SEQ = "sleep_alarm_seq_id";
    private static final String SLEEP_PACKAGE = "com.urbandroid.sleep";
    private static final String SLEEP_ADD_ALARM_ACTIVITY = "com.urbandroid.sleep.alarmclock.AddAlarmActivity";

    private RecyclerView recyclerSounds;
    private SoundAdapter soundAdapter;
    private List<SoundItem> soundList;

    private TextView tvActiveSummary;
    private TextView tvTimerRemaining;
    private MaterialButton btnTimer;
    private MaterialButton btnStopAll;
    private ImageButton btnSavePreset;
    private FloatingActionButton fabMasterPlayPause;
    private ChipGroup chipGroupPresets;

    private AudioPlayerService playerService;
    private boolean isBound = false;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && AudioPlayerService.BROADCAST_STATE_CHANGED.equals(intent.getAction())) {
                syncServiceState();
            }
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            AudioPlayerService.LocalBinder b = (AudioPlayerService.LocalBinder) binder;
            playerService = b.getService();
            isBound = true;
            syncServiceState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playerService = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerView();
        setupPresets();
        setupListeners();
        requestNotificationPermission();

        // Start & Bind Service
        Intent serviceIntent = new Intent(this, AudioPlayerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initViews() {
        recyclerSounds = findViewById(R.id.recyclerSounds);
        tvActiveSummary = findViewById(R.id.tvActiveSoundsSummary);
        tvTimerRemaining = findViewById(R.id.tvTimerRemaining);
        btnTimer = findViewById(R.id.btnTimer);
        btnStopAll = findViewById(R.id.btnStopAll);
        btnSavePreset = findViewById(R.id.btnSavePreset);
        fabMasterPlayPause = findViewById(R.id.fabMasterPlayPause);
        chipGroupPresets = findViewById(R.id.chipGroupPresets);
    }

    private void setupRecyclerView() {
        soundList = SoundCatalog.createFreshSoundList();
        soundAdapter = new SoundAdapter(this, soundList, this);
        recyclerSounds.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerSounds.setAdapter(soundAdapter);
    }

    private void setupPresets() {
        chipGroupPresets.removeAllViews();

        // Populate all built-in presets dynamically
        for (Preset preset : SoundCatalog.getBuiltInPresets()) {
            Chip chip = new Chip(this);
            chip.setText(preset.getNameResId());
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            chip.setChipBackgroundColorResource(R.color.chip_bg);
            chip.setChipStrokeColorResource(R.color.accent_blue);
            chip.setChipStrokeWidth(1f);
            if (preset.getIconResId() != 0) {
                chip.setChipIconResource(preset.getIconResId());
                chip.setChipIconTintResource(R.color.accent_blue);
            }
            chip.setCheckable(true);
            chip.setOnClickListener(v -> loadPreset(preset.getId()));
            chipGroupPresets.addView(chip);
        }

        loadCustomPresetChips();
    }

    private void setupListeners() {
        btnStopAll.setOnClickListener(v -> {
            if (playerService != null) {
                playerService.stopAll();
            }
        });

        fabMasterPlayPause.setOnClickListener(v -> {
            if (playerService != null) {
                if (playerService.isPlaying()) {
                    playerService.pauseAll();
                } else {
                    playerService.resumeOrStartAll();
                }
            }
        });

        btnTimer.setOnClickListener(v -> showSleepTimerDialog());

        btnSavePreset.setOnClickListener(v -> showSavePresetDialog());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQ_NOTIFICATIONS
                );
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(AudioPlayerService.BROADCAST_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, filter);
        }
        syncServiceState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(stateReceiver);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        super.onDestroy();
    }

    @Override
    public void onToggle(SoundItem item, boolean enabled, int volume) {
        if (playerService != null) {
            playerService.setTrackState(item.getId(), enabled, volume);
        }
    }

    @Override
    public void onVolumeChanged(SoundItem item, int volume) {
        if (playerService != null) {
            playerService.updateTrackVolume(item.getId(), volume);
        }
    }

    private void loadPreset(String presetId) {
        if (playerService != null) {
            playerService.loadPresetAndPlay(presetId);
            Preset p = SoundCatalog.getBuiltInPreset(presetId);
            if (p != null) {
                String name = p.getNameResId() != 0 ? getString(p.getNameResId()) : p.getName();
                Toast.makeText(this, getString(R.string.preset_loaded, name), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadCustomPreset(Preset preset) {
        if (playerService == null || preset == null) return;
        for (SoundItem s : soundList) {
            boolean active = preset.getSoundVolumes().containsKey(s.getId());
            int vol = preset.getSoundVolumes().getOrDefault(s.getId(), s.getVolume());
            playerService.setTrackState(s.getId(), active, vol);
        }
        Toast.makeText(this, getString(R.string.preset_loaded, preset.getName()), Toast.LENGTH_SHORT).show();
    }

    private void syncServiceState() {
        if (playerService == null) return;

        boolean isPlaying = playerService.isPlaying();
        fabMasterPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        fabMasterPlayPause.setContentDescription(getString(isPlaying ? R.string.pause : R.string.play));

        tvActiveSummary.setText(playerService.getActiveSoundsSummary());

        // Update list states
        Map<String, Integer> activeMap = playerService.getActiveSoundsWithVolume();
        boolean changed = false;
        for (SoundItem item : soundList) {
            boolean wasActive = item.isPlaying();
            boolean nowActive = activeMap.containsKey(item.getId());
            if (wasActive != nowActive) {
                item.setPlaying(nowActive);
                changed = true;
            }
            if (nowActive) {
                int vol = activeMap.get(item.getId());
                if (item.getVolume() != vol) {
                    item.setVolume(vol);
                    changed = true;
                }
            }
        }
        if (changed) {
            soundAdapter.notifyDataSetChanged();
        }

        // Update timer UI
        if (playerService.isTimerActive() && playerService.getTimerRemainingMs() > 0) {
            long totalSec = playerService.getTimerRemainingMs() / 1000;
            long min = totalSec / 60;
            long sec = totalSec % 60;
            tvTimerRemaining.setVisibility(View.VISIBLE);
            tvTimerRemaining.setText("⏳ " + getString(R.string.timer_badge_format, min, sec));
            btnTimer.setText(getString(R.string.timer_badge_format, min, sec));
        } else {
            tvTimerRemaining.setVisibility(View.GONE);
            btnTimer.setText(R.string.sleep_timer);
        }
    }

    private void showSleepTimerDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_sleep_timer, null);
        View layoutSleepOption = view.findViewById(R.id.layoutSleepAlarmOption);
        SwitchMaterial switchSleepAlarm = view.findViewById(R.id.switchSleepAlarm);

        boolean sleepInstalled = isSleepAppInstalled();
        layoutSleepOption.setVisibility(sleepInstalled ? View.VISIBLE : View.GONE);
        if (sleepInstalled) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            switchSleepAlarm.setChecked(prefs.getBoolean(KEY_TIMER_SLEEP_ALARM, false));
            switchSleepAlarm.setOnCheckedChangeListener((buttonView, isChecked) ->
                    prefs.edit().putBoolean(KEY_TIMER_SLEEP_ALARM, isChecked).apply());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sleep_timer)
                .setView(view)
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();

        int[] btnIds = new int[]{
                R.id.btnTimer15, R.id.btnTimer30, R.id.btnTimer45,
                R.id.btnTimer60, R.id.btnTimer90, R.id.btnTimer120
        };
        int[] minutesValues = new int[]{15, 30, 45, 60, 90, 120};
        for (int i = 0; i < btnIds.length; i++) {
            final int mins = minutesValues[i];
            view.findViewById(btnIds[i]).setOnClickListener(v -> {
                boolean withAlarm = sleepInstalled && switchSleepAlarm.isChecked();
                dialog.dismiss();
                setTimer(mins, withAlarm);
            });
        }

        view.findViewById(R.id.btnTimerCustom).setOnClickListener(v -> {
            boolean withAlarm = sleepInstalled && switchSleepAlarm.isChecked();
            dialog.dismiss();
            showCustomTimerDialog(withAlarm);
        });

        view.findViewById(R.id.btnTimerDisable).setOnClickListener(v -> {
            dialog.dismiss();
            cancelTimer();
        });

        dialog.show();
    }

    private void showCustomTimerDialog(boolean withSleepAlarm) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_custom_timer, null);
        EditText et = view.findViewById(R.id.etCustomMinutes);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.timer_custom)
                .setView(view)
                .setPositiveButton(R.string.dialog_set, (dialog, which) -> {
                    String val = et.getText() != null ? et.getText().toString().trim() : "";
                    if (!val.isEmpty()) {
                        try {
                            int mins = Integer.parseInt(val);
                            if (mins > 0) {
                                setTimer(mins, withSleepAlarm);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void setTimer(int minutes, boolean withSleepAlarm) {
        if (playerService == null || minutes <= 0) return;

        playerService.startSleepTimer(minutes);
        if (withSleepAlarm) {
            String alarmTime = createSleepAsAndroidAlarm(minutes);
            if (alarmTime != null) {
                Toast.makeText(this,
                        getString(R.string.timer_with_sleep_alarm_toast, minutes, alarmTime),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.timer_sleep_alarm_failed, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, getString(R.string.timer_active_format, minutes), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isSleepAppInstalled() {
        try {
            getPackageManager().getPackageInfo(SLEEP_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private String createSleepAsAndroidAlarm(int offsetMinutes) {
        int safeOffset = Math.max(1, Math.min(720, offsetMinutes));
        Calendar target = Calendar.getInstance();
        target.add(Calendar.MINUTE, safeOffset);

        int hour = target.get(Calendar.HOUR_OF_DAY);
        int minute = target.get(Calendar.MINUTE);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int seqId = prefs.getInt(KEY_SLEEP_ALARM_SEQ, 0) + 1;
        prefs.edit().putInt(KEY_SLEEP_ALARM_SEQ, seqId).apply();
        String label = getString(R.string.timer_sleep_alarm_label_fmt, safeOffset, seqId);

        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
        intent.setComponent(new ComponentName(SLEEP_PACKAGE, SLEEP_ADD_ALARM_ACTIVITY));
        intent.putExtra(AlarmClock.EXTRA_HOUR, hour);
        intent.putExtra(AlarmClock.EXTRA_MINUTES, minute);
        intent.putExtra(AlarmClock.EXTRA_MESSAGE, label);
        intent.putExtra(AlarmClock.EXTRA_VIBRATE, true);
        intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);

        try {
            startActivity(intent);
            return String.format(Locale.US, "%02d:%02d", hour, minute);
        } catch (Exception e) {
            return null;
        }
    }

    private void cancelTimer() {
        if (playerService != null) {
            playerService.cancelSleepTimer();
            Toast.makeText(this, R.string.timer_off, Toast.LENGTH_SHORT).show();
        }
    }

    private void showSavePresetDialog() {
        if (playerService == null || playerService.getActiveSoundsWithVolume().isEmpty()) {
            Toast.makeText(this, R.string.active_sounds_none, Toast.LENGTH_SHORT).show();
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_save_preset, null);
        EditText et = view.findViewById(R.id.etPresetName);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.save_preset_title)
                .setView(view)
                .setPositiveButton(R.string.dialog_save, (dialog, which) -> {
                    String name = et.getText().toString().trim();
                    if (!name.isEmpty()) {
                        saveCurrentPreset(name);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void saveCurrentPreset(String name) {
        if (playerService == null) return;
        Preset preset = new Preset("custom_" + System.currentTimeMillis(), name);
        preset.setSoundVolumes(playerService.getActiveSoundsWithVolume());

        List<Preset> customList = getSavedCustomPresets();
        customList.add(preset);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_CUSTOM_PRESETS, new Gson().toJson(customList)).apply();

        addCustomPresetChip(preset);
        AudioPlayerService.notifyExternalStateChanged(this);
        Toast.makeText(this, R.string.preset_saved, Toast.LENGTH_SHORT).show();
    }

    private List<Preset> getSavedCustomPresets() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_CUSTOM_PRESETS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Preset>>() {}.getType();
        try {
            return new Gson().fromJson(json, type);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void loadCustomPresetChips() {
        List<Preset> presets = getSavedCustomPresets();
        for (Preset p : presets) {
            addCustomPresetChip(p);
        }
    }

    private void addCustomPresetChip(Preset preset) {
        Chip chip = new Chip(this);
        chip.setText(preset.getName());
        chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        chip.setChipBackgroundColorResource(R.color.chip_bg);
        chip.setCheckable(true);
        chip.setOnClickListener(v -> loadCustomPreset(preset));
        chipGroupPresets.addView(chip);
    }
}
