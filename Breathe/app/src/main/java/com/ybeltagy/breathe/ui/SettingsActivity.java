package com.ybeltagy.breathe.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.ybeltagy.breathe.R;
import com.ybeltagy.breathe.ble.BLEScanner;
import com.ybeltagy.breathe.ble.BLEService;
import com.ybeltagy.breathe.collection.Export;
import com.ybeltagy.breathe.data.InhalerUsageEvent;
import com.ybeltagy.breathe.data.WearableData;

import java.util.List;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private List<InhalerUsageEvent> IUEList;
    private BreatheViewModel breatheViewModel;

    private static final int ACCESS_FINE_LOCATION_REQUEST = 1;
    private static final int ENABLE_BLUETOOTH = 2; // todo: consider moving to a centralized location

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        //Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // The activity's interactions with the data layer are through BreatheViewModel alone
        // view model for the recyclerview
        breatheViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(BreatheViewModel.class);

        //fixme: I'm not sure constantly updating the list is the most efficient way to do this.
        breatheViewModel.getAllInhalerUsageEvents().observe(this, inhalerUsageEvents -> this.IUEList = inhalerUsageEvents);

    }

    public void exportAllIUEs(View view){

        //fixme: is it bad that the UI is directly calling the Export?
        Intent fileIntent = Export.extractAllIUE(this, IUEList);
        if(fileIntent != null){
            startActivity(Intent.createChooser(fileIntent, "Export IUE"));
        }
        else{
            Toast.makeText(this, "Failed to Export Data", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * A testing method just for development.
     * @param view
     */
    public void testWearableData(View view) {
        (new Thread() {
            public void run() {
                WearableData wearableData = BLEService.getWearableData();
            }
        }).start();
    }

    public void onSimulateIUEButtonClick(View view) {
        breatheViewModel.simulateIUE(this);
    }

    /**
     * Starts a scan for a wearable sensor device. The wearable includes its service UUID in its advertisements.
     * The service UUID of the wearable sensor is used to filter for it.
     * @param view
     */
    public void onConnectToWearableButtonClick(View view) {
        scanForWearableSensor();
    }

    public void scanForWearableSensor(){
        if(!hasLocationPermissions()) return;

        if(!isBluetoothEnabled()) return;

        BLEScanner.scanForWearableSensor(this);
    }

    /**
     * Returns true if the app has ACCESS_FINE_LOCATION permission. If not, it requests it from the user and returns false.
     * @return true if app has ACCESS_FINE_LOCATION permission
     */
    private boolean hasLocationPermissions() {
        // M is for Marshmallow. Before Android Marshmallow, permissions are given at install time rather than at runtime.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)  return true;

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                Toast.makeText(this, "Bluetooth scanning requires location permission", Toast.LENGTH_SHORT).show();
            }

            requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, ACCESS_FINE_LOCATION_REQUEST);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == ACCESS_FINE_LOCATION_REQUEST){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                scanForWearableSensor();
            }else{
                Toast.makeText(this, "Can't scan without a Location permissions", Toast.LENGTH_SHORT).show();
            }
        }else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    // todo: implement a way an on request result that will restart the scan.
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
        Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
        startActivityForResult(enableBtIntent, 0); // fixme: startActivityForResult is now deprecated.

        return false;
    }

    public void onConnectToInhalerButtonClick(View view) {
        if(!hasLocationPermissions()) return;

        if(!isBluetoothEnabled()) return;

        BLEScanner.scanForInhaler(this);
    }

    public void clearIUEs(View view){
        breatheViewModel.clearIUEs();
        //TODO: If you use shared preferences to store the remaining IUES, consider updating it here.
        // Or maybe it is better to split them apart.
    }
}
