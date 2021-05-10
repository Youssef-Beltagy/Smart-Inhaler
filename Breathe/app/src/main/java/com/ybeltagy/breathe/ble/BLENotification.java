package com.ybeltagy.breathe.ble;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.ybeltagy.breathe.ui.MainActivity;
import com.ybeltagy.breathe.R;

import static androidx.core.content.ContextCompat.getSystemService;

import static com.ybeltagy.breathe.ble.BLEFinals.BLE_NOTIFICATION_CHANNEL_DESCRIPTION;
import static com.ybeltagy.breathe.ble.BLEFinals.BLE_NOTIFICATION_CHANNEL_ID;
import static com.ybeltagy.breathe.ble.BLEFinals.BLE_NOTIFICATION_CHANNEL_NAME;
import static com.ybeltagy.breathe.ble.BLEFinals.BLE_SERVICE_NOTIFICATION_DESCRIPTION;
import static com.ybeltagy.breathe.ble.BLEFinals.BLE_SERVICE_NOTIFICATION_TITLE;

public class BLENotification {


    /**
     * Create the NotificationChannel, but only on API 26+ because
     * the NotificationChannel class is new and not in the support library
     * @param context
     */
    protected static void createNotificationChannel(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // TODO: change to IMPORTANCE_NONE later and observe the behavior.
            NotificationChannel channel = new NotificationChannel(BLE_NOTIFICATION_CHANNEL_ID, BLE_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
            channel.setDescription(BLE_NOTIFICATION_CHANNEL_DESCRIPTION);

            // Register the channel with the system; you can't change the importance or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(context, NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Builds and displays a notification for the BLEService
     * @param context
     * @return
     */
    public static Notification getBLEServiceNotification(Context context){

        createNotificationChannel(context);
        // If the user presses the notification, open the main activity
        Intent openAppIntent = new Intent(context, MainActivity.class);

        //Use PendingIntent.getActivity to make a PendingIntend that opens the main activity of the app.
        PendingIntent pendingOpenAppIntent = PendingIntent.getActivity(context, 0, openAppIntent, 0);

        // Build a notification in the BLE_NOTIFICATION_CHANNEL
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, BLE_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // An icon is necessary. I used the launcher icon.
                .setContentTitle(BLE_SERVICE_NOTIFICATION_TITLE)
                .setContentText(BLE_SERVICE_NOTIFICATION_DESCRIPTION)
                .setContentIntent(pendingOpenAppIntent)
                .setPriority(NotificationCompat.PRIORITY_MIN);

        return builder.build(); // return the built notification object
    }
}
