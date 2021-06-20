package com.ybeltagy.breathe.ble;

import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

import static androidx.core.content.ContextCompat.startForegroundService;

/**
 * Manages scanning for the wearable sensor
 */
public class BLEScanner {

    private static final String tag = BLEScanner.class.getName();

    /**
     * Used to make a timer.
     */
    private static final ScheduledExecutorService executorTimer = Executors.newSingleThreadScheduledExecutor();

    /**
     * Currently Not Scanning for a device.
     */
    private static final byte NOT_SCANNING = 0;

    /**
     * Currently Scanning for a device.
     */
    private static final byte SCANNING = 1;

    /**
     * Found a device but scanning has not been terminated yet.
     */
    private static final byte FOUND_DEVICE = 2;

    /**
     * Are we scanning for the wearable right now or not?
     */
    private volatile static byte scanningForWearable = NOT_SCANNING;

    /**
     * Are we scanning for the inhaler right now or not?
     */
    private volatile static byte scanningForInhaler = NOT_SCANNING;

    /**
     * returns the value of scanningForWearable which represents whether the device is currently scanning or not.
     * @return value of scanningForWearable.
     */
    private static byte getScanningForWearable(){
        synchronized (BLEScanner.class){ // It should be unnecessary to synchronize, but just to be safe.
            return scanningForWearable;
        }
    }

    /**
     * Sets the value of scanningForWearable which represents whether the device is currently scanning or not.
     * @param scanningForWearable
     */
    private static void setScanningForWearable(byte scanningForWearable) {
        synchronized (BLEScanner.class){
            BLEScanner.scanningForWearable = scanningForWearable;
        }
    }

    /**
     * returns the value of scanningForInhaler which represents whether the device is currently scanning or not.
     * @return value of scanningForInhaler.
     */
    private static byte getScanningForInhaler(){
        synchronized (BLEScanner.class){ // It should be unnecessary to synchronize, but just to be safe.
            return scanningForInhaler;
        }
    }

    /**
     * Sets the value of scanningForInhaler which represents whether the device is currently scanning or not.
     * @param scanningForInhaler
     */
    private static void setScanningForInhaler(byte scanningForInhaler) {
        synchronized (BLEScanner.class){
            BLEScanner.scanningForInhaler = scanningForInhaler;
        }
    }

    /**
     * Starts a scan (using the provided callbac) for a device with the provided uuid.
     * @param serviceUuid
     * @param callback
     * @return the scanner object
     */
    private static BluetoothLeScannerCompat startScanner(String serviceUuid, ScanCallback callback){

        // Initialize a scanner
        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();

        ScanSettings settings = new ScanSettings.Builder()
                // Accept both legacy and new Bluetooth advertisements. not 100% sure about its necessity right now.
                .setLegacy(false)
                // A powerful but power consuming scan.
                .setScanMode(no.nordicsemi.android.support.v18.scanner.ScanSettings.SCAN_MODE_LOW_LATENCY)
                // Report results immediately
                .setReportDelay(0)
                // If hardware batching is supported, use it (default is true).
                .setUseHardwareBatchingIfSupported(true)
                // Report a device on first advertisement packet you find that matches the filter.
                .setCallbackType(no.nordicsemi.android.support.v18.scanner.ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                // Determine a match even with a weak signal and few advertisement packets.
                .setMatchMode(no.nordicsemi.android.support.v18.scanner.ScanSettings.MATCH_MODE_AGGRESSIVE)
                // Match one advertisement per filter (only match one wearable sensor)
                .setNumOfMatches(no.nordicsemi.android.support.v18.scanner.ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .build();
        // todo: It will be useful if the behavior of the app is tested when two wearables or two inhalers are advertising.
        //  will probably connect to only one of them with no guarantee of which.

        // Make a filter that looks for the service UUID of the wearable Sensor.
        // I modified the wearable sensor so it advertises its service UUID.
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(serviceUuid)).build());

        scanner.startScan(filters, settings, callback); // start the scan asynchronously.

        return scanner;
    }

    /**
     * Starts a scan for a wearable and schedules a timer to stop the scan if the phone is not scanning.
     * If the phone is scanning, this method does nothing.
     *
     * This method is synchronized to avoid starting two scans at the same time.
     * @param context
     */
    public static void scanForWearableSensor(Context context) {

        synchronized (BLEScanner.class){

            // Confirm the state didn't change.
            if (getScanningForWearable() != NOT_SCANNING){
                Log.d(tag, "Already Scanning for wearable");
                return; // Already scanning for the wearable.
            }

            setScanningForWearable(SCANNING); // remember that the app is currently scanning
        }

        WearableScanCallback callback = new WearableScanCallback(context);

        BluetoothLeScannerCompat scanner = startScanner(BLEFinals.WEARABLE_SERVICE_UUID_STRING, callback);

        // schedule a thread to stop the scanner.
        executorTimer.schedule(new Runnable() {
                                   @Override
                                   public void run() {
                                       Log.d(tag, "In Scanner Timer: terminating wearable scan");
                                       scanner.stopScan(callback);
                                       setScanningForWearable(NOT_SCANNING);
                                   }}
                , BLEFinals.SCANNER_TIMEOUT_SECONDS, TimeUnit.SECONDS);

    }

