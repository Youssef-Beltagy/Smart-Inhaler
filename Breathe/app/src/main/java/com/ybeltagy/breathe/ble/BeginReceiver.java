package com.ybeltagy.breathe.ble;

import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import static androidx.core.content.ContextCompat.startForegroundService;

/**
 * This BroadcastReceiver is triggered by Bluetooth/BLE connection Broadcasts.
 * listens for boot and update. Starts the BLEService.
 * Limitation: can't detect when the app opens
 */
public class BeginReceiver extends BroadcastReceiver {

    /**
     * Debugging tag
     */
    private static final String tag = BeginReceiver.class.getName();

    /**
     * This onReceive method is executed when the device boots or when the app is updated.
     * Google's documentation of the onReceive method is copied below.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(tag, "Action: " + action + " received");
        Log.d(tag, "Intent: " + intent.toString());
        Log.d(tag, "Intent Extras: " + intent.getExtras().toString());

        //FIXME: for debugging. Delete before production.
        Toast.makeText(context, "Detected Start Event", Toast.LENGTH_SHORT).show();

        //Start the BLEService
        Intent foregroundServiceIntent = new Intent(context, BLEService.class);
        startForegroundService(context, foregroundServiceIntent);
    }

}