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
    private static final String tag = "BeginReceiver";

    /**
     * This onReceive method is executed when the device boots or when the app is updated.
     * Google's documentation of the onReceive method is copied below.
     *
     * <p>
     * This method is called when the BroadcastReceiver is receiving an Intent
     * broadcast.  During this time you can use the other methods on
     * BroadcastReceiver to view/modify the current result values.  This method
     * is always called within the main thread of its process, unless you
     * explicitly asked for it to be scheduled on a different thread using
     * {@link Context#registerReceiver(BroadcastReceiver,
     * IntentFilter, String, Handler)}. When it runs on the main
     * thread you should
     * never perform long-running operations in it (there is a timeout of
     * 10 seconds that the system allows before considering the receiver to
     * be blocked and a candidate to be killed). You cannot launch a popup dialog
     * in your implementation of onReceive().
     *
     * <p><b>If this BroadcastReceiver was launched through a &lt;receiver&gt; tag,
     * then the object is no longer alive after returning from this
     * function.</b> This means you should not perform any operations that
     * return a result to you asynchronously. If you need to perform any follow up
     * background work, schedule a {@link JobService} with
     * {@link JobScheduler}.
     * <p>
     * If you wish to interact with a service that is already running and previously
     * bound using {@link Context#bindService(Intent, ServiceConnection, int) bindService()},
     * you can use {@link #peekService}.
     *
     * <p>The Intent filters used in {@link Context#registerReceiver}
     * and in application manifests are <em>not</em> guaranteed to be exclusive. They
     * are hints to the operating system about how to find suitable recipients. It is
     * possible for senders to force delivery to specific recipients, bypassing filter
     * resolution.  For this reason, {@link #onReceive(Context, Intent) onReceive()}
     * implementations should respond only to known actions, ignoring any unexpected
     * Intents that they may receive.
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

        //Start the BLEService
        Intent foregroundServiceIntent = new Intent(context, BLEService.class);
        startForegroundService(context, foregroundServiceIntent);
    }

}