package com.ybeltagy.breathe;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.LinkedList;
import java.util.List;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.observer.ConnectionObserver;
import no.nordicsemi.android.ble.utils.ILogger;

public class BLEService extends Service {

    BluetoothDevice bluetoothDevice = null;
    String tag = "BLEService";
    BluetoothGatt bluetoothGatt = null;
    Context curContext = this; // fixme delete if possible
    List<BluetoothGattCharacteristic> characteristicList= new LinkedList<>();
    private WearableBLEManager manager;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        Log.d(tag, "I'm in onCreate");
//        // Start up the thread running the service. Note that we create a
//        // separate thread because the service normally runs in the process's
//        // main thread, which we don't want to block. We also make it
//        // background priority so CPU-intensive work doesn't disrupt our UI.
    }

    // consider using a newer minSDK
    @SuppressLint("NewApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent.getAction().equals("STOP")){

            if(bluetoothGatt != null){
                // todo: schedule these calls with a timer.
                // todo research which of close and disconnect to call first.

                
                //bluetoothGatt.disconnect();
                //bluetoothGatt.close();


            }
            stopForeground(true);
            stopSelf();

        }else{

            Log.d(tag, "I'm in onStartCommand");
            bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("device");

            Log.d(tag, "Connected to "+bluetoothDevice.getName());
            Log.d(tag, "Device MAC: "+bluetoothDevice.toString());

            //private void createNotificationChannel() {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        CharSequence name = "Inhaler Notification Channel";
                        String description = "Inhaler Channel Description";
                        int importance = NotificationManager.IMPORTANCE_DEFAULT;
                        NotificationChannel channel = new NotificationChannel("Channel ID", name, importance);
                        channel.setDescription(description);
                        // Register the channel with the system; you can't change the importance
                        // or other notification behaviors after this
                        NotificationManager notificationManager = getSystemService(NotificationManager.class);
                        notificationManager.createNotificationChannel(channel);
                    }
            //}

            // Create an explicit intent for an Activity in your app
            Intent openAppIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingOpenAppIntent = PendingIntent.getActivity(this, 0, intent, 0);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "Channel ID")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Connection Status")
                    .setContentText("Connected to " + bluetoothDevice.getName())
                    .setContentIntent(pendingOpenAppIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

            // notificationId is a unique int for each notification that you must define
            startForeground(10, builder.build());

            //bluetoothDevice.

            manager = new WearableBLEManager(this);
            manager.setConnectionObserver(new ConnectionObserver() {
                @Override
                public void onDeviceConnecting(@NonNull BluetoothDevice device) {
                    Log.d(tag, "CO onDeviceConnecting");
                }

                @Override
                public void onDeviceConnected(@NonNull BluetoothDevice device) {
                    Log.d(tag, "CO onDeviceConnected");
                }

                @Override
                public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
                    Log.d(tag, "CO onDeviceFailedToConnect");
                }

                @Override
                public void onDeviceReady(@NonNull BluetoothDevice device) {
                    Log.d(tag, "CO onDeviceReady");
                }

                @Override
                public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
                    Log.d(tag, "CO onDeviceDisconnecting");
                }

                @Override
                public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) {
                    Log.d(tag, "CO onDeviceDisconnected");
                }
            });
            manager.connect(bluetoothDevice)
                    .timeout(100000)
                    .retry(3, 100)
                    .useAutoConnect(true)
                    .done(device -> Log.d(tag, "Device initiated: "+ device))
                    .enqueue();



//                bluetoothGatt = bluetoothDevice.connectGatt(curContext, true,
//                        new BluetoothGattCallback() {
//                            @Override
//                            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//                                super.onConnectionStateChange(gatt, status, newState);
//
//                                Log.d(tag, "Gatt: " + gatt.toString() + " |status: " + status + " |newState: " + newState);
//
//                                if(status == 0){ //todo: confirm that 0 represents a successful connection.
//                                    Boolean val = bluetoothGatt.discoverServices();
//                                    Log.d(tag, "DiscoverServices: " + val);
//                                }
////                        else{
////                            bluetoothDevice.connectGatt(curContext,true, this);
////                        }
//
//                                //todo: possibly 8 for disconnections
//                            }
//
//                            @Override
//                            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//                                super.onServicesDiscovered(gatt, status);
//                                Log.d(tag,"I'm in onServicesDiscovered");
//                                //gatt.getServices();
//                                for(BluetoothGattService gattService : gatt.getServices()){
//                                    Log.d(tag,"Service UUID: " + gattService.getUuid());
//                                    for(BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()){
//                                        Log.d(tag, "\tCharacteristic UUID: " + characteristic.getUuid());
//                                        Log.d(tag, "\tCharacteristic Value: " + characteristic.getStringValue(0));
//
//                                        if(characteristic.getUuid().toString().equals("c3856cfa-4af6-4d0d-a9a0-5ed875d937cc")
//                                                || characteristic.getUuid().toString().equals("e36d8858-cac3-4b03-9356-98b40fdd122e")
//                                                || characteristic.getUuid().toString().equals("03192130-212a-48c9-b058-ee4dace59d26")
//                                                || characteristic.getUuid().toString().equals("71eec950-3841-4984-83fa-0cfd8b9c901f") )
//                                            characteristicList.add(characteristic);
//                                    }
//                                }
//
//                                if(characteristicList.size() > 0){
//                                    gatt.readCharacteristic(characteristicList.get(0));
//                                    characteristicList.remove(0);
//                                }
//                                Log.d(tag,"I ended onServicesDiscovered");
//                            }
//
//                            @Override
//                            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//                                super.onCharacteristicRead(gatt, characteristic, status);
//                                String val = "";
//                                for(int i=0; i< characteristic.getValue().length ; i++) {
//                                    val += characteristic.getValue()[i] +" ";
//                                }
//                                Log.d(tag, "Characteristic ("+ characteristic.getUuid()+") = " + val);
//                                if(characteristicList.size() > 0){
//                                    gatt.readCharacteristic(characteristicList.get(0));
//                                    characteristicList.remove(0);
//                                }
//
//                            }
//
//                            @Override
//                            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//                                super.onCharacteristicChanged(gatt, characteristic);
//                            }
//                        }, BluetoothDevice.TRANSPORT_LE);


        }

        // If we get killed after returning from here, restart with the intent.
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy(){
        Log.d(tag, "I'm in onDestroy");
        //Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }


}
