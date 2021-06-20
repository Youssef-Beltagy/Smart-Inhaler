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

import com.ybeltagy.breathe.data.WearableData;

import java.util.Set;

import no.nordicsemi.android.ble.BleManager;

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
    private static final String tag = BLEService.class.getName();

    /**
     * BLEManager for the wearable. Represents a wearable.
     */
    private static volatile WearableBLEManager wearableBLEManager = null;
    // FIXME: Avoid using static for wearable/inhaler ble managers and instead implement the onBind functionality.

    /**
     * BLEManager for the inhaler. Represents an inhaler.
     */
    private static volatile InhalerBLEManager inhalerBLEManager = null;

    /**
     * This receiver reports when Bluetooth is enabled or disabled.
     */
    private static BroadcastReceiver bleStateReceiver = null;

    /**
     * Represents the device's hardware BLE
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
        startForeground(BLEFinals.BLE_SERVICE_NOTIFICATION_ID, notification);
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
        // called even when I force killed the service.
        Log.d(tag, "In onDestroy");
        Toast.makeText(this, "service destroyed", Toast.LENGTH_SHORT).show();

        // unregister the BluetoothStateReceiver.
        this.unregisterReceiver(bleStateReceiver);


        // disconnect from connected devices.
        cleanupBLEManager(wearableBLEManager);
        wearableBLEManager = null;

        cleanupBLEManager(inhalerBLEManager);
        inhalerBLEManager = null;
    }

    /**
     * Possible entries:
     *  Boot
     *      Attempts to connect to the bonded wearable sensor
     *  Bluetooth enabled
     *      Attempts to connect to the bonded wearable sensor
     *  Bluetooth disabled
     *      Disconnects from the current device.
     *  A device is found
     *      Disconnects from the current device.
     *      Save the newly found device in the shared preferences.
     *      Connects with the newly found device.
     *  The app is opened.
     *      Attempts to connect to the bonded wearable sensor.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(tag, "I'm in onStartCommand");

        /**
         * Found a device
         *      Disconnects from the current device.
         *      Save the newly found device in the shared preferences.
         *      Connects with the newly found device.
         */
        if(intent.getAction() != null && intent.getAction().equals(BLEFinals.ACTION_CONNECT_TO_WEARABLE)){



            BluetoothDevice wearableSensor = intent.getParcelableExtra(BLEFinals.WEARABLE_BLUETOOTH_DEVICE_KEY);
            cleanupBLEManager(wearableBLEManager); // disconnect from any connected device.
            wearableBLEManager = null;
            saveBLEDevice(BLEFinals.WEARABLE_BLUETOOTH_DEVICE_KEY, wearableSensor); // save the wearable mac address in the shared preferences
            connectToWearable(wearableSensor); // connect to the newly found one.

        }else if (intent.getAction() != null && intent.getAction().equals(BLEFinals.ACTION_CONNECT_TO_INHALER)){

            /**
             * Found a device
             *      Disconnects from the current device.
             *      Save the newly found device in the shared preferences.
             *      Connects with the newly found device.
             */

            BluetoothDevice inhaler = intent.getParcelableExtra(BLEFinals.INHALER_BLUETOOTH_DEVICE_KEY);
            cleanupBLEManager(inhalerBLEManager); // disconnect from any connected device.
            inhalerBLEManager = null;
            saveBLEDevice(BLEFinals.INHALER_BLUETOOTH_DEVICE_KEY, inhaler); // save the wearable mac address in the shared preferences
            connectToInhaler(inhaler); // connect to the newly found one.

        } else if (intent.getAction() != null && intent.getAction().equals(BLEFinals.ACTION_BLUETOOTH_DISABLED)){
            /**
             * Bluetooth disabled
             *      Disconnects from the current devices.
             */

            cleanupBLEManager(wearableBLEManager); // disconnect from any connected device.
            wearableBLEManager = null;

            cleanupBLEManager(inhalerBLEManager);
            inhalerBLEManager = null;
        }else{
            /**
             * Boot or Bluetooth Enabled or the app is opened
             *      Attempt to connect to the bonded wearable sensor if the wearableBLEManager is not already connected.
             */

            connectToWearable(findBondedAndSavedBLEDevice(BLEFinals.WEARABLE_BLUETOOTH_DEVICE_KEY));

            connectToInhaler(findBondedAndSavedBLEDevice(BLEFinals.INHALER_BLUETOOTH_DEVICE_KEY));

        }

        // If we get killed after returning from here, restart with the intent
        return Service.START_REDELIVER_INTENT;
    }


    /**
     * This method shows which methods should be used to end this service.
     * Since this service is meant to run all the time, this method is never used.
     * It is still included here as a reference if the peripheral solves the auto connection issue.
     */
    private void stopThisService(){
        stopForeground(true); // TODO: there are weird edge cases here. -- look at example again
        stopSelf();
    }

    /**
     * Synchronously queries the wearable sensor for wearable data and returns the result.
     * This call is blocking so don't do it in the main UI thread.
     * @return the current wearable data or null
     * @deprecated Needs static BLEmanagers which cause annoying warnings
     */
    public static WearableData getWearableData(){
        Log.d(tag, "Attempting to get wearable data");
        if(wearableBLEManager == null || !wearableBLEManager.isConnected()) return null;

        return wearableBLEManager.getWeatherData();
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
     */
    private void saveBLEDevice(String key, BluetoothDevice bleDevice){
        SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
        preferencesEditor.putString(key, bleDevice.getAddress());
        preferencesEditor.apply();
    }

    /**
     * Attempts to connect to the wearable sensor if its mac address is saved in the shared preferences
     * and it is bonded.
     */
    private BluetoothDevice findBondedAndSavedBLEDevice(String key){
        String macAddress = sharedPreferences.getString(key, null);

        if(macAddress == null) return null;

        BluetoothDevice bluetoothDevice = findDeviceInBondedDevices(macAddress);

        if(bluetoothDevice == null) return null; // this line is unnecessary, but clarifies the behavior of the method.

        return bluetoothDevice;

    }

    /**
     * If wearableBLEManager is null, Connects to the wearableSensor parameter assuming it really is the wearable sensor.
     * If wearableBLEManager is not null, does nothing.
     * @param wearableSensor the Bluetooth device to connect to
     */
    private void connectToWearable(BluetoothDevice wearableSensor){

        if(wearableSensor == null) return;

        if(wearableBLEManager != null){
            Log.d(tag, "wearable BLE manager is not null");
            return;
        }

        wearableBLEManager = new WearableBLEManager(this);
        //todo: is it better to reuse the manager or to make a new one?
        // Since every manager is usually used exclusively for one device, I'm not sure reusing the manager is a good idea.
        // Even if it is somewhat inefficient, I feel safer creating a new manager.

        wearableBLEManager.setConnectionObserver(new BLEConnectionObserver()); // todo: consider deleting. Mostly used for debugging.


        wearableBLEManager.connect(wearableSensor)
                .useAutoConnect(true)
                .done(device -> {
                    Log.d(tag, "Wearable connected: " + device);
                    Toast.makeText(this, "Wearable: " + wearableSensor.getName() + " Connected", Toast.LENGTH_SHORT).show();
                })
                .enqueue();

    }

    /**
     * If inhalerBLEManager is null, Connects to the inhaler parameter assuming it really is the inhaler.
     * If inhalerBLEManager is not null, does nothing.
     * @param inhaler the bluetooth device to connect to.
     */
    private void connectToInhaler(BluetoothDevice inhaler){

        if(inhaler == null) return;

        if(inhalerBLEManager != null){
            Log.d(tag, "Inhaler BLE manager is not null");
            return;
        }

        inhalerBLEManager = new InhalerBLEManager(this);
        //todo: is it better to reuse the manager or to make a new one?
        // Since every manager is usually used exclusively for one device, I'm not sure reusing the manager is a good idea.
        // Even if it is somewhat inefficient, I feel safer creating a new manager.

        inhalerBLEManager.setConnectionObserver(new BLEConnectionObserver()); // todo: consider deleting. Mostly used for debugging.


        inhalerBLEManager.connect(inhaler)
                .useAutoConnect(true)
                .done(device -> {
                    Log.d(tag, "Inhaler Connected: " + device);
                    Toast.makeText(this, "Inhaler: " + inhaler.getName() + " Connected", Toast.LENGTH_SHORT).show();
                })
                .enqueue();

    }

    /**
     * Disconnects from the current wearable device and closes peripheralBLEManager.
     * Sets peripheralBLEManager to null.
     * @param peripheralBLEManager the BleManager which should be disconnected.
     */
    private void cleanupBLEManager(BleManager peripheralBLEManager){
        // From the BleMulticonnectProfileService implemented in: https://github.com/NordicSemiconductor/Android-nRF-Toolbox
        // It seems a BLEManager connects with only one device.
        // Before connecting to a new device, disconnect from the first one.
        //todo: to test this, I need two wearable sensors.
        if(peripheralBLEManager == null) return;

        if(peripheralBLEManager.isConnected()){// If the device is connected.
            peripheralBLEManager.disconnect(). // disconnect from it.
                    done( device ->  {
                        Log.d(tag, device.getName() + " BLEManager Successfully disconnected");
                        peripheralBLEManager.close(); // close the manager whether the disconnection was a success or failure
                    }).
                    fail( (device, status) -> {
                        Log.d(tag, device.getName() + " BLEManager Failed to disconnected");
                        peripheralBLEManager.close(); // close the manager whether the disconnection was a success or failure
                    });
        }else{ // If the device was not connected, close the manager.
            peripheralBLEManager.close();
        }
    }

}