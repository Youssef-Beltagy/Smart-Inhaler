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
 * Manages scanning for the wearable sensor and the smart inhaler.
 */
public class BLEScanner{

    private static final String tag = "BLEScanner";

    //Todo: After you press the scan button and you get the permission, it restarts the scan.
    //foreground vs background -> make the service and if necessary, make it a foreground one.
    //information flow
    //permissions
    //Integrating with

    /**
     * Are we scanning right now or not?
     */
    private volatile static byte scanningForWearable = 0;

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
     * Used to make a timer.
     */
    private static final ScheduledExecutorService executorTimer = Executors.newSingleThreadScheduledExecutor();

    /**
     * Starts a scan for a wearable and schedules a timer to stop the scan if the phone is not scanning.
     * If the phone is scanning, this method does nothing.
     *
     * This method is synchronized to avoid starting two scans at the same time.
     * @param context
     */
    public static void scanForWearableSensor(Context context) {

        synchronized (BLEScanner.class){

            if (getScanningForWearable() != NOT_SCANNING){
                Log.d(tag, "already scanning");
                return; // Already scanning for the wearable.
            }

            setScanningForWearable(SCANNING); // remember that the app is currently scanning
        }

        // Initialize a scanner
        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();

        ScanSettings settings = new ScanSettings.Builder()
                .setLegacy(false)
                // Accept both legacy and new Bluetooth advertisementsnot 100% sure about its necessity right now.
                // The wearable sensor uses legacy advertisement
                .setScanMode(no.nordicsemi.android.support.v18.scanner.ScanSettings.SCAN_MODE_LOW_LATENCY)
                // A powerful but power consuming scan.
                .setReportDelay(0)
                // Report results immediately
                .setUseHardwareBatchingIfSupported(true)
                // If hardware batching is supported, use it (default is true).
                .setCallbackType(no.nordicsemi.android.support.v18.scanner.ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                // Report a device on first advertisement packet you find that matches the filter.
                .setMatchMode(no.nordicsemi.android.support.v18.scanner.ScanSettings.MATCH_MODE_AGGRESSIVE)
                // Determine a match even with a weak signal and few advertisement packets.
                .setNumOfMatches(no.nordicsemi.android.support.v18.scanner.ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                // Match one advertisement per filter (only match one wearable sensor)
                // todo: It will be useful if the behavior of the app is tested when two smart-wearables are advertising.
                .build();

        // Make a filter that looks for the service UUID of the wearable Sensor.
        // I modified the wearable sensor so it advertises its service UUID.
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString((BLEFinals.WEARABLE_SERVICE_UUID_STRING))).build());

        BLEScannerCallback callback = new BLEScannerCallback(context);

        scanner.startScan(filters, settings, callback); // start the scan asynchronously.

        // schedule a thread to stop the scanner.
        executorTimer.schedule(new Runnable() {
                                   @Override
                                   public void run() {
                                       Log.d(tag, "In Scanner Timer");
                                       stopScanning(scanner, callback);
                                   }}
                , BLEFinals.SCANNER_TIMEOUT_SECONDS, TimeUnit.SECONDS);

    }

    /**
     * returns the value of scanningForWearable which represents whether the device is currently scanning or not.
     * @return value of scanningForWearable.
     */
    public static byte getScanningForWearable(){
        return scanningForWearable;
    }

    /**
     * Sets the value of scanningForWearable which represents whether the device is currently scanning or not.
     * @param scanningForWearable
     * @return the previous value of scanningForWearable
     */
    private static synchronized byte setScanningForWearable(byte scanningForWearable) {
        // It seems unnecessary to synchronize this method because only
        // one thread should modify this boolean at a time.
        byte temp = BLEScanner.scanningForWearable;
        BLEScanner.scanningForWearable = scanningForWearable;
        return temp;
    }

    private static void stopScanning(BluetoothLeScannerCompat scanner, ScanCallback callback){
        Log.d(tag, "In stopScanning: terminating scan");
        scanner.stopScan(callback);
        setScanningForWearable(NOT_SCANNING);
    }

    private static class BLEScannerCallback extends ScanCallback{

        private Context scannerContext;

        public BLEScannerCallback(Context scannerContext){
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

            Log.d(tag, "found: " + result.getDevice().toString());

            synchronized (BLEScanner.class){
                // If the scan was stopped immediately before this callback, don't jeopardize the state of the scanner.
                if(getScanningForWearable() == NOT_SCANNING) return;

                // If the previous state was SCANNING, you can continue execution. Otherwise, you can't.
                if(setScanningForWearable(FOUND_DEVICE) != SCANNING) return;
            }


            // Pass the discovered device to the BLE service
            Intent intent = new Intent(scannerContext, BLEService.class);
            intent.setAction(BLEFinals.ACTION_CONNECT_TO_WEARABLE);
            intent.putExtra(BLEFinals.WEARABLE_BLUETOOTH_DEVICE_KEY, result.getDevice());
            startForegroundService(scannerContext, intent);

        }
    }

}
