package com.ybeltagy.breathe;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;

// todo: fix logs.

public class WearableBLEManager extends BleManager {
    final static UUID SERVICE_UUID = UUID.fromString("25380284-e1b6-489a-bbcf-97d8f7470aa4");
    final static UUID TEMP_CHAR   = UUID.fromString("c3856cfa-4af6-4d0d-a9a0-5ed875d937cc");
    final static UUID HUMID_CHAR  = UUID.fromString("e36d8858-cac3-4b03-9356-98b40fdd122e");
    final static UUID CHAR_CHAR   = UUID.fromString("03192130-212a-48c9-b058-ee4dace59d26");
    final static UUID DIG__CHAR  = UUID.fromString("71eec950-3841-4984-83fa-0cfd8b9c901f");

    private static final String tag = "WearableBLEManager";

    // Client characteristics
    private BluetoothGattCharacteristic temperatureCharacteristic = null;
    private BluetoothGattCharacteristic humidityCharacteristic = null;
    private BluetoothGattCharacteristic characterCharacteristic = null;
    private BluetoothGattCharacteristic digitCharacteristic = null;


    WearableBLEManager(@NonNull final Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new WearableGattCallback();
    }

    @Override
    public void log(final int priority, @NonNull final String message) {
        // fixme: different from example. Remake if necessary.
        Log.d(tag, message);
    }

    /**
     * BluetoothGatt callbacks object.
     */
    private class WearableGattCallback extends BleManagerGattCallback {

        // This method will be called when the device is connected and services are discovered.
        // You need to obtain references to the characteristics and descriptors that you will use.
        // Return true if all required services are found, false otherwise.
        @Override
        public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(SERVICE_UUID);

            if(service == null) return false;

            temperatureCharacteristic = service.getCharacteristic(TEMP_CHAR);
            humidityCharacteristic = service.getCharacteristic(HUMID_CHAR);
            characterCharacteristic = service.getCharacteristic(CHAR_CHAR);
            digitCharacteristic = service.getCharacteristic(DIG__CHAR);

            if(temperatureCharacteristic == null
                || humidityCharacteristic == null
                || characterCharacteristic == null
                || digitCharacteristic == null)
                    return false;

            // Ensure all characteristics have a read property.
            return (temperatureCharacteristic.getProperties() &
                    humidityCharacteristic.getProperties() &
                    characterCharacteristic.getProperties() &
                    digitCharacteristic.getProperties() &
                    BluetoothGattCharacteristic.PROPERTY_READ) != 0;
        }

        // If you have any optional services, allocate them here. Return true only if
        // they are found.
        @Override
        protected boolean isOptionalServiceSupported(@NonNull final BluetoothGatt gatt) {
            return super.isOptionalServiceSupported(gatt);
        }

        // Initialize your device here. Often you need to enable notifications and set required
        // MTU or write some initial data. Do it here.
        @Override
        protected void initialize() {
            // You may enqueue multiple operations. A queue ensures that all operations are
            // performed one after another, but it is not required.
            beginAtomicRequestQueue()
                    .done(callback -> log(Log.INFO, "Target initialized - callback" + callback.toString()))
                    .enqueue();
            // You may easily enqueue more operations here like such:
            readCharacteristic(temperatureCharacteristic)
                    .done(value -> log(Log.INFO, "Temp: " + value))
                    .enqueue();
            readCharacteristic(humidityCharacteristic)
                    .done(value -> log(Log.INFO, "Humidity: " + value))
                    .enqueue();
            readCharacteristic(characterCharacteristic)
                    .done(value -> log(Log.INFO, "Char: " + value ))
                    .enqueue();
            readCharacteristic(digitCharacteristic)
                    .done(value -> log(Log.INFO, "Digit: " + value))
                    .enqueue();
            // Set a callback for your notifications. You may also use waitForNotification(...).
            // Both callbacks will be called when notification is received.
//            setNotificationCallback(firstCharacteristic, callback);
//            // If you need to send very long data using Write Without Response, use split()
//            // or define your own splitter in split(DataSplitter splitter, WriteProgressCallback cb).
//            writeCharacteristic(secondCharacteristic, "Very, very long data that will no fit into MTU")
//                    .split()
//                    .enqueue();
        }

        @Override
        protected void onDeviceDisconnected() {
            // Device disconnected. Release your references here.
            temperatureCharacteristic = null;
            humidityCharacteristic = null;
            characterCharacteristic = null;
            digitCharacteristic = null;
        }
    }

    // Define your API.

//    private abstract class FluxHandler implements ProfileDataCallback {
//        @Override
//        public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
//            // Some validation?
//            if (data.size() != 1) {
//                onInvalidDataReceived(device, data);
//                return;
//            }
//            onFluxCapacitorEngaged();
//        }
//
//        abstract void onFluxCapacitorEngaged();
//    }
//
//    /** Initialize time machine. */
//    public void enableFluxCapacitor(final int year) {
//        waitForNotification(firstCharacteristic)
//                .trigger(
//                        writeCharacteristic(secondCharacteristic, new FluxJumpRequest(year))
//                                .done(device -> log(Log.INDO, "Power on command sent"))
//                )
//                .with(new FluxHandler() {
//                    public void onFluxCapacitorEngaged() {
//                        log(Log.WARN, "Flux Capacitor enabled! Going back to the future in 3 seconds!");
//
//                        sleep(3000).enqueue();
//                        write(secondCharacteristic, "Hold on!".getBytes())
//                                .done(device -> log(Log.WARN, "It's " + year + "!"))
//                                .fail((device, status) -> "Not enough flux? (status: " + status + ")")
//                                .enqueue();
//                    }
//                })
//                .enqueue();
//    }

    /**
     * Aborts time travel. Call during 3 sec after enabling Flux Capacitor and only if you don't
     * like 2020.
     */
//    public void abort() {
//        cancelQueue();
//    }
}
