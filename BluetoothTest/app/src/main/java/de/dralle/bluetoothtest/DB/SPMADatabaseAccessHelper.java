package de.dralle.bluetoothtest.DB;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nils on 11.06.16.
 */
public class SPMADatabaseAccessHelper {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = SPMADatabaseAccessHelper.class.getName();

    private static SPMADatabaseAccessHelper instance = null;

    private Context context;

    private SQLiteOpenHelper db = null;

    private SQLiteDatabase writeConnection = null;
    private SQLiteDatabase readConnection = null;
    /**
     * Used to read/write user tables
     */
    private UserAccessHelper userAccessHelper;
    /**
     * Used to read/write device tables
     */
    private DeviceAccessHelper deviceAccessHelper;

    private SPMADatabaseAccessHelper(Context context) {
        this.context = context;
        db = new SPMADatabaseHelper(context);
        writeConnection=db.getWritableDatabase();
        readConnection=db.getReadableDatabase();
        userAccessHelper=new UserAccessHelper(writeConnection);
        deviceAccessHelper=new DeviceAccessHelper(writeConnection);
    }
    public static SPMADatabaseAccessHelper getInstance(Context context){
        if(instance==null){
            instance=new SPMADatabaseAccessHelper(context);
        }
        return instance;
    }
    @Deprecated
    public User addUser(String name) {
        return userAccessHelper.addUser(name);
    }

    /**
     * Updates a database User
     *
     * @param u Userdata to be updated
     * @return User given
     */
    public User createOrUpdateUser(User u) {
        return userAccessHelper.createOrUpdateUser(u);
    }

    /**
     * Gets a user from the db
     * @param id User id
     * @return User data. Can be null.
     */
    public User getUser(int id) {
        return userAccessHelper.getUser(id);

    }

    /**
     * Looks for a device´s friendly name. This is not the device´s bluetooth name, but the user name used by the app on the remote device
     *
     * @param address Remote device address
     * @return Remote device friendly name/remote device user name. returns address when no name was found.
     */
    public String getDeviceFriendlyName(String address) {
        return deviceAccessHelper.getDeviceFriendlyName(address);
    }

    /**
     * Looks for a device´s id from an address
     *
     * @param address Remote device address
     * @return Remote device database id. -1 if no device is found
     */
    public int getDeviceID(String address) { //TODO: unify with above
        return deviceAccessHelper.getDeviceID(address);

    }
    /**
     * Looks for a device´s id from an address
     *
     * @param address Remote device address
     * @return Remote device data. Null if no device found.
     */
    public DeviceDBData getDevice(String address) {
        return deviceAccessHelper.getDevice(address);
    }


    /**
     * Adds a device to the device table, if the device is new. Otherwise updates the existing device
     *
     * @param device
     */
    public void addDeviceIfNotExistsUpdateOtherwise(BluetoothDevice device) {
        deviceAccessHelper.addDeviceIfNotExistsUpdateOtherwise(device);

    }

    /**
     * Adds a device to the device table, if the device is new. Otherwise updates the existing device. LastSeen will be updated based on current system time. LastSeen and ID wont be read from the DeviceDBData class
     *
     * @param device
     */
    public void addDeviceIfNotExistsUpdateOtherwise(DeviceDBData device) {
        deviceAccessHelper.addDeviceIfNotExistsUpdateOtherwise(device);
    }

    /**
     * Updates the last seen property of a given device
     *
     * @param address Address of the remote device
     */
    public void updateDeviceLastSeen(String address) {
        deviceAccessHelper.updateDeviceLastSeen(address);
    }

    /**
     * Adds a new device based on its address
     *
     * @param address Remote device address
     */
    private void insertNewDevice(String address) {
        deviceAccessHelper.insertNewDevice(address);
    }

    /**
     * Checks if a certain device exists in the DB
     *
     * @param address Remote device address
     */
    private boolean checkDeviceExists(String address) {
        return deviceAccessHelper.checkDeviceExists(address);
    }

    /**
     * Updates various properties of a given device
     *
     * @param device Remote device
     */
    @Deprecated
    public void updateDevice(BluetoothDevice device) {
        addDeviceIfNotExistsUpdateOtherwise(device);
    }