    /**
     * If the phone is not scanning for an inhaler, starts a scan for an inhaler and schedules a timer to stop the scan
     * If the phone is scanning for an inhaler, this method does nothing.
     *
     * This method is synchronized to avoid starting two scans at the same time.
     * @param context
     */
    public static void scanForInhaler(Context context) {

        synchronized (BLEScanner.class){

            // Confirm the state didn't change.
            if (getScanningForInhaler() != NOT_SCANNING){
                Log.d(tag, "Already Scanning for inhaler");
                return; // Already scanning.
            }

            setScanningForInhaler(SCANNING); // remember that the app is currently scanning
        }

        InhalerScanCallback callback = new InhalerScanCallback(context);

        // Initialize a scanner
        BluetoothLeScannerCompat scanner = startScanner(BLEFinals.INHALER_SERVICE_UUID_STRING, callback);

        // schedule a thread to stop the scanner.
        executorTimer.schedule(new Runnable() {
                                   @Override
                                   public void run() {
                                       Log.d(tag, "In Scanner Timer: terminating inhaler scan");
                                       scanner.stopScan(callback);
                                       setScanningForInhaler(NOT_SCANNING);
                                   }}
                , BLEFinals.SCANNER_TIMEOUT_SECONDS, TimeUnit.SECONDS);

    }

    private static class WearableScanCallback extends ScanCallback{

        private Context scannerContext;

        public WearableScanCallback(Context scannerContext){
            this.scannerContext = scannerContext;
        }

        /**
         * Callback when a BLE advertisement has been found. Since the scanner filters on the wearable sensor UUID,
         * a wearable sensor was found.
         *
         * @param callbackType Determines how this callback was triggered. Could be one of
         *                     {@link no.nordicsemi.android.support.v18.scanner.ScanSettings#CALLBACK_TYPE_ALL_MATCHES},
         *                     {@link no.nordicsemi.android.support.v18.scanner.ScanSettings#CALLBACK_TYPE_FIRST_MATCH} or
         *                     {@link no.nordicsemi.android.support.v18.scanner.ScanSettings#CALLBACK_TYPE_MATCH_LOST}
         * @param result       A Bluetooth LE scan result.
         */
        @Override
        public void onScanResult(int callbackType, @NonNull ScanResult result) {
            super.onScanResult(callbackType, result);

            Log.d(tag, "Wearable found: " + result.getDevice().getName() + "| MAC: " + result.getDevice().toString());

            synchronized (BLEScanner.class){
                // Confirm that the scanner is still on and this is the first found device.
                byte currentState = getScanningForWearable();
                if(currentState == NOT_SCANNING || currentState == FOUND_DEVICE) return;

                // If the previous state was SCANNING, you can continue execution. Otherwise (FOUND_DEVICE or NOT_SCANNING), you can't.
                setScanningForWearable(FOUND_DEVICE);
            }


            // Pass the discovered device to the BLE service
            Intent intent = new Intent(scannerContext, BLEService.class);
            intent.setAction(BLEFinals.ACTION_CONNECT_TO_WEARABLE);
            intent.putExtra(BLEFinals.WEARABLE_BLUETOOTH_DEVICE_KEY, result.getDevice());
            startForegroundService(scannerContext, intent);

        }
    }

    private static class InhalerScanCallback extends ScanCallback{

        private Context scannerContext;

        public InhalerScanCallback(Context scannerContext){
            this.scannerContext = scannerContext;
        }

        /**
         * Callback when a BLE advertisement has been found. Since the scanner filters on the inhaler service uuid,
         * an inhaler was found.
         **
         * @param callbackType Determines how this callback was triggered. Could be one of
         *                     {@link ScanSettings#CALLBACK_TYPE_ALL_MATCHES},
         *                     {@link ScanSettings#CALLBACK_TYPE_FIRST_MATCH} or
         *                     {@link ScanSettings#CALLBACK_TYPE_MATCH_LOST}
         * @param result       A Bluetooth LE scan result.
         */
        @Override
        public void onScanResult(int callbackType, @NonNull ScanResult result) {
            super.onScanResult(callbackType, result);

            Log.d(tag, "Inhaler found: " + result.getDevice().getName() + "| MAC: " + result.getDevice().toString());

            synchronized (BLEScanner.class){
                // Confirm that the scanner is still on and this is the first found device.
                byte currentState = getScanningForInhaler();
                if(currentState == NOT_SCANNING || currentState == FOUND_DEVICE) return;

                // If the previous state was SCANNING, you can continue execution. Otherwise (FOUND_DEVICE or NOT_SCANNING), you can't.
                setScanningForInhaler(FOUND_DEVICE);
            }

            // Pass the discovered device to the BLE service
            Intent intent = new Intent(scannerContext, BLEService.class);
            intent.setAction(BLEFinals.ACTION_CONNECT_TO_INHALER);
            intent.putExtra(BLEFinals.INHALER_BLUETOOTH_DEVICE_KEY, result.getDevice());
            startForegroundService(scannerContext, intent);

        }
    }

}
