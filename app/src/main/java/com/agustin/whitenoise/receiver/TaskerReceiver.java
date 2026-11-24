package com.agustin.whitenoise.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.agustin.whitenoise.service.AudioPlayerService;

public class TaskerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        Intent serviceIntent = new Intent(context, AudioPlayerService.class);
        serviceIntent.setAction(intent.getAction());
        if (intent.getExtras() != null) {
            serviceIntent.putExtras(intent.getExtras());
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (RuntimeException ignored) {
        }
    }
}
