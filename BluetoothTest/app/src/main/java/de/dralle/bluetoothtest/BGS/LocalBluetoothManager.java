package de.dralle.bluetoothtest.BGS;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nils on 20.06.16.
 */
public class LocalBluetoothManager {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = LocalBluetoothManager.class.getName();
    /**
     * Context to be ujsed
     */
    private Context context;
    /**
     * Help with device handling
     */
    private RemoteBTDeviceManager deviceManager = null;

    public LocalBluetoothManager() {

    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setDeviceManager(RemoteBTDeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    /**
     * Checks if the device is discoverable, and if no requests it
     *
     * @param msgData may contain additional data
     */
    public void makeDeviceVisible(JSONObject msgData) {
        //if a duration is given, use that
        int duration = 0; //0 for always visible
        try {
            duration = msgData.getInt("Duration");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            Log.i(LOG_TAG, "Making device discoverable");
            if (btAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent discoverableIntent = new
                        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
                discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(discoverableIntent);
            } else {
                Log.v(LOG_TAG, "Device already visible");
            }

        }
    }

    /**
     * Starts scanning for bluetooth devices
     *
     * @param msgData may contain additional data
     * @return scan was successfully initialized
     */
    public boolean scanForNearbyDevices(JSONObject msgData) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION.toString()) == PackageManager.PERMISSION_GRANTED) {
                Log.i(LOG_TAG, "Permission ACCESS_COARSE_LOCATION granted");
            } else {
                Log.w(LOG_TAG, "Permission ACCESS_COARSE_LOCATION denied");
                return false;
            }
        }
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            if (btAdapter.isEnabled()) {
                deviceManager.clearNearbyDevices();
                deviceManager.clearSupportedDevices();//clear current device list
                if (btAdapter.isDiscovering()) {
                    Log.w(LOG_TAG, "Discovery running. Cancelling discovery");
                    btAdapter.cancelDiscovery();
                }
                Log.i(LOG_TAG, "Starting discovery");
                if (btAdapter.startDiscovery()) {
                    Log.i(LOG_TAG, "Started discovery");
                    return true;
                } else {
                    Log.i(LOG_TAG, "Failed to start discovery");
                }
            } else {
                Log.i(LOG_TAG, "Bluetooth disabled. Cant scan");
            }
        }
        Log.w(LOG_TAG, "No bluetooth. Cant scan");
        return false;
    }


    /**
     * Checks if bluetooth is on, and if no requests permission to turn it on
     */
    public void turnBluetoothOn() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            Log.i(LOG_TAG, "Turning bluetooth on");
            if (!btAdapter.isEnabled()) {
                Intent btOnRequest = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                btOnRequest.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //Flag needed when starting from a service
                context.startActivity(btOnRequest);
            } else {
                Log.v(LOG_TAG, "Bluetooth already on");
            }

        }
    }

    /**
     * Turns Bluetooth off
     */
    public void turnBluetoothOff() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            Log.i(LOG_TAG, "Turning bluetooth off");
            btAdapter.disable();

        }
    }

    /**
     * @return The local bluetooth device name
     */
    public String getLocalDeviceName() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            return adapter.getName();
        }
        return null;
    }
    /**
     * @return The local bluetooth device address. Not working in API 23+
     */
    public String getLocalDeviceAddress() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            return adapter.getAddress();
        }
        return null;
    }


}
