package com.ybeltagy.breathe.ble;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class BluetoothStateReceiver extends BroadcastReceiver {
    /**
     * Debugging tag
     */
    private static final String tag = "BluetoothStateReceiver";

    /**
     * If a BLE connection is made, tell the service to connect with the devices.
     * If a BLE connectioin is lost, disconnect from all devices.
     * @param context
     * @param intent
     */

    //fixme: clean this file.
    // https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#ACTION_STATE_CHANGED
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

        Log.d(tag, "Action: " + action + " received");
        Log.d(tag, "Intent: " + intent.toString());
        Log.d(tag, "State: " + state);
        Log.d(tag, "Intent Extras: " + intent.getExtras().toString());

        Toast.makeText(context, "Action: " + action + " S: " + state, Toast.LENGTH_SHORT);

        if (state == BluetoothAdapter.STATE_ON) {
            // TODO: connect the devices
            //BLEService.connectDevices(); //
        }else if (state == BluetoothAdapter.STATE_OFF){
            // TODO: disconnect the devices
            //BLEService.disconnectDevices(); // not sure if this is necessary.
        }
    }

}
