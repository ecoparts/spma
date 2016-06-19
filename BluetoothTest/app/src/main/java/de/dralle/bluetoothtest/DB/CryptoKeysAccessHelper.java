package de.dralle.bluetoothtest.DB;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by nils on 19.06.16.
 */
public class CryptoKeysAccessHelper {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = CryptoKeysAccessHelper.class.getName();
    /**
     * SQLite database connection
     */
    private SQLiteDatabase connection;

    public CryptoKeysAccessHelper(SQLiteDatabase connection) {
        this.connection = connection;
    }

    /**
     * Write a new public key for a certain device
     * @param deviceID
     * @param data
     */
    public void insertDeviceRSAPublicKey(int deviceID, String data) {
        Log.i(LOG_TAG, "Writing RSA Public key of "+deviceID);
        if (deviceID > -1) {
            connection.delete("RSA","DeviceID = ?",new String[]{deviceID+""});
            ContentValues cv = new ContentValues();
            cv.put("DeviceID", deviceID);
            cv.put("Key",data);
            connection.insert("RSA",null,cv);
            Log.i(LOG_TAG,"New RSA Public key for device "+deviceID);
        }
    }
    public String getDeviceRSAPublicKey(int deviceID) {
        Log.i(LOG_TAG, "Reading RSA Public key of "+deviceID+" from db");
        if (deviceID > -1) {
            Cursor c=connection.rawQuery("select Key from RSA where DeviceID = ?",new String[]{deviceID+""});
            String key=null;
            if(c.moveToNext()){
                key=c.getString(0);
            }
            c.close();
            Log.i(LOG_TAG,"RSA Public key read "+key);
            return key;
        }
        return null;
    }
    public void insertDeviceAESKey(int deviceID, String data) {
        Log.i(LOG_TAG, "Writing AES key of "+deviceID);
        if (deviceID > -1) {
            connection.delete("AES","DeviceID = ?",new String[]{deviceID+""});
            ContentValues cv = new ContentValues();
            cv.put("DeviceID", deviceID);
            cv.put("Key",data);
            connection.insert("AES",null,cv);
            Log.i(LOG_TAG,"New AES key for device "+deviceID);
        }
    }
    public String getDeviceAESKey(int deviceID) {
        Log.i(LOG_TAG, "Reading AES key of "+deviceID+" from db");
        if (deviceID > -1) {
            Cursor c=connection.rawQuery("select Key from AES where DeviceID = ?",new String[]{deviceID+""});
            String key=null;
            if(c.moveToNext()){
                key=c.getString(0);
            }
            c.close();
            Log.i(LOG_TAG,"AES key read "+key);
            return key;
        }
        return null;
    }

}
