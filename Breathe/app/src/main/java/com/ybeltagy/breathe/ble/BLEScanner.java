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

//TODO: consider using a localBroadcast or a broadcast that is directed towards our broadcast receiver.
//This is apparently a good performance improvement and a best practice.

//TODO consider storing the Bluetooth device in shared preferences
//TODO consider adding support for multiple devices

//fixme: put constants in a utility class.

//TODO: how safe is it to pass a context object around?
//TODO: request to open BLE and Location services

//TODO: attempt to stop scanning when a device is found

public class BLEScanner{

    /**
     * Scans for a BLEDevice and makes a Broadcast when it finds one.
     */

    //fixme: consider storing a bluetooth adapter or other references as a performance improvement.

    private static final String tag = "BLEScanner";

    private volatile static boolean scanningForWearable = false;

    //TODO: Consider making a centralized threadpool for everything Repo+DB+BLE
    //TODO: discuss program/code organization with Sarah
    //Ensure that two timers can happen concurrenty.
    private static final ScheduledExecutorService executorTimer = Executors.newSingleThreadScheduledExecutor();

    //TODO: consider deleting
    //Only executed once when the program is loaded.
    static {
        Log.d(tag, "static block scanningForWearable = false");
        scanningForWearable = false;
    }

    //fixme: assess the necessity of synchronized.
    //It is not a big deal, but I'm a little worried about concurrency.
    public static boolean isScanningForWearable() {
        return scanningForWearable;
    }

    public static void setScanningForWearable(boolean scanningForWearable) {
        BLEScanner.scanningForWearable = scanningForWearable;
    }

    //FIXME: continue
    //FIXME: comment on parameters.
    public static void scanForWearableSensor(Context context) {

        if (isScanningForWearable()){
            Log.d(tag, "already scanning");
            return; // Already scanning for the wearable.
        }

        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();

        ScanSettings settings = new ScanSettings.Builder()
                .setLegacy(false) // Not sure
                .setScanMode(no.nordicsemi.android.support.v18.scanner.ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .setUseHardwareBatchingIfSupported(true)
                .setCallbackType(no.nordicsemi.android.support.v18.scanner.ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                .setMatchMode(no.nordicsemi.android.support.v18.scanner.ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(no.nordicsemi.android.support.v18.scanner.ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .build();

        List<ScanFilter> filters = new ArrayList<>();

        filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString((BLEFinals.WEARABLE_SERVICE_UUID_STRING))).build());

        BLEScannerCallback callback = new BLEScannerCallback(context);

        scanner.startScan(filters, settings, callback);
        setScanningForWearable(true);

        executorTimer.schedule(new Runnable() {
            @Override
            public void run() {//TODO: consider synchronizing.
                if (isScanningForWearable()) {
                    Log.d(tag, "Stopping scanning");
                    scanner.stopScan(callback);
                    setScanningForWearable(false);
                }
            }}
            , 30, TimeUnit.SECONDS);

    }

    private static class BLEScannerCallback extends ScanCallback{

        private Context scannerContext;

        public BLEScannerCallback(Context scannerContext){
            this.scannerContext = scannerContext;
        }


        //TODO: assess the feasibility of stopping a scan after a successful callback.
        /**
         * Callback when a BLE advertisement has been found.
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

            // refactor

            Intent intent = new Intent(scannerContext, BLEService.class);
            intent.setAction(BLEFinals.ACTION_CONNECT_TO_WEARABLE);
            // Set the optional additional information in extra field.
            intent.putExtra(BLEFinals.WEARABLE_BLUETOOTH_DEVICE_KEY, result.getDevice());
            startForegroundService(scannerContext, intent); //FIXME why does it take a context too?

        }
    }

}
