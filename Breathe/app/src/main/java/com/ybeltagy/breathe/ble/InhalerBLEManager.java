package com.ybeltagy.breathe.ble;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ybeltagy.breathe.collection.BreatheRepository;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;


// TODO: Cleanup the information flow

/**
 * Represents the inhaler.
 */
public class InhalerBLEManager extends BleManager {

    public final static UUID SERVICE_UUID = UUID.fromString(BLEFinals.INHALER_SERVICE_UUID_STRING);
    public final static UUID IUE_CHAR_UUID = UUID.fromString(BLEFinals.INHALER_IUE_CHAR_UUID_STRING);

    private static final String tag = InhalerBLEManager.class.getName();

    // This characteristic sends indications for the iue Data.
    private BluetoothGattCharacteristic iueCharacteristic = null;

    InhalerBLEManager(@NonNull final Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new InhalerGattCallback();
    }

    /**
     * BluetoothGatt callbacks object.
     */
    private class InhalerGattCallback extends BleManagerGattCallback {

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

            iueCharacteristic = service.getCharacteristic(IUE_CHAR_UUID);

            if (iueCharacteristic == null)
                return false;

            // Ensure iue characteristic has an indication property.
            return (iueCharacteristic.getProperties() &
                    BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0; // fixme: change to indicate if the inhaler code is modified
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
        @SuppressLint("NewApi")
        protected void initialize() { //fixme: review again later.
            // You may enqueue multiple operations. A queue ensures that all operations are
            // performed one after another, but it is not required.
            beginAtomicRequestQueue()
                    .done(callback -> Log.d(tag, "Target initialized - callback" + callback.toString()))
                    .enqueue();

            setIndicationCallback(iueCharacteristic).with(
                    (device, data) ->
                    {
                    Log.d(tag, "Received IUE");

                    if (data.size() != 8){
                        Log.d(tag, "wrong size"); // corrupt data
                        return;
                    }

                    //fixme: be aware that this is a signed epoch.
                    ByteBuffer buf = ByteBuffer.wrap(data.getValue()).order(ByteOrder.LITTLE_ENDIAN);

                    int num = buf.getInt();

                    Instant iueTimestamp = Instant.ofEpochSecond((long)num);

                    Context curContext = getContext();
                    BreatheRepository.startDataCollection(iueTimestamp, curContext); // TODO: find a better place to call this

                    StringBuilder sb = new StringBuilder();

                    for(byte b : data.getValue()){
                        sb.append(String.format("%02X-", b));
                    }
                    Log.d(tag, sb.toString());
                    Log.d(tag, Integer.toString(num));
                    Log.d(tag, iueTimestamp.toString());

            });

            enableIndications(iueCharacteristic).enqueue(); // This enables indicates and guarantees bonding.
        }

        /**
         * When the device disconnects clear the characteristic.
         */
        @Override
        protected void onDeviceDisconnected() {
            // Device disconnected. Release your references here.
            iueCharacteristic = null;
        }
    }
    
}
