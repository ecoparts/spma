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

    private Context context;

    private SQLiteOpenHelper db = null;

    public SPMADatabaseAccessHelper(Context context) {
        this.context = context;
        db = new SPMADatabaseHelper(context);
    }

    public User addUser(String name) {
        SQLiteDatabase connection = db.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("Name", name);
        long rowid = connection.insert("User", null, cv);
        Cursor c = connection.rawQuery("select ID, Name from User where User._rowid_=?", new String[]{rowid + ""});
        c.moveToNext();
        String sid = c.getString(0);
        name = c.getString(1);
        int id = Integer.parseInt(sid);
        c.close();
        connection.close();
        Log.i(LOG_TAG, "New User " + name + " with id " + id + " inserted");
        User u = new User();
        u.setId(id);
        u.setName(name);
        return u;
    }

    public User getUser(int id) {
        SQLiteDatabase connection = db.getReadableDatabase();
        Cursor c = connection.rawQuery("select Name from User where User.ID=?", new String[]{id + ""});
        if (c.moveToNext()) {
            String name = c.getString(0);
            c.close();
            connection.close();
            User u = new User();
            u.setId(id);
            u.setName(name);
            return u;
        } else {
            c.close();
            connection.close();
            return null;
        }
    }

    /**
     * Looks for a device´s friendly name. This is not the device´s bluetooth name, but the user name used by the app on the remote device
     *
     * @param address Remote device address
     * @return Remote device friendly name/remote device user name. returns address when no name was found.
     */
    public String getDeviceFriendlyName(String address) {
        SQLiteDatabase connection = db.getReadableDatabase();
        Cursor c = connection.rawQuery("select FriendlyName from Devices where Address = ?", new String[]{"'"+address+"'"});

        if (c.moveToNext()) {
            String name = c.getString(0);
            c.close();
            connection.close();
            return name;
        } else {
            c.close();
            connection.close();
            return address;
        }

    }

    /**
     * Adds a device to the device table, if the device is new. Otherwise updates the existing device
     *
     * @param device
     */
    public void addDeviceIfNotExistsUpdateOtherwise(BluetoothDevice device) {
        SQLiteDatabase connection = db.getWritableDatabase();
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
                connection.delete("Devices","Address = ?",new String[]{device.getAddress()});
            }
        } else {
            Log.w(LOG_TAG, "Insert or update of device failed");
        }
        c.close();
        connection.close();
    }

    /**
     * Updates the last seen property of a given device
     * @param address Address of the remote device
     */
    public void updateDeviceLastSeen(String address){
        SQLiteDatabase connection = db.getWritableDatabase();
        ContentValues values = new ContentValues();


        values.put("LastSeen", System.currentTimeMillis() / 1000);
        connection.update("Devices", values, "Address = ?", new String[]{address});
        Log.i(LOG_TAG,address + " updated lastSeen");
        connection.close();
    }
    /**
     * Updates various properties of a given device
     * @param device Remote device
     */
    public void updateDevice(BluetoothDevice device){
        SQLiteDatabase connection = db.getWritableDatabase();
        ContentValues values = new ContentValues();


        values.put("DeviceName", device.getName());
        values.put("Paired", device.getBondState() == BluetoothDevice.BOND_BONDED);
        values.put("LastSeen", System.currentTimeMillis() / 1000);
        connection.update("Devices", values, "Address = ?", new String[]{device.getAddress()});
        Log.i(LOG_TAG,device.getAddress() + " updated");
        connection.close();
    }
    /**
     * Get all devices, that are saved in this database
     */
    public List<DeviceDBData> getAllDevices() { //TODO; add update method for LastSeen
        SQLiteDatabase connection = db.getReadableDatabase();
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
        connection.close();
        return devices;


    }
    /**
     * Get all devices, that are saved in this database, and sort them based on their age
     */
    public List<DeviceDBData> getMostRecentDevices(int maxage) {
        List<DeviceDBData> devices = getAllDevices();
        List<DeviceDBData> filteredDevices=new ArrayList<>();
        for(DeviceDBData dd:devices){
            if(maxage>=System.currentTimeMillis()/1000-dd.getLastSeen()){
                filteredDevices.add(dd);
            }
        }
        return filteredDevices;

    }

}
