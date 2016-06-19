package de.dralle.bluetoothtest.DB;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by nils on 19.06.16.
 */
public class DeviceAccessHelper {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = DeviceAccessHelper.class.getName();
    /**
     * SQLite database connection
     */
    private SQLiteDatabase connection;

    public DeviceAccessHelper(SQLiteDatabase connection) {
        this.connection = connection;
    }
}
