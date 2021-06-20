package com.ybeltagy.breathe.ble;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import static androidx.core.content.ContextCompat.startForegroundService;

public class BluetoothStateReceiver extends BroadcastReceiver {
    /**
     * Debugging tag
     */
    private static final String tag = BluetoothStateReceiver.class.getName();

    //
    /**
     * Listens to https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#ACTION_STATE_CHANGED
     * Tells the BLEService when Bluetooth is enabled or disabled.
     * @param context the calling context of the broadcast
     * @param intent the calling intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

        Log.d(tag, "Action: " + action + " received");
        Log.d(tag, "Intent: " + intent.toString());
        Log.d(tag, "State: " + state);
        Log.d(tag, "Intent Extras: " + intent.getExtras().toString());

        Toast.makeText(context, "Action: " + action + " S: " + state, Toast.LENGTH_SHORT).show();

        if (state == BluetoothAdapter.STATE_ON) {
            Intent bluetoothEnabledIntent = new Intent(context, BLEService.class);
            bluetoothEnabledIntent.setAction(BLEFinals.ACTION_BLUETOOTH_ENABLED);
            startForegroundService(context, bluetoothEnabledIntent);
        }else if (state == BluetoothAdapter.STATE_OFF){
            Intent bluetoothDisabledIntent = new Intent(context, BLEService.class);
            bluetoothDisabledIntent.setAction(BLEFinals.ACTION_BLUETOOTH_DISABLED);
            startForegroundService(context, bluetoothDisabledIntent);
        }
    }

}
