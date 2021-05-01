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

/**
 * Attempts to make a BLE connection with the wearable sensor and the smart inhaler.
 * When a connection is established, provides an interface to get the wearable sensor data.
 */
public class BLEService extends Service {

    /**
     * Used to persistently store the mac address of the wearable sensor.
     */
    private static SharedPreferences sharedPreferences = null;

    /**
     * Debugging tag
     */
    private static String tag = "WearableBLEService";

    /**
     * BLEManager for the wearable.
     */
    private static volatile WearableBLEManager manager = null;

    /**
     * This receiver reports when Bluetooth is enabled or disabled.
     */
    private static BroadcastReceiver bleStateReceiver = null;

    /**
     * Represents the device's hardware.
     */
    BluetoothAdapter bluetoothAdapter = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(tag, "In onCreate");
        Toast.makeText(this, "BLEService Started", Toast.LENGTH_SHORT).show();

        //Get a reference to the Bluetooth adapter to use for the life of this service.
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //start a receiver that listens for when bluetooth is enabled or disabled.
        registerBluetoothStateReceiver();

        //Make the shared preferences file that contains the wearable sensor mac address.
        sharedPreferences = getSharedPreferences(BLEFinals.BLE_SHARED_PREF_FILE_NAME, MODE_PRIVATE);

        // Make a notification to attach this service to.
        Notification notification = BLENotification.getBLEServiceNotification(this);

        // Start this service in the foreground and attach the notification to it.
        startForeground(BLE_SERVICE_NOTIFICATION_ID, notification);
    }

    /**
     * Initializes a receiver that detects when Bluetooth is enabled or disabled.
     */
    private void registerBluetoothStateReceiver(){
        IntentFilter intentFilter = new IntentFilter();

        // Only listen for when Bluetooth is enabled or disabled.
        // https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#ACTION_STATE_CHANGED
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        // Initialize the broadcast receiver
        bleStateReceiver = new BluetoothStateReceiver();

        // Register the broadcast receiver.
        this.registerReceiver(bleStateReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        // called even when I force stopped the service.
        Log.d(tag, "In onDestroy");
        Toast.makeText(this, "service destroyed", Toast.LENGTH_SHORT).show();

        // unregister the BluetoothStateReceiver.
        this.unregisterReceiver(bleStateReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(tag, "I'm in onStartCommand");

        if(intent.getAction() != null && intent.getAction().equals(BLEFinals.ACTION_CONNECT_TO_WEARABLE)){
            BluetoothDevice wearableSensor = intent.getParcelableExtra(BLEFinals.WEARABLE_BLUETOOTH_DEVICE_KEY);
            saveWearableAndConnect(wearableSensor);
        }else if (intent.getAction() != null && intent.getAction().equals(BLEFinals.ACTION_BLUETOOTH_DISABLED)){

            //fixme: I'm not sure this is the best way to clean the resources.
            // This might lead to close being called before the disconnection logic is complete.
            manager.close(); // The device should be disconnected, so close the connection too.
            manager = null;

        }else{
            attemptToConnectToWearable();
        }

        // If we get killed after returning from here, restart with the intent
        return Service.START_REDELIVER_INTENT;
    }


    /**
     * This method shows which methods should be used to end this service.
     * Since this service is meant to run all the time, this method is never used.
     * It is still included here as a reference if the peripheral solves the autoconnection issue.
     */
    private void stopThisService(){
        stopForeground(true); // TODO: there are weird edge cases here. -- look at example again
        stopSelf();
    }

    /**
     * Synchronously queries the wearable sensor for wearable data and returns the result.
     * This call is blocking so don't do it in the main UI thread.
     * @return the current wearable data or null
     */
    public static WearableData getWearableData(){
        if(manager == null || !manager.isConnected()) return null;

        return manager.getWeatherData();
    }

    /**
     * Returns a bonded Bluetooth Device with the given address or returns null.
     * @param macAddress the address of the device to look for
     * @return the requested Bluetooth device or null
     */
    private BluetoothDevice findDeviceInBondedDevices(String macAddress){

        if(bluetoothAdapter == null) return null;

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices.size() > 0) {
            for (BluetoothDevice currentDevice : bondedDevices) {
                if(currentDevice.getAddress().equals(macAddress)) return currentDevice;
            }
        }

        return null;
    }

    /**
     * Saves the address of the wearable sensor into the shared preference for future attempts to connect.
     * Then connects to the wearable sensor.
     * @param wearableSensor
     */
    private void saveWearableAndConnect(BluetoothDevice wearableSensor){

        SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
        preferencesEditor.putString(BLEFinals.WEARABLE_BLUETOOTH_DEVICE_KEY, wearableSensor.getAddress());
        preferencesEditor.apply();

        connectToWearable(wearableSensor);
    }

    /**
     * Attempts to connect to the wearable sensor if its mac address is saved in the shared preferences
     * and it is bonded.
     */
    private void attemptToConnectToWearable(){
        String macAddress = sharedPreferences.getString(BLEFinals.WEARABLE_BLUETOOTH_DEVICE_KEY, null);

        if(macAddress == null) return;

        BluetoothDevice wearableSensor = findDeviceInBondedDevices(macAddress);

        if(wearableSensor == null) return;

        connectToWearable(wearableSensor);

    }

    /**
     * Connects to the wearableSensor parameter assuming it really is the wearable sensor.
     * @param wearableSensor
     */
    private void connectToWearable(BluetoothDevice wearableSensor){

        if(manager != null){
            manager.disconnect();
            //todo: again, I'm not sure this is the best way to clean the resources especially because I only call disconnect without close.
            // look into cleaning the resources more
            //todo: to test this, I need two wearable sensors.
        }

        manager = new WearableBLEManager(this);//todo: is it better to reuse the manager or to make a new one?

        manager.setConnectionObserver(new BLEConnectionObserver()); // todo: consider deleting. Mostly used for debugging.

        manager.connect(wearableSensor)
                .useAutoConnect(true)
                .done(device -> Log.d(tag, "Device connected: " + device))
                .enqueue();

    }

}