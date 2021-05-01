package com.ybeltagy.breathe.ble;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ybeltagy.breathe.WearableData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.data.Data;

// todo: fix logs.
// todo: consider making a singleton
public class WearableBLEManager extends BleManager {

    /**
     * Represents the wearable sensor.
     */

    public final static UUID SERVICE_UUID = UUID.fromString(BLEFinals.WEARABLE_SERVICE_UUID_STRING);
    public final static UUID WEARABLE_DATA_CHAR = UUID.fromString(BLEFinals.WEARABLE_DATA_CHAR_STRING);

    private static final String tag = "WearableBLEManager";

    // Client characteristics
    private BluetoothGattCharacteristic wearableDataCharacteristic = null;

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

            if (service == null) return false;

            wearableDataCharacteristic = service.getCharacteristic(WEARABLE_DATA_CHAR);

            if (wearableDataCharacteristic == null)
                return false;

            // Ensure wearableData characteristic has a read property.
            return (wearableDataCharacteristic.getProperties() &
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

            // TODO: to meet a stretch goal, you may need to enable Ble notificaionts/indications here.
        }

        @Override
        protected void onDeviceDisconnected() {
            // Device disconnected. Release your references here.
            wearableDataCharacteristic = null;
        }
    }

    public WearableData getWeatherData() {
        if (wearableDataCharacteristic == null)
            return null;

        final WearableData wearableData = new WearableData();

        try{
            readCharacteristic(wearableDataCharacteristic)
                    .with(new DataReceivedCallback() {
                        @Override
                        public void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {

                            ByteBuffer buf = ByteBuffer.wrap(data.getValue()).order(ByteOrder.LITTLE_ENDIAN);
                            wearableData.setTemperature(buf.getFloat()); // Actually use floats
                            wearableData.setHumidity(buf.getFloat());
                            wearableData.setCharacter(
                                    (char)((buf.get()-65) + 'A')
                            );
                            wearableData.setDigit(
                                    (char)((buf.get()-48) + '0')
                            );

                            log(Log.INFO, "Temperature: " + wearableData.getTemperature());
                            log(Log.INFO, "Humidity: " + wearableData.getHumidity());
                            log(Log.INFO, "Character: " + wearableData.getCharacter());
                            log(Log.INFO, "Digit: " + wearableData.getDigit());
                        }
                    }).await();
        }catch (Exception e){
            log(0, e.toString());
            return null;
        }

        return wearableData;
    }
    
}
