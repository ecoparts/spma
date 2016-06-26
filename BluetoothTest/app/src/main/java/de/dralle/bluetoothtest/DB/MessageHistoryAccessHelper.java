package de.dralle.bluetoothtest.DB;

import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nils on 19.06.16.
 */
public class MessageHistoryAccessHelper {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = MessageHistoryAccessHelper.class.getName();
    /**
     * SQLite database connection
     */
    private SQLiteDatabase connection;

    public MessageHistoryAccessHelper(SQLiteDatabase connection) {
        this.connection = connection;
    }

    /**
     * Add a received message to the message history
     * @param senderId
     * @param message
     * @param userId

     */
    public void addReceivedMessage(int senderId, String message, int userId) {
        Log.i(LOG_TAG, "Logging message " + message + " from " + senderId + " for " + userId);
        if (senderId > -1) {
            ContentValues cv = new ContentValues();
            cv.put("Text", message);
            cv.put("Timestamp", System.currentTimeMillis() / 1000);
            cv.put("UserID", userId);
            cv.put("DeviceID", senderId);
            connection.insert("Received", null, cv);
            Log.i(LOG_TAG, "Logged message " + message + " from " + senderId + " for " + userId);
        } else {
            Log.i(LOG_TAG, "Logging message " + message + " from " + senderId + " for " + userId + " failed");
        }

    }

    /**
     * Add a send message to history
     * @param receiverId
     * @param message
     * @param userId

     */
    public void addSendMessage(int receiverId, String message, int userId) {
        Log.i(LOG_TAG, "Logging message " + message + " for " + receiverId + " from " + userId);
        if (receiverId > -1) {
            ContentValues cv = new ContentValues();
            cv.put("Text", message);
            cv.put("Timestamp", System.currentTimeMillis() / 1000);
            cv.put("UserID", userId);
            cv.put("DeviceID", receiverId);
            connection.insert("Send", null, cv);
            Log.i(LOG_TAG, "Logged message " + message + " for " + receiverId + " from " + userId);
        } else {
            Log.i(LOG_TAG, "Logging message " + message + " for " + receiverId + " from " + userId + " failed");
        }

    }

}
