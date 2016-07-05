package de.dralle.bluetoothtest.BGS;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import de.dralle.bluetoothtest.DB.DeviceDBData;
import de.dralle.bluetoothtest.DB.SPMADatabaseAccessHelper;

/**
 * Created by nils on 20.06.16.
 */
public class RemoteBTDeviceManager {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = RemoteBTDeviceManager.class.getName();
    /**
     * Context to be ujsed
     */
    private Context context;

    /**
     *
     */
    private int nextReturnedBTDevice;
    /**
     * List of nearby devices
     */
    private List<BluetoothDevice> nearbyDevices = null;
    /**
     * List of nearby supported devices
     */
    private List<BluetoothDevice> supportedDevices = null;

    public RemoteBTDeviceManager() {
        nearbyDevices = new ArrayList<>();
        supportedDevices = new ArrayList<>();
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public List<BluetoothDevice> getNearbyDevices() {
        return nearbyDevices;
    }

    public List<BluetoothDevice> getSupportedDevices() {
        return supportedDevices;
    }

    public BluetoothDevice getNearbyDevice() {
        BluetoothDevice deviceToBeReturned = null;
        if (nearbyDevices.size() > nextReturnedBTDevice) {
            deviceToBeReturned = nearbyDevices.get(nextReturnedBTDevice);
        }
        nextReturnedBTDevice++;
        return deviceToBeReturned;
    }

    public BluetoothDevice getSupportedDeviceByAddress(String address) {
        BluetoothDevice deviceToBeReturned = null;
        for (BluetoothDevice d : supportedDevices) {
            if (d.getAddress().equals(address)) {
                deviceToBeReturned = d;
            }
        }
        return deviceToBeReturned;
    }

    public boolean addNearbyDevice(BluetoothDevice device) {
        for (BluetoothDevice dev : nearbyDevices) {
            if (dev.getAddress().equals(device.getAddress())) {
                return false;
            }
        }
        nearbyDevices.add(device);
        return true;
    }

    public boolean addSupportedDevice(BluetoothDevice device) {
        for (BluetoothDevice dev : supportedDevices) {
            if (dev.getAddress().equals(device.getAddress())) {
                return false;
            }
        }
        supportedDevices.add(device);
        return true;
    }

    public void addDeviceToCache(BluetoothDevice device) {
        SPMADatabaseAccessHelper db = SPMADatabaseAccessHelper.getInstance(context);
        db.addDeviceIfNotExistsUpdateOtherwise(device);
    }

    public List<DeviceDBData> getAllCachedDevices() {
        SPMADatabaseAccessHelper db = SPMADatabaseAccessHelper.getInstance(context);
        return db.getAllDevices();
    }

    public DeviceDBData getCachedDevice(String address) {
        List<DeviceDBData> allDevices = getAllCachedDevices();
        for (DeviceDBData d : allDevices) {
            if (d.getAddress().equals(address)) {
                return d;
            }
        }
        return null;
    }

    public void clearNearbyDevices() {
        nextReturnedBTDevice = 0;
        nearbyDevices.clear();
    }
    public void clearCachedDevices() {
        SPMADatabaseAccessHelper db = SPMADatabaseAccessHelper.getInstance(context);
        db.cl
    }

    public void clearSupportedDevices() {
        supportedDevices.clear();
    }

}
