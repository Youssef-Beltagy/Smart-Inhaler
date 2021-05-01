package com.ybeltagy.breathe.ble;

import android.bluetooth.BluetoothDevice;

public class BLEFinals {

    //todo: organize and clean later

    /**
     * A centralized location to store constants
     */

    protected final static String WEARABLE_SERVICE_UUID_STRING = "25380284-e1b6-489a-bbcf-97d8f7470aa4";

    protected final static String WEARABLE_DATA_CHAR_STRING = "c3856cfa-4af6-4d0d-a9a0-5ed875d937cc";

    public static final String BLE_NOTIFICATION_CHANNEL_ID = "com.ybeltagy.breathe.ble.ble_notification_channel_id";
    protected static final String BLE_NOTIFICATION_CHANNEL_NAME = "BLE Connection";
    protected static final String BLE_NOTIFICATION_CHANNEL_DESCRIPTION = "Ready to connect to the inhaler and wearable sensor";

    public static final int BLE_SERVICE_NOTIFICATION_ID = 1;
    protected static final String BLE_SERVICE_NOTIFICATION_TITLE = "Ready to connect";
    protected static final String BLE_SERVICE_NOTIFICATION_DESCRIPTION = "Ready to connect with the inhaler and wearable sensor";


    // fixme: consider making one shared pref file for all of the broject.
    protected static final String BLE_SHARED_PREF_FILE_NAME = "com.ybeltagy.breathe.ble.bonded_devices";

    protected static final String ACTION_CONNECT_TO_WEARABLE = "com.ybeltagy.breathe.ble.connect_to_wearable_action";

    protected static final String WEARABLE_BLUETOOTH_DEVICE_KEY = "com.ybeltagy.breathe.ble.wearable_bluetooth_device_key";
}
