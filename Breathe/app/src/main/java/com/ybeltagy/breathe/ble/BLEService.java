package com.ybeltagy.breathe.ble;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.ybeltagy.breathe.WearableData;

import java.util.Set;

import static com.ybeltagy.breathe.ble.BLEFinals.BLE_SERVICE_NOTIFICATION_ID;

public class BLEService extends Service {

    BluetoothAdapter bluetoothAdapter = null;
    private static SharedPreferences sharedPreferences = null;

    /**
     * Debugging tag
     */
    private static String tag = "WearableBLEService";

    /**
     * BLEManager for the wearable.
     */
    private static volatile WearableBLEManager manager = null;

    //consider making volatile
    private static BroadcastReceiver bleStateReceiver = null;





    //TODO: consider multi-threading
    //TODO: How do I know a service is running. Is a bool flag reliable? When should I set and reset it?

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(tag, "In onCreate");
        Toast.makeText(this, "service created", Toast.LENGTH_SHORT).show();

        IntentFilter intentFilter = new IntentFilter();

        //https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#ACTION_STATE_CHANGED
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        // Make and register the BLE BR.
        bleStateReceiver = new BluetoothStateReceiver();

        this.registerReceiver(bleStateReceiver, intentFilter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        sharedPreferences = getSharedPreferences(BLEFinals.BLE_SHARED_PREF_FILE_NAME, MODE_PRIVATE);
    }

    @Override
    public void onDestroy() {
        Log.d(tag, "In onDestroy");
        Toast.makeText(this, "service destroyed", Toast.LENGTH_SHORT).show();
        // called even when I force stopped the service.

        this.unregisterReceiver(bleStateReceiver);

        // Unregister receiver
        // make sure it is called even when app crashes or is closed forcefully.
        // May need to register the receiver somewhere different.
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(tag, "I'm in onStartCommand");


        Notification notification = BLENotification.getBLEServiceNotification(this);
        startForeground(BLE_SERVICE_NOTIFICATION_ID, notification);

        if(intent.getAction() != null && intent.getAction().equals(BLEFinals.ACTION_CONNECT_TO_WEARABLE)){
            BluetoothDevice wearableSensor = intent.getParcelableExtra(BLEFinals.WEARABLE_BLUETOOTH_DEVICE_KEY);
            connectToWearable(wearableSensor);
        }else{
            attemptToConnectToWearable();

        }


        // If we get killed after returning from here, restart with the intent
        return Service.START_REDELIVER_INTENT;
    }


    private void stopThisService(){
        stopForeground(true); // TODO: there are weird edge cases here. -- look at example again
        stopSelf();
    }


    public static WearableData getWearableData(){
        if(manager == null || !manager.isConnected()) return null;

        return manager.getWeatherData();
    }

    /**
     * Returns a bonded Bluetooth Device with the given address or returns a null.
     * @param macAddress the address of the device to look for
     * @return the requested Bluetooth device or null
     */
    private BluetoothDevice finDeviceInBondededDevices(String macAddress){

        if(bluetoothAdapter == null) return null;

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices.size() > 0) {
            for (BluetoothDevice currentDevice : bondedDevices) {
                if(currentDevice.getAddress().equals(macAddress)) return currentDevice;
            }
        }

        return null;
    }

    private void connectToWearable(BluetoothDevice wearableSensor){

        SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
        preferencesEditor.putString(BLEFinals.WEARABLE_BLUETOOTH_DEVICE_KEY, wearableSensor.getAddress());
        preferencesEditor.apply();

        if(manager != null) manager.disconnect(); //todo: look into cleaning the resources more

        manager = new WearableBLEManager(this);//todo: is it better to reuse the manager to make a new one?

        manager.setConnectionObserver(new BLEConnectionObserver()); // todo: consider deleting. Mostly used for debugging.

        manager.connect(wearableSensor)
                //.timeout(100000)
                //.retry(3, 100)
                .useAutoConnect(true)
                .done(device -> Log.d(tag, "Device connected: " + device))
                .enqueue();
    }

    private void attemptToConnectToWearable(){
        String macAddress = sharedPreferences.getString(BLEFinals.WEARABLE_BLUETOOTH_DEVICE_KEY, null);

        if(macAddress == null) return;

        BluetoothDevice wearableSensor = finDeviceInBondededDevices(macAddress);

        if(wearableSensor == null) return;

        if(manager != null) manager.disconnect(); //todo: look into cleaning the resources more

        manager = new WearableBLEManager(this);//todo: is it better to reuse the manager to make a new one?

        manager.setConnectionObserver(new BLEConnectionObserver()); // todo: consider deleting. Mostly used for debugging.

        manager.connect(wearableSensor)
                //.timeout(100000)
                //.retry(3, 100)
                .useAutoConnect(true)
                .done(device -> Log.d(tag, "Device connected: " + device))
                .enqueue();

    }

}