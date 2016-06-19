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

   
}
