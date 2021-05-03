package com.ybeltagy.breathe;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.os.Bundle;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.widget.Toast;

import com.ybeltagy.breathe.ble.BLEScanner;
import com.ybeltagy.breathe.ble.BLEService;

/**
 * This activity contains the main logic of the Breathe app. It renders the UI and registers a
 * Bluetooth Broadcast receiver to listen for Bluetooth connection and disconnection to the phone
 * (to be changed to BLE).
 */
public class MainActivity extends AppCompatActivity {

    private static final String tag = "MainActivity";

    @RequiresApi(api = Build.VERSION_CODES.O) // for start foreground sevice. todo: remove
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //fixme: is there a better way to detect the app was opened?
        Intent foregroundServiceIntent = new Intent(this, BLEService.class);
        startForegroundService(foregroundServiceIntent); //FIXME why does it take a context too?


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


    /**
     * A testing method just for development.
     * @param view
     */
    public void testWearableData(View view) {
        //Fixme: inline thread is just a demo. Remove and use executorservice later.
        Context context = this;

        (new Thread() {
            public void run() {
                WearableData wearableData = BLEService.getWearableData();
                String temp = "No data";

                if(wearableData != null) temp = "Wearable Data!" +
                        "\nTemp = " + wearableData.getTemperature() + "\nHumidity: " + wearableData.getHumidity() +
                        "\nCharacter: " + wearableData.getCharacter() + "\nDigit: " + wearableData.getDigit();

                Log.d(tag,temp);
            }
        }).start();
    }

    /**
     * Starts a scan for a wearable sensor device. The wearable includes its service UUID in its advertisements.
     * The service UUID of the wearable sensor is used to filter for it.
     * @param view
     */
    public void scanForWearableSensor(View view) {

        if(!hasLocationPermissions()) return;

        if(!isBluetoothEnabled()) return;

        BLEScanner.scanForWearableSensor(this);
    }

    /**
     * Returns true if the app has ACCESS_FINE_LOCATION permission. If not, it requests it from the user and returns false.
     * @return true if app has ACCESS_FINE_LOCATION permission
     */
    private boolean hasLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 0);
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if Bluetooth is enabled. Otherwise, requests the user to enable it and returns false.
     * @return true if Bluetooth is enabled.
     */
    private boolean isBluetoothEnabled(){

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // There is no Bluetooth adapter (i.e, the device doesn't support bluetooth)
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "This device does not support bluetooth", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Bluetooth is supported and enabled.
        if(bluetoothAdapter.isEnabled()) {
            return true;
        }

        // Bluetooth is supported but not enabled.
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 0);

        return false;
    }
}