package com.ybeltagy.breathe.ble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import static androidx.core.content.ContextCompat.startForegroundService;

/**
 * This BroadcastReceiver is triggered by Bluetooth/BLE connection Broadcasts.
 */
public class BeginReceiver extends BroadcastReceiver {


    /**
     * listens for boot and update. Starts the BLEService.
     * Limitation: can't detect when the app opens
     */

    /**
     * Debugging tag
     */
    private static final String tag = "BeginReceiver";

    //todo: document this file.
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(tag, "Action: " + action + " received");
        Log.d(tag, "Intent: " + intent.toString());
        Log.d(tag, "Intent Extras: " + intent.getExtras().toString());

        Toast toast = Toast.makeText(context, "Action: " + action, Toast.LENGTH_SHORT);
        toast.show();

        Intent foregroundServiceIntent = new Intent(context, BLEService.class);
        startForegroundService(context, foregroundServiceIntent); //FIXME why does it take a context too?
    }

}