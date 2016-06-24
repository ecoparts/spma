package de.dralle.bluetoothtest.DB;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

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
    /**
     * Is the database locked? Used to ensure only one db access at a time
     */
    private boolean dbLocked = false;

    private SQLiteDatabase writeConnection = null;
    /**
     * Used to read/write user tables
     */
    private UserAccessHelper userAccessHelper;
    /**
     * Used to read/write device tables
     */
    private DeviceAccessHelper deviceAccessHelper;
    /**
     * Used to read/write msg history tables
     */
    private MessageHistoryAccessHelper messageHistoryAccessHelper;
    /**
     * Used to read/write crypto key tables
     */
    private CryptoKeysAccessHelper cryptoKeysAccessHelper;

    private SPMADatabaseAccessHelper(Context context) {
        this.context = context;
        db = new SPMADatabaseHelper(context);
        writeConnection = db.getWritableDatabase();
        Log.i(LOG_TAG, "Database connection open");
        userAccessHelper = new UserAccessHelper(writeConnection);
        deviceAccessHelper = new DeviceAccessHelper(writeConnection);
        messageHistoryAccessHelper = new MessageHistoryAccessHelper(writeConnection);
        cryptoKeysAccessHelper = new CryptoKeysAccessHelper(writeConnection);
        unlockDB();
    }

    public static SPMADatabaseAccessHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SPMADatabaseAccessHelper(context);
        }
        return instance;
    }

    private void lockDB() {
        while (dbLocked) ;
        dbLocked = true;
        Log.v(LOG_TAG, "Database locked");
    }

    private void unlockDB() {
        dbLocked = false;
        Log.v(LOG_TAG, "Database unlocked");
    }

    @Deprecated
    public User addUser(String name) {
        lockDB();
        User u = userAccessHelper.addUser(name);
        unlockDB();
        return u;
    }

    /**
     * Updates a database User
     *
     * @param u Userdata to be updated
     * @return User given
     */
    public User createOrUpdateUser(User u) {
        lockDB();
        User user = userAccessHelper.createOrUpdateUser(u);
        unlockDB();
        return user;
    }

    /**
     * Gets a user from the db
     *
     * @param id User id
     * @return User data. Can be null.
     */
    public User getUser(int id) {
        lockDB();
        User u = userAccessHelper.getUser(id);
        unlockDB();
        return u;

    }

    /**
     * Looks for a device´s friendly name. This is not the device´s bluetooth name, but the user name used by the app on the remote device
     *
     * @param address Remote device address
     * @return Remote device friendly name/remote device user name. returns address when no name was found.
     */
    public String getDeviceFriendlyName(String address) {
        lockDB();
        String name = deviceAccessHelper.getDeviceFriendlyName(address);
        unlockDB();
        return name;
    }

    /**
     * Looks for a device´s id from an address
     *
     * @param address Remote device address
     * @return Remote device database id. -1 if no device is found
     */
    public int getDeviceID(String address) {
        lockDB();
        int id = deviceAccessHelper.getDeviceID(address);
        unlockDB();
        return id;

    }

    /**
     * Looks for a device´s id from an address
     *
     * @param address Remote device address
     * @return Remote device data. Null if no device found.
     */
    public DeviceDBData getDevice(String address) {
        lockDB();
        DeviceDBData d = deviceAccessHelper.getDevice(address);
        unlockDB();
        return d;
    }


    /**
     * Adds a device to the device table, if the device is new. Otherwise updates the existing device
     *
     * @param device
     */
    public void addDeviceIfNotExistsUpdateOtherwise(BluetoothDevice device) {
        lockDB();
        deviceAccessHelper.addDeviceIfNotExistsUpdateOtherwise(device);
        unlockDB();


    }

    /**
     * Adds a device to the device table, if the device is new. Otherwise updates the existing device. LastSeen will be updated based on current system time. LastSeen and ID wont be read from the DeviceDBData class
     *
     * @param device
     */
    public void addDeviceIfNotExistsUpdateOtherwise(DeviceDBData device) {
        lockDB();
        deviceAccessHelper.addDeviceIfNotExistsUpdateOtherwise(device);
        unlockDB();
    }

    /**
     * Updates the last seen property of a given device
     *
     * @param address Address of the remote device
     */
    public void updateDeviceLastSeen(String address) {
        lockDB();
        deviceAccessHelper.updateDeviceLastSeen(address);
        unlockDB();
    }

    /**
     * Adds a new device based on its address
     *
     * @param address Remote device address
     */
    private void insertNewDevice(String address) {
        lockDB();
        deviceAccessHelper.insertNewDevice(address);
        unlockDB();
    }

    /**
     * Checks if a certain device exists in the DB
     *
     * @param address Remote device address
     */
    private boolean checkDeviceExists(String address) {
        lockDB();
        boolean exists = deviceAccessHelper.checkDeviceExists(address);
        unlockDB();
        return exists;
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
        lockDB();
        deviceAccessHelper.updateDeviceFriendlyName(address, friendlyName);
        unlockDB();

    }

    /**
     * Get all devices, that are saved in this database
     */
    public List<DeviceDBData> getAllDevices() {
        lockDB();
        List<DeviceDBData> allDevices = deviceAccessHelper.getAllDevices();
        unlockDB();
        return allDevices;

    }

    /**
     * Get all devices, that are saved in this database, and sort them based on their age
     */
    public List<DeviceDBData> getMostRecentDevices(int maxage) {
        lockDB();
        List<DeviceDBData> devices = deviceAccessHelper.getMostRecentDevices(maxage);
        unlockDB();
        return devices;
    }

    public void addReceivedMessage(String senderAddress, String message, int userId) {
        Log.i(LOG_TAG, "Logging message " + message + " from " + senderAddress + " for " + userId);
        int deviceId = getDeviceID(senderAddress);
        if (deviceId > -1) {
            lockDB();
            messageHistoryAccessHelper.addReceivedMessage(deviceId, message, userId);
            unlockDB();
        } else {
            Log.i(LOG_TAG, "Logging message " + message + " from " + senderAddress + " for " + userId + " failed");
        }

    }

    public void addSendMessage(String receiverAddress, String message, int userId) {
        Log.i(LOG_TAG, "Logging message " + message + " for " + receiverAddress + " from " + userId);
        int deviceId = getDeviceID(receiverAddress);
        if (deviceId > -1) {
            lockDB();
            messageHistoryAccessHelper.addSendMessage(deviceId, message, userId);
            unlockDB();
        } else {
            Log.i(LOG_TAG, "Logging message " + message + " for " + receiverAddress + " from " + userId + " failed");
        }

    }


    public void insertDeviceRSAPublicKey(String address, String data) {
        Log.i(LOG_TAG, "Writing RSA Public key of " + address);
        int deviceId = getDeviceID(address);
        if (deviceId > -1) {
            lockDB();
            cryptoKeysAccessHelper.insertDeviceRSAPublicKey(deviceId, data);
            unlockDB();
            Log.i(LOG_TAG, "New RSA Public key for device " + address);
        }
    }

    public String getDeviceRSAPublicKey(String address) {
        Log.i(LOG_TAG, "Reading RSA Public key from " + address + " from db");
        int deviceId = getDeviceID(address);
        if (deviceId > -1) {
            lockDB();
            String key = cryptoKeysAccessHelper.getDeviceRSAPublicKey(deviceId);
            unlockDB();
            Log.i(LOG_TAG, "RSA Public key read " + key);
            return key;
        }
        return null;
    }

    public void insertDeviceAESKey(String address, String data) {
        Log.i(LOG_TAG, "Writing AES key from " + address);
        int deviceId = getDeviceID(address);
        if (deviceId > -1) {
            lockDB();
            cryptoKeysAccessHelper.insertDeviceAESKey(deviceId, data);
            unlockDB();
            Log.i(LOG_TAG, "New AES key for device " + address);
        }
    }

    public String getDeviceAESKey(String address) {
        Log.i(LOG_TAG, "Reading AES key from " + address + " from db");
        int deviceId = getDeviceID(address);
        if (deviceId > -1) {
            lockDB();
            String key = cryptoKeysAccessHelper.getDeviceAESKey(deviceId);
            unlockDB();
            Log.i(LOG_TAG, "AES key read " + key);
            return key;
        }
        return null;
    }

    public void closeConnections() {
        lockDB();
        writeConnection.close();
        Log.i(LOG_TAG, "Database connection closed");
    }
}
