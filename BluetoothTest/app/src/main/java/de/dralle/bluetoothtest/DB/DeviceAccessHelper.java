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

    /**
     * Looks for a device´s friendly name. This is not the device´s bluetooth name, but the user name used by the app on the remote device
     *
     * @param address Remote device address
     * @return Remote device friendly name/remote device user name. returns address when no name was found.
     */
    public String getDeviceFriendlyName(String address) {
        DeviceDBData device = getDevice(address);
        if (device != null) {
            return device.getFriendlyName();
        } else {
            return address;
        }

    }

    /**
     * Looks for a device´s id from an address
     *
     * @param address Remote device address
     * @return Remote device database id. -1 if no device is found
     */
    public int getDeviceID(String address) {
        DeviceDBData device = getDevice(address);
        if (device != null) {
            return device.getId();
        } else {
            return -1;
        }
    }

    /**
     * Looks for a device´s id from an address
     *
     * @param address Remote device address
     * @return Remote device data. Null if no device found.
     */
    public DeviceDBData getDevice(String address) {
        DeviceDBData device = null;
        Cursor c = connection.rawQuery("select * from Devices where Address = ?", new String[]{address});
        if (c.moveToNext()) {
            device = new DeviceDBData();
            device.setId(c.getInt(0));
            device.setDeviceName(c.getString(2));
            device.setPaired(c.getInt(4) == 1);
            device.setFriendlyName(c.getString(3));
            device.setAddress(c.getString(1));
            device.setLastSeen(c.getInt(5));
        } else {
            Log.w(LOG_TAG, "No device with address " + address);
        }
        c.close();
        return device;
    }

    /**
     * Adds a device to the device table, if the device is new. Otherwise updates the existing device
     *
     * @param device
     */
    public void addDeviceIfNotExistsUpdateOtherwise(BluetoothDevice device) {
        addDeviceIfNotExistsUpdateOtherwise(new DeviceDBData(device));
    }

    /**
     * Adds a device to the device table, if the device is new. Otherwise updates the existing device. LatSeen will be updated based on current system time. LastSeen and ID wont be read from the DeviceDBData class
     *
     * @param device
     */
    public void addDeviceIfNotExistsUpdateOtherwise(DeviceDBData device) {
        Cursor c = connection.rawQuery("select count(*) from Devices where Address = ?", new String[]{device.getAddress()});
        if (c.moveToNext()) {
            int cnt = c.getInt(0);
            Log.i(LOG_TAG, cnt + " entries for device " + device.getAddress());
            if (cnt == 1) {//One device
                ContentValues values = new ContentValues();
                values.put("DeviceName", device.getDeviceName());
                values.put("FriendlyName", device.getFriendlyName());
                values.put("Paired", device.isPaired());
                values.put("LastSeen", System.currentTimeMillis() / 1000);
                connection.update("Devices", values, "Address = ?", new String[]{device.getAddress()});
                Log.i(LOG_TAG, "DB updated");
            } else if (cnt == 0) { //No device
                ContentValues values = new ContentValues();
                values.put("DeviceName", device.getDeviceName());
                values.put("FriendlyName", device.getFriendlyName());
                values.put("Paired", device.isPaired());
                values.put("Address", device.getAddress());
                values.put("LastSeen", System.currentTimeMillis() / 1000);
                connection.insert("Devices", null, values);
                Log.i(LOG_TAG, "New device in DB");
            } else {//Multiple devices. Shouldnt happen.
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
    public void insertNewDevice(String address) {
        addDeviceIfNotExistsUpdateOtherwise(new DeviceDBData(address, address, address, 0, 0, false));
    }

    /**
     * Checks if a certain device exists in the DB
     *
     * @param address Remote device address
     */
    public boolean checkDeviceExists(String address) {
        DeviceDBData device = getDevice(address);
        return device != null;
    }


    /**
     * Updates a devices friendly name
     *
     * @param address
     * @param friendlyName
     */
    public void updateDeviceFriendlyName(String address, String friendlyName) {
        ContentValues values = new ContentValues();
        values.put("FriendlyName", friendlyName);
        connection.update("Devices", values, "Address = ?", new String[]{address});
        Log.i(LOG_TAG, "Device " + address + " updated with name " + friendlyName);
    }

    /**
     * Get all devices, that are saved in this database
     */
    public List<DeviceDBData> getAllDevices() {
        Cursor c = null;
        try {
            c = connection.rawQuery("select * from Devices order by LastSeen", new String[]{});
        } catch (Exception e) {

        }
        List<DeviceDBData> devices = new ArrayList<>();
        if (c != null) {


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
        }
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
}
