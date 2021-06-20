package com.ybeltagy.breathe.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.util.Log;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.observer.ConnectionObserver;

//FIXME: Since we are not doing anything fancy depending on the connection callbacks,
// This class is not useful. However, because in the future, a future team might want to use it,
// we will leave it

/**
 * Contain callback methods for BLE connection updates.
 */
public class BLEConnectionObserver implements ConnectionObserver {

    private static final String tag = "BLEConnectionObserver";

    /**
     * Called when the Android device started connecting to given device.
     * The {@link #onDeviceConnected(BluetoothDevice)} will be called when the device is connected,
     * or {@link #onDeviceFailedToConnect(BluetoothDevice, int)} if connection will fail.
     *
     * @param device the device that got connected.
     */
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
    }

    /**
     * Called when the device failed to connect.
     *
     * @param device the device that failed to connect.
     * @param reason the reason of failure.
     */
    @Override
    public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
        Log.d(tag, "CO onDeviceFailedToConnect");
    }

    /**
     * Method called when all initialization requests has been completed.
     *
     * @param device the device that get ready.
     */
    @Override
    public void onDeviceReady(@NonNull BluetoothDevice device) {

        Log.d(tag, "CO onDeviceReady");

    }

    /**
     * Called when user initialized disconnection.
     *
     * @param device the device that gets disconnecting.
     */
    @Override
    public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {

    }

    /**
     * Called when the device has disconnected (when the callback returned
     * {@link BluetoothGattCallback#onConnectionStateChange(BluetoothGatt, int, int)} with state
     * DISCONNECTED).
     *
     * @param device the device that got disconnected.
     * @param reason reason of the disconnect (mapped from the status code reported by the GATT
     */
    @Override
    public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) {
        Log.d(tag, "CO onDeviceDisconnected");
    }
}