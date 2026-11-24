package com.agustin.whitenoise.receiver;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.agustin.whitenoise.provider.WhiteNoiseControlProvider;
import com.agustin.whitenoise.service.AudioPlayerService;

/**
 * Invisible foreground trampoline used on Android 12+ if a background caller
 * with overlay permission triggers playback while the service is cold.
 */
public class RemoteStartActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent incoming = getIntent();
        String action = incoming != null ? incoming.getAction() : null;
        if (WhiteNoiseControlProvider.isAllowedAction(action)) {
            Intent serviceIntent = new Intent(this, AudioPlayerService.class);
            serviceIntent.setAction(action);
            if (incoming.getExtras() != null) {
                serviceIntent.putExtras(incoming.getExtras());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
