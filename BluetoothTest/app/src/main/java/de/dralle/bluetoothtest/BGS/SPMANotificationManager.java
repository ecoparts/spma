package de.dralle.bluetoothtest.BGS;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import de.dralle.bluetoothtest.GUI.MainActivity;
import de.dralle.bluetoothtest.R;

/**
 * Created by nils on 20.06.16.
 */
public class SPMANotificationManager {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = SPMANotificationManager.class.getName();
    /**
     * Notification id for the main notification
     */
    private static final int SPMA_NOTIFICATION_ID = 91;
    /**
     *
     */
    private Context con;


    public SPMANotificationManager(Context con) {
        this.con = con;
    }

    /**
     * Shows a notification, which informs the user about this service running
     */
    public void startNotification() {
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) con.getSystemService(Context.NOTIFICATION_SERVICE);
        //Prepare intent
        Intent intent = new Intent(con, MainActivity.class);
        //Wrap into pending intent
        PendingIntent pInten = PendingIntent.getActivity(con, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //Create Notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(con)
                .setSmallIcon(R.drawable.n_icon_test)
                .setContentTitle(con.getResources().getString(R.string.BGSNotificationTitle))
                .setContentText(con.getResources().getString(R.string.BGSNotificationText))
                .setOngoing(true);
        //set intent
        notificationBuilder.setContentIntent(pInten);
        //send
        notificationManager.notify(SPMA_NOTIFICATION_ID, notificationBuilder.build());
        Log.i(LOG_TAG, "Notification shown");
    }


    public void endNotification() {
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) con.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(SPMA_NOTIFICATION_ID);
        Log.i(LOG_TAG, "Notification deleted");
    }


}
