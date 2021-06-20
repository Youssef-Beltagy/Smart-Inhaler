package com.ybeltagy.breathe.ble;

/**
 * A centralized location to store constants
 */
public class BLEFinals {

    protected final static String WEARABLE_SERVICE_UUID_STRING = "25380284-e1b6-489a-bbcf-97d8f7470aa4";
    protected final static String WEARABLE_DATA_CHAR_UUID_STRING = "c3856cfa-4af6-4d0d-a9a0-5ed875d937cc";

    protected final static String INHALER_SERVICE_UUID_STRING = "e814c25d-7107-459e-b25d-23fec96d49da";
    protected final static String INHALER_IUE_CHAR_UUID_STRING = "d7dc7c50-48ce-45a4-9c3e-243a5bb75608";

    // TODO: This is a characteristic to synchronize the clock of the inhaler.
    //  This has not been implemented yet on the inhaler side.
    protected final static String INHALER_CLOCK_CHAR_UUID_STRING = "015529f7-554c-4138-a71e-40a2dfede10a";

    // Constants for the BLE notification channel.
    public static final String BLE_NOTIFICATION_CHANNEL_ID = "com.ybeltagy.breathe.ble.ble_notification_channel_id";
    protected static final String BLE_NOTIFICATION_CHANNEL_NAME = "BLE Connection";
    protected static final String BLE_NOTIFICATION_CHANNEL_DESCRIPTION = "Ready to connect to the inhaler and wearable sensor";

    // Constants for the BLE notification
    public static final int BLE_SERVICE_NOTIFICATION_ID = 1;
    protected static final String BLE_SERVICE_NOTIFICATION_TITLE = "Ready to connect";
    protected static final String BLE_SERVICE_NOTIFICATION_DESCRIPTION = "Ready to connect with the inhaler and wearable sensor";

    /**
     * The file name of the shared preferences. TODO: reference in the app level finals for record.
     */
    protected static final String BLE_SHARED_PREF_FILE_NAME = "com.ybeltagy.breathe.ble.bonded_devices";

    /**
     * The key used to pass a wearable sensor Bluetooth device in intents and to save the mac address in the shared preferences.
     */
    protected static final String WEARABLE_BLUETOOTH_DEVICE_KEY = "com.ybeltagy.breathe.ble.wearable_bluetooth_device_key";

    /**
     * The key used to pass an inhaler Bluetooth device in intents and to save the mac address in the shared preferences.
     */
    protected static final String INHALER_BLUETOOTH_DEVICE_KEY = "com.ybeltagy.breathe.ble.inhaler_bluetooth_device_key";

    // Actions.
    protected static final String ACTION_CONNECT_TO_WEARABLE = "com.ybeltagy.breathe.ble.connect_to_wearable_action";
    protected static final String ACTION_CONNECT_TO_INHALER = "com.ybeltagy.breathe.ble.connect_to_inhaler_action";
    protected static final String ACTION_BLUETOOTH_ENABLED = "com.ybeltagy.breathe.ble.bluetooth_enabled_action";
    protected static final String ACTION_BLUETOOTH_DISABLED = "com.ybeltagy.breathe.ble.bluetooth_disabled_action";

    /**
     * The timeout for scanning for the wearable or the inhaler.
     */
    protected static final int SCANNER_TIMEOUT_SECONDS = 15;
}