    /**
     * Updates a devices friendly name
     *
     * @param address
     * @param friendlyName
     */
    public void updateDeviceFriendlyName(String address, String friendlyName) {
        deviceAccessHelper.updateDeviceFriendlyName(address, friendlyName);

    }

    /**
     * Get all devices, that are saved in this database
     */
    public List<DeviceDBData> getAllDevices() {
        return deviceAccessHelper.getAllDevices();


    }

    /**
     * Get all devices, that are saved in this database, and sort them based on their age
     */
    public List<DeviceDBData> getMostRecentDevices(int maxage) {
        return deviceAccessHelper.getMostRecentDevices(maxage);

    }

    public void addReceivedMessage(String senderAddress, String message, int userId,boolean encrypted) {
        Log.i(LOG_TAG, "Logging message " + message + " from " + senderAddress + " for " + userId);

        int deviceId = getDeviceID(senderAddress);
        if (deviceId > -1) {
            SQLiteDatabase connection =writeConnection;
            ContentValues cv = new ContentValues();
            cv.put("Text", message);
            cv.put("Timestamp", System.currentTimeMillis() / 1000);
            cv.put("Encrypted",encrypted);
            cv.put("UserID", userId);
            cv.put("DeviceID", deviceId);
            connection.insert("Received", null, cv);
            Log.i(LOG_TAG, "Logged message " + message + " from " + senderAddress + " for " + userId);
        } else {
            Log.i(LOG_TAG, "Logging message " + message + " from " + senderAddress + " for " + userId + " failed");
        }

    }

    public void addSendMessage(String receiverAddress, String message, int userId,boolean encrypted) {
        Log.i(LOG_TAG, "Logging message " + message + " for " + receiverAddress + " from " + userId);

        int deviceId = getDeviceID(receiverAddress);
        if (deviceId > -1) {
            SQLiteDatabase connection =writeConnection;
            ContentValues cv = new ContentValues();
            cv.put("Text", message);
            cv.put("Timestamp", System.currentTimeMillis() / 1000);
            cv.put("Encrypted",encrypted);
            cv.put("UserID", userId);
            cv.put("DeviceID", deviceId);
            connection.insert("Send", null, cv);
            Log.i(LOG_TAG, "Logged message " + message + " for " + receiverAddress + " from " + userId);
        } else {
            Log.i(LOG_TAG, "Logging message " + message + " for " + receiverAddress + " from " + userId + " failed");
        }

    }


    public void insertDeviceRSAPublicKey(String address, String data) {
        Log.i(LOG_TAG, "Writing RSA Public key from "+address);
        int deviceId = getDeviceID(address);
        if (deviceId > -1) {
            SQLiteDatabase connection = writeConnection;

            connection.delete("RSA","DeviceID = ?",new String[]{deviceId+""});
            ContentValues cv = new ContentValues();
            cv.put("DeviceID", deviceId);
            cv.put("Key",data);
            connection.insert("RSA",null,cv);
            Log.i(LOG_TAG,"New RSA Public key for device "+address);
        }
    }
    public String getDeviceRSAPublicKey(String address) {
        Log.i(LOG_TAG, "Reading RSA Public key from "+address+" from db");
        int deviceId = getDeviceID(address);
        if (deviceId > -1) {
            SQLiteDatabase connection = readConnection;

            Cursor c=connection.rawQuery("select Key from RSA where DeviceID = ?",new String[]{deviceId+""});
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
    public void insertDeviceAESKey(String address, String data) {
        Log.i(LOG_TAG, "Writing AES key from "+address);
        int deviceId = getDeviceID(address);
        if (deviceId > -1) {
            SQLiteDatabase connection = writeConnection;

            connection.delete("AES","DeviceID = ?",new String[]{deviceId+""});
            ContentValues cv = new ContentValues();
            cv.put("DeviceID", deviceId);
            cv.put("Key",data);
            connection.insert("AES",null,cv);
            Log.i(LOG_TAG,"New AES key for device "+address);
        }
    }
    public String getDeviceAESKey(String address) {
        Log.i(LOG_TAG, "Reading AES key from "+address+" from db");
        int deviceId = getDeviceID(address);
        if (deviceId > -1) {
            SQLiteDatabase connection = readConnection;

            Cursor c=connection.rawQuery("select Key from AES where DeviceID = ?",new String[]{deviceId+""});
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
    public void closeConnections(){
        writeConnection.close();
        readConnection.close();
    }
}
