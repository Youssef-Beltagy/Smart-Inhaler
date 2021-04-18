package com.ybeltagy.breathe;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import static androidx.core.content.ContextCompat.startForegroundService;

/**
 * This BroadcastReceiver is triggered by Bluetooth/BLE connection Broadcasts.
 */
public class BLEConnectionReceiver extends BroadcastReceiver {

    /**
     * Debugging tag
     */
    private static final String tag = "BLEConnectionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(tag, "Action: " + action + " received");
        Log.d(tag, "Intent: " + intent.toString());
        Log.d(tag, "Intent Extras: " + intent.getExtras().toString());

        String toastText;
        // For example, do we have to make toastText Atomic?
        if( action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
            BluetoothDevice bluetoothDevice;
            bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            toastText = "Connected to " + bluetoothDevice.getName();
            Log.d(tag, "Connected to " + bluetoothDevice.getName());
            Log.d(tag, "Device: " + bluetoothDevice.toString());

            Intent foregroundServiceIntent = new Intent(context, WearableBLEService.class);
            foregroundServiceIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, bluetoothDevice);
            startForegroundService(context, foregroundServiceIntent); //FIXME why does it take a context too?
        }else{
            Log.e(tag, "Error! received an intent not in the filter");
            Log.e(tag, "Action: " + action + " received");
            toastText = "Action: " + action + " received";
        }

        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show();
    }
}