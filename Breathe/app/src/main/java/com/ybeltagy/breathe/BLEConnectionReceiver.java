package com.ybeltagy.breathe;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.SyncStateContract;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static android.provider.Settings.System.getString;
import static androidx.core.content.ContextCompat.getSystemService;
import static androidx.core.content.ContextCompat.startForegroundService;

/**
 * This BroadcastReceiver is triggered by Bluetooth connection and disconnection intents by a paired
 * secondary device to the phone. (To be changed to BLE connection and disconnection)
 */
public class BLEConnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("BroadcastActions", "Action: " + action + " received");

        BluetoothDevice bluetoothDevice;

        String toastText; // todo: what happens when there are multiple BRs at the same time? Should we worry about race conditions?
        // For example, do we have to make toastText Atomic?

        Log.d("BroadcastActions", "Intent: " + intent.toString());
        Log.d("BroadcastActions", "Intent Extras: " + intent.getExtras().toString());


        switch (action) {
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // todo: confirm this is our inhaler/pin
                toastText = "Connected to " + bluetoothDevice.getName();
                Log.d("BroadcastActions", "Connected to "+bluetoothDevice.getName());
                Log.d("BroadcastActions", "Device: "+bluetoothDevice.toString());

                //todo: should I hardcode a mac address for now?
                //todo: Should I search?
                //todo: In the future, Either search or ask the user to put the mac.

                //todo: am i using the correct arguments.
                Intent foregroundServiceIntent = new Intent(context,BLEService.class);
                foregroundServiceIntent.putExtra("device", bluetoothDevice);
                foregroundServiceIntent.setAction("START");
                startForegroundService(context, foregroundServiceIntent);


                break;

            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                toastText = "Disconnected from " + bluetoothDevice.getName();
                Log.d("BroadcastActions", "Disconnected from "+bluetoothDevice.getName());
                Log.d("BroadcastActions", "Device: "+bluetoothDevice.toString());

                Intent stopIntent = new Intent(context, BLEService.class);
                stopIntent.setAction("STOP");
                context.startService(stopIntent);
                break;

            default:
                toastText = "Action: " + action + " received";
        }
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show();
    }
}