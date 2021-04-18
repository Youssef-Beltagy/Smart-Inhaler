package com.ybeltagy.breathe;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import no.nordicsemi.android.ble.observer.BondingObserver;
import no.nordicsemi.android.ble.observer.ConnectionObserver;

public class WearableBLEService extends Service {

    /**
     * Debugging tag
     */
    private static String tag = "WearableBLEService";

    /**
     * BLEManager for the wearable.
     */
    private static volatile WearableBLEManager manager = null; //TODO: consider multi-threading

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(tag, "In onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(tag, "I'm in onStartCommand");

        BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        // TODO: identify whether it is a pin or an inhaler

        Log.d(tag, "Connected to " + bluetoothDevice.getName());
        Log.d(tag, "Device MAC: " + bluetoothDevice.toString());

        manager = new WearableBLEManager(this);

        // TODO: consider making this service implement ConnectionObserver and passing the service as the ConnectionObserver
        manager.setConnectionObserver(new ConnectionObserver() {
            @Override
            public void onDeviceConnecting(@NonNull BluetoothDevice device) {
                Log.d(tag, "CO onDeviceConnecting");
            }

            /**
             * Called when the device has been connected. This does not mean that the application may start
             * communication. Service discovery will be handled automatically after this call.
             *
             * @param device the device that got connected.
             */
            @Override
            public void onDeviceConnected(@NonNull BluetoothDevice device) {
                Log.d(tag, "CO onDeviceConnected");
                startThisService(bluetoothDevice);
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
                // Stop this service when the device disconnects.
                stopThisService();
            }
        });

        manager.connect(bluetoothDevice)
                //.timeout(100000)
                //.retry(3, 100)
                .useAutoConnect(true)
                .done(device -> Log.d(tag, "Device connected: " + device))
                .enqueue();
        // If we get killed after returning from here, restart with the intent.
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Log.d(tag, "In onDestroy");
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    private void startThisService(BluetoothDevice bluetoothDevice){
        //-------------------------------------Read up on notifications
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
        PendingIntent pendingOpenAppIntent = PendingIntent.getActivity(this, 0, openAppIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "Channel ID")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Connection Status")
                .setContentText("Connected to " + bluetoothDevice.getName())
                .setContentIntent(pendingOpenAppIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        startForeground(10, builder.build());
        //----------------------------
    }

    private void stopThisService(){
        stopForeground(true); // TODO: there are weird edge cases here. -- look at example again
        stopSelf();
    }

    public static WearableData getWearableData(){
        if(manager == null) return null;

        return manager.getWeatherData();
    }

}
