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

    /**
     * Are we scanning right now or not?
     */
    private volatile static boolean scanningForWearable = false;

    /**
     * Used to make a timer.
     */
    private static final ScheduledExecutorService executorTimer = Executors.newSingleThreadScheduledExecutor();

    public static void scanForWearableSensor(Context context) {

        if (isScanningForWearable()){
            Log.d(tag, "already scanning");
            return; // Already scanning for the wearable.
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
                // Determine a match even with a week signal and few advertisment packets.
                .setNumOfMatches(no.nordicsemi.android.support.v18.scanner.ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                // Match one advertisement per filter (only match one wearable sensor)
                // todo: It will be useful if the behavior of the app is tested when two smart-wearables are advertising.
                .build();

        // Make a filter that looks for the service UUID of the wearable Sensor.
        // I modified the wearable sensor so it advertises its service UUID.
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString((BLEFinals.WEARABLE_SERVICE_UUID_STRING))).build());

        BLEScannerCallback callback = new BLEScannerCallback(context);

        // schedule a thread to stop the scanner.
        executorTimer.schedule(new Runnable() {
                                   @Override
                                   public void run() {
                                       Log.d(tag, "In Scanner Timer");
                                       stopScanning(scanner, callback);
                                   }}
                , BLEFinals.SCANNER_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        scanner.startScan(filters, settings, callback); // start the scan asynchronously.
        setScanningForWearable(true); // remember that the app is currently scanning

    }

    /**
     * returns the value of scanningForWearable which represents whether the device is currently scanning or not.
     * @return value of scanningForWearable.
     */
    public static boolean isScanningForWearable(){
        return scanningForWearable;
    }

    /**
     * Sets the value of scanningForWearable wich represents whether the device is currently scanning or not.
     * @param scanningForWearable
     */
    private static void setScanningForWearable(boolean scanningForWearable) {
        // It seems unnecessary to sycnhronize this method because only
        // one thread should modify this boolean at a time.
        BLEScanner.scanningForWearable = scanningForWearable;
    }

    /**
     * resets scanningForWearable to false and returns the previous value of scanning for wearable.
     * This method is as static synchronized method.
     * @return the previous value of scanning for wearable
     */
    private static synchronized boolean resetScanningForWearable() {
        boolean oldScanningForWearable = scanningForWearable;
        scanningForWearable = false;
        return oldScanningForWearable;
    }

    private static void stopScanning(BluetoothLeScannerCompat scanner, ScanCallback callback){
        Log.d(tag, "In stopScanning");
        if (resetScanningForWearable()) {
            Log.d(tag, "In stopScanning: terminating scan");
            scanner.stopScan(callback);
            setScanningForWearable(false);
        }
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

            // Pass the discovered device to the BLE service
            Intent intent = new Intent(scannerContext, BLEService.class);
            intent.setAction(BLEFinals.ACTION_CONNECT_TO_WEARABLE);
            intent.putExtra(BLEFinals.WEARABLE_BLUETOOTH_DEVICE_KEY, result.getDevice());
            startForegroundService(scannerContext, intent);

        }
    }

}
