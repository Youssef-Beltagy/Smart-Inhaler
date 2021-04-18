package com.ybeltagy.breathe;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.content.Context; // fixme: delete! just for testing

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

import static androidx.core.content.ContextCompat.startForegroundService;

/**
 * This activity contains the main logic of the Breathe app. It renders the UI and registers a
 * Bluetooth Broadcast receiver to listen for Bluetooth connection and disconnection to the phone
 * (to be changed to BLE).
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // RecyclerView ----------------------------------------------------------------------------
        // populate the fake data for the RecyclerView

        // fake data - assumed number of doses in a canister
        // TODO: Replace with real number of doses in a canister
        // todo: or put that in the resources folder if we will continue using it.
        int totalDosesInCaniser = 200;

        // todo: replace with dynamically loaded data from the database
        LinkedList<String> eventList = new LinkedList<>();
        for (int i = 1; i <= 20; i++) {
            Date date = new Date(2020, 10, i); // todo: avoid using Date because it is partially deprecated.
            eventList.addLast(date.toString());
        }
        renderDiaryView(eventList);

        // ProgressBar -----------------------------------------------------------------------------
        renderMedStatusView(eventList, totalDosesInCaniser);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // render the Progress Bar for the medicine status in first pane (top of the screen)
    private void renderMedStatusView(List<String> eventList, int totalDosesInCaniser) {
        ProgressBar medicineStatusBar = findViewById(R.id.doses_progressbar);
        // update max amount of progress bar to number of doses in a full medicine canister
        medicineStatusBar.setMax(totalDosesInCaniser);
        // set doses taken shown in the ProgressBar to fake data increment (20 fake IUE events)
        // TODO: Delete fake data increment, replace with real increment (# of newly synced IUEs)
        medicineStatusBar.setProgress(eventList.size());

        // set text to show how many doses have been taken
        TextView dosesTakenText = findViewById(R.id.doses_textview);
        dosesTakenText.setText(String.format("%d / %d", medicineStatusBar.getProgress(), medicineStatusBar.getMax()));
    }

    // render the diary RecyclerView for the diary timeline of events in third pane
    // (bottom of screen)
    private void renderDiaryView(List<String> eventList) { // Use interfaces instead of actual classes
        RecyclerView iueRecyclerView = findViewById(R.id.diary_recyclerview);
        // make adapter and provide data to be displayed
        IUEListAdapter iueListAdapter = new IUEListAdapter(this, eventList);
        // connect adapter and recyclerView
        iueRecyclerView.setAdapter(iueListAdapter);
        // set layout manager for recyclerView
        iueRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void testWearableData(View view) {
        //Fixme: inline thread is just as a demo. Remove and use executorservice later.
        Context context = this;

        (new Thread() {
            public void run() {
                WearableData wearableData = WearableBLEService.getWearableData();
                String temp = "No data";

                if(wearableData != null) temp = "\nTemp = " + wearableData.getTemperature() + "\nHumidity: " + wearableData.getHumidity() +
                        "\nCharacter: " + wearableData.getCharacter() + "\nDigit" + wearableData.getDigit();
                Log.d("MainActivity",temp);

//                final String toastString = temp;
//                ContextCompat.getMainExecutor(context).execute(()  -> {
//                    Toast.makeText(context, toastString, Toast.LENGTH_LONG);
//                });
            }
        }).start();
    }

    //FIXME: continue
    public void scanForWearableSensor(View view) {
        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        ScanSettings settings = new ScanSettings.Builder()
                .setLegacy(false) // Not sure
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .setUseHardwareBatchingIfSupported(true)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .build();
        List<ScanFilter> filters = new ArrayList<>();


        //TODO: research context
        Context context = this;

        //TODO: put the UUID in a better place
        filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("25380284-e1b6-489a-bbcf-97d8f7470aa4")).build());
        scanner.startScan(filters, settings, new ScanCallback() {
            /**
             * Callback when a BLE advertisement has been found.
             *
             * @param callbackType Determines how this callback was triggered. Could be one of
             *                     {@link ScanSettings#CALLBACK_TYPE_ALL_MATCHES},
             *                     {@link ScanSettings#CALLBACK_TYPE_FIRST_MATCH} or
             *                     {@link ScanSettings#CALLBACK_TYPE_MATCH_LOST}
             * @param result       A Bluetooth LE scan result.
             */
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onScanResult(int callbackType, @NonNull ScanResult result) {
                super.onScanResult(callbackType, result);

                Log.d("main", "found: " + result.getDevice().toString());

                // refactor
                Intent foregroundServiceIntent = new Intent(context, WearableBLEService.class);
                foregroundServiceIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, result.getDevice());
                startForegroundService(foregroundServiceIntent);

                // FIXME: I'm not sure I'm stopping the scanning the correct way.
                BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
                scanner.stopScan(new ScanCallback() {
                    /**
                     * Callback when scan could not be started.
                     *
                     * @param errorCode Error code (one of SCAN_FAILED_*) for scan failure.
                     */
                    @Override
                    public void onScanFailed(int errorCode) {
                        super.onScanFailed(errorCode);
                    }
                });

            }
        });
    }
}