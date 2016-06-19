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

    private SPMADatabaseAccessHelper(Context context) {
        this.context = context;
        db = new SPMADatabaseHelper(context);
        writeConnection=db.getWritableDatabase();
        readConnection=db.getReadableDatabase();
        userAccessHelper=new UserAccessHelper(writeConnection);
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
        SQLiteDatabase connection = writeConnection;
        ContentValues cv = new ContentValues();

        cv.put("Name", u.getName());
        cv.put("AES", u.getAes());
        cv.put("RSAPrivate", u.getRsaPrivate());
        cv.put("RSAPublic", u.getRsaPublic());

        //Check if user is already there
        Cursor c = connection.rawQuery("select count(*) from User where ID = ?", new String[]{u.getId() + ""});
        if (c.moveToNext()) {
            int cnt = c.getInt(0);
            if (cnt == 0) {
                //Insert
                cv.put("ID", u.getId());
                connection.insert("User", null, cv);
                Log.i(LOG_TAG, "User " + u.getName() + " with id " + u.getId() + " updated");
            } else {
                //Update. No need to check for count, because primary key
                connection.update("User", cv, "ID = ?", new String[]{u.getId() + ""});
                Log.i(LOG_TAG, "User " + u.getName() + " with id " + u.getId() + " updated");
            }
        }


        c.close();



        return u;
    }

    public User getUser(int id) {
        SQLiteDatabase connection = readConnection;
        Cursor c = connection.rawQuery("select * from User where User.ID=?", new String[]{id + ""});
        User u = null;
        try {


            if (c.moveToNext()) {
                u = new User();
                u.setId(id);
                u.setName(c.getString(1));
                u.setAes(c.getString(2));
                u.setRsaPrivate(c.getString(3));
                u.setRsaPublic(c.getString(4));


            }
            c.close();
        } catch (Exception e) {

        }

        return u;

    }

    /**
     * Looks for a device´s friendly name. This is not the device´s bluetooth name, but the user name used by the app on the remote device
     *
     * @param address Remote device address
     * @return Remote device friendly name/remote device user name. returns address when no name was found.
     */
    public String getDeviceFriendlyName(String address) {
        try {
            SQLiteDatabase connection = writeConnection;
            Cursor c = connection.rawQuery("select FriendlyName from Devices where Address = ?", new String[]{address});

            if (c.moveToNext()) {
                String name = c.getString(0);
                c.close();
                return name;
            } else {
                c.close();
                return address;
            }
        }
        catch(Exception e){
            e.printStackTrace();
            Log.i(LOG_TAG,"Opening DB for requesting device friendly name failed");
        }
        return address;

    }

    /**
     * Looks for a device´s id from an address
     *
     * @param address Remote device address
     * @return Remote device database id. -1 if no device is found
     */
    public int getDeviceID(String address) { //TODO: unify with above
        SQLiteDatabase connection = readConnection;
        Cursor c = connection.rawQuery("select ID from Devices where Address = ?", new String[]{address});

        if (c.moveToNext()) {
            int id = c.getInt(0);
            c.close();
            return id;
        } else {
            c.close();
            Log.w(LOG_TAG, "No device with address " + address);
            return -1;
        }

    }


    /**
     * Adds a device to the device table, if the device is new. Otherwise updates the existing device
     *
     * @param device
     */
    public void addDeviceIfNotExistsUpdateOtherwise(BluetoothDevice device) {
        SQLiteDatabase connection =writeConnection;
        Cursor c = connection.rawQuery("select count(*) from Devices where Address = ?", new String[]{device.getAddress()});


        if (c.moveToNext()) {
            int cnt = c.getInt(0);
            Log.i(LOG_TAG, cnt + " entries for device " + device.getAddress());
            if (cnt == 1) {
                ContentValues values = new ContentValues();
                values.put("DeviceName", device.getName());
                values.put("Paired", device.getBondState() == BluetoothDevice.BOND_BONDED);

                values.put("LastSeen", System.currentTimeMillis() / 1000);
                connection.update("Devices", values, "Address = ?", new String[]{device.getAddress()});
                Log.i(LOG_TAG, "DB updated");
            } else if (cnt == 0) {
                ContentValues values = new ContentValues();
                values.put("DeviceName", device.getName());
                values.put("FriendlyName", device.getName());
                values.put("Paired", device.getBondState() == BluetoothDevice.BOND_BONDED);
                values.put("Address", device.getAddress());
                values.put("LastSeen", System.currentTimeMillis() / 1000);
                connection.insert("Devices", null, values);
                Log.i(LOG_TAG, "New device in DB");
            } else {
                Log.w(LOG_TAG, "Insert or update of device failed. Seriously, something failed in a very bad way");
                Log.v(LOG_TAG, "Panic mode");
                //"self healing": check
                connection.delete("Devices", "Address = ?", new String[]{device.getAddress()});
            }
        } else {
            Log.w(LOG_TAG, "Insert or update of device failed");
        }
        c.close();

    }

    /**
     * Adds a device to the device table, if the device is new. Otherwise updates the existing device. LatSeen will be updated based on current system time. LastSeen and ID wont be read from the DeviceDBData class
     *
     * @param device
     */
    public void addDeviceIfNotExistsUpdateOtherwise(DeviceDBData device) {
        SQLiteDatabase connection = writeConnection;
        Cursor c = connection.rawQuery("select count(*) from Devices where Address = ?", new String[]{device.getAddress()});


        if (c.moveToNext()) {
            int cnt = c.getInt(0);
            Log.i(LOG_TAG, cnt + " entries for device " + device.getAddress());
            if (cnt == 1) {
                ContentValues values = new ContentValues();
                values.put("DeviceName", device.getDeviceName());
                values.put("FriendlyName", device.getFriendlyName());
                values.put("Paired", device.isPaired());

                values.put("LastSeen", System.currentTimeMillis() / 1000);
                connection.update("Devices", values, "Address = ?", new String[]{device.getAddress()});
                Log.i(LOG_TAG, "DB updated");
            } else if (cnt == 0) {
                ContentValues values = new ContentValues();
                values.put("DeviceName", device.getDeviceName());
                values.put("FriendlyName", device.getFriendlyName());
                values.put("Paired", device.isPaired());
                values.put("Address", device.getAddress());
                values.put("LastSeen", System.currentTimeMillis() / 1000);
                connection.insert("Devices", null, values);
                Log.i(LOG_TAG, "New device in DB");
            } else {
                Log.w(LOG_TAG, "Insert or update of device failed. Seriously, something failed in a very bad way");
                Log.v(LOG_TAG, "Panic mode");
                //"self healing": check
                connection.delete("Devices", "Address = ?", new String[]{device.getAddress()});
            }
        } else {
            Log.w(LOG_TAG, "Insert or update of device failed");
        }
        c.close();
    }

    /**
     * Updates the last seen property of a given device
     *
     * @param address Address of the remote device
     */
    public void updateDeviceLastSeen(String address) {
        if (!checkDeviceExists(address)) {
            insertNewDevice(address);
        }
        SQLiteDatabase connection = writeConnection;
        ContentValues values = new ContentValues();


        values.put("LastSeen", System.currentTimeMillis() / 1000);
        connection.update("Devices", values, "Address = ?", new String[]{address});
        Log.i(LOG_TAG, address + " updated lastSeen");


    }

    /**
     * Adds a new device based on its address
     *
     * @param address Remote device address
     */
    private void insertNewDevice(String address) {
        addDeviceIfNotExistsUpdateOtherwise(new DeviceDBData(address, address, address, 0, 0, false));
    }

    /**
     * Checks if a certain device exists in the DB
     *
     * @param address Remote device address
     */
    private boolean checkDeviceExists(String address) {
        SQLiteDatabase connection = readConnection;
        Cursor c = connection.rawQuery("select count(*) from Devices where Address = ?", new String[]{address});
        int cnt = 0;

        if (c.moveToNext()) {
            cnt = c.getInt(0);
            Log.i(LOG_TAG, cnt + " entries for device " + address);
        }
        c.close();
        return cnt > 0;
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
        SQLiteDatabase connection = writeConnection;
        ContentValues values = new ContentValues();

        values.put("FriendlyName", friendlyName);


        connection.update("Devices", values, "Address = ?", new String[]{address});
        Log.i(LOG_TAG, "Device " + address + " updated with name " + friendlyName);

    }

    /**
     * Get all devices, that are saved in this database
     */
    public List<DeviceDBData> getAllDevices() {
        SQLiteDatabase connection = readConnection;
        Cursor c = connection.rawQuery("select * from Devices order by LastSeen", new String[]{});
        List<DeviceDBData> devices = new ArrayList<>();
        while (c.moveToNext()) {
            DeviceDBData device = new DeviceDBData();
            device.setId(c.getInt(0));
            device.setAddress(c.getString(1));
            device.setDeviceName(c.getString(2));
            device.setFriendlyName(c.getString(3));
            device.setPaired(c.getInt(4) == 1);
            device.setLastSeen(c.getInt(5));
            devices.add(device);

        }
        c.close();

        return devices;


    }

    /**
     * Get all devices, that are saved in this database, and sort them based on their age
     */
    public List<DeviceDBData> getMostRecentDevices(int maxage) {
        List<DeviceDBData> devices = getAllDevices();
        List<DeviceDBData> filteredDevices = new ArrayList<>();
        for (DeviceDBData dd : devices) {
            if (maxage >= System.currentTimeMillis() / 1000 - dd.getLastSeen()) {
                filteredDevices.add(dd);
            }
        }
        return filteredDevices;

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
