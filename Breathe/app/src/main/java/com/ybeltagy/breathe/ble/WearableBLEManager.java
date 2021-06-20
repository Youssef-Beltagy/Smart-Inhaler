package com.ybeltagy.breathe.ble;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ybeltagy.breathe.data.WearableData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;

public class WearableBLEManager extends BleManager {

    /**
     * Represents the wearable sensor.
     */

    public final static UUID SERVICE_UUID = UUID.fromString(BLEFinals.WEARABLE_SERVICE_UUID_STRING);
    public final static UUID WEARABLE_DATA_CHAR_UUID = UUID.fromString(BLEFinals.WEARABLE_DATA_CHAR_UUID_STRING);

    private static final String tag = "WearableBLEManager";

    // This characteristic contains all the wearable Data.
    private BluetoothGattCharacteristic wearableDataCharacteristic = null;

    WearableBLEManager(@NonNull final Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new WearableGattCallback();
    }

    public WearableData getWeatherData() {
        if (wearableDataCharacteristic == null)
            return null;

        final WearableData wearableData = new WearableData();

        try{
            readCharacteristic(wearableDataCharacteristic)
                    .with( (device, data) -> { // Data received Callback

                        //TODO: Add support for the PM2.5 Sensor data.

                        if(data == null || data.getValue() == null) return;

                        //Parse the input in Little_endian because the esp32/stm32 are little endian
                        ByteBuffer buf = ByteBuffer.wrap(data.getValue()).order(ByteOrder.LITTLE_ENDIAN);

                        // get the temperature in little endian
                        wearableData.setTemperature(buf.getFloat());

                        // get the humidity in little endian
                        wearableData.setHumidity(buf.getFloat());

                        // get the character (converts from UTF-8 in the message to Java's UTF-16)
                        wearableData.setCharacter(
                                (char) buf.get()
                        );

                        // get the digit (converts from UTF-8 in the message to Java's UTF-16)
                        wearableData.setDigit(
                                (char) buf.get()
                        );

                        Log.d(tag, "Wearable Data!");
                        Log.d(tag, "Temperature: " + wearableData.getTemperature());
                        Log.d(tag, "Humidity: " + wearableData.getHumidity());
                        Log.d(tag, "Character: " + wearableData.getCharacter());
                        Log.d(tag, "Digit: " + wearableData.getDigit());
                    }).await();
        }catch (Exception e){
            Log.d(tag, e.toString());
            return null;
        }

        return wearableData;
    }

    /**
     * BluetoothGatt callbacks object.
     */
    private class WearableGattCallback extends BleManagerGattCallback {

        /**
         * This method will be called when the device is connected and services are discovered.
         * You need to obtain references to the characteristics and descriptors that you will use.
         * @param gatt represents a Gatt connection object
         * @return  true if all required services are found, false otherwise.
         */
        @Override
        public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(SERVICE_UUID);

            if (service == null) return false;

            wearableDataCharacteristic = service.getCharacteristic(WEARABLE_DATA_CHAR_UUID);

            if (wearableDataCharacteristic == null)
                return false;

            // Ensure wearableData characteristic has a read property.
            return (wearableDataCharacteristic.getProperties() &
                    BluetoothGattCharacteristic.PROPERTY_READ) != 0;
        }

        /**
         * If you have any optional services, allocate them here. Return true only if
         * they are found.
         * <p>
         *  We don't have optional services, so we just call super.
         * @param gatt
         * @return
         */
        @Override
        protected boolean isOptionalServiceSupported(@NonNull final BluetoothGatt gatt) {
            return super.isOptionalServiceSupported(gatt);
        }

        /**
         * Initialize the wearable device. Initializes the request queue.
         */
        @Override
        protected void initialize() {
            // You may enqueue multiple operations. A queue ensures that all operations are
            // performed one after another, but it is not required.
            beginAtomicRequestQueue()
                    .done(callback -> Log.d(tag, "Target initialized - callback" + callback.toString()))
                    .enqueue();

            readCharacteristic(wearableDataCharacteristic).enqueue();
            // Make a read request to guarantee bonding for the stm32 wearable

            // TODO: to meet a stretch goal, you may need to enable Ble notificaionts/indications here.
            //  You need to enable notifications and set required
            //  MTU or write some initial data. Do it here.
        }

        /**
         * When the device disconnects clear the characteristic.
         */
        @Override
        protected void onDeviceDisconnected() {
            // Device disconnected. Release your references here.
            wearableDataCharacteristic = null;
        }
    }
    
}
