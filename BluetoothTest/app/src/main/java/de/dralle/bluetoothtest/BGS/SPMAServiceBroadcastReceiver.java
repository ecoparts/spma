package de.dralle.bluetoothtest.BGS;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nils on 20.06.16.
 */
public class SPMAServiceBroadcastReceiver extends BroadcastReceiver {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = SPMAServiceBroadcastReceiver.class.getName();

    /**
     * Class to help with checking UUIDs
     */
    private UUIDChecker uuidChecker = null;

    /**
     * Class to help with sending internal messages
     */
    private InternalMessageSender internalMessageSender = null;
    /**
     * for parsing internal messages
     */
    private InternalMessageParser internalMessageParser = null;
    /**
     * Help with device handling
     */
    private RemoteBTDeviceManager deviceManager = null;

    public SPMAServiceBroadcastReceiver() {

    }

    public void setInternalMessageParser(InternalMessageParser internalMessageParser) {
        this.internalMessageParser = internalMessageParser;
    }

    public InternalMessageSender getInternalMessageSender() {
        return internalMessageSender;
    }

    public void setInternalMessageSender(InternalMessageSender internalMessageSender) {
        this.internalMessageSender = internalMessageSender;
    }


    /**
     *
     */

    public UUIDChecker getUuidChecker() {
        return uuidChecker;
    }

    public void setUuidChecker(UUIDChecker uuidChecker) {
        this.uuidChecker = uuidChecker;
    }

    public void setDeviceManager(RemoteBTDeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {

            Log.i(LOG_TAG, "New device found");

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            deviceManager.addNearbyDevice(device);


        }
        if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
            Log.i(LOG_TAG, "Discovery started");
            internalMessageSender.sendClearDevices();
        }
        if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            Log.i(LOG_TAG, "Discovery finished. " + deviceManager.getNearbyDevices().size() + " devices found");
            BluetoothDevice nextDeviceToScan = deviceManager.getNearbyDevice();
            if (nextDeviceToScan != null) {
                nextDeviceToScan.fetchUuidsWithSdp();
            }
        }
        if (BluetoothDevice.ACTION_UUID.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.i(LOG_TAG, "Sdp scan for device " + device.getAddress());
            ParcelUuid[] allUUIDs = device.getUuids();

            if (allUUIDs == null) {
                Log.i(LOG_TAG, "Device " + device.getAddress() + " supports null UUIDs");
            } else {
                Log.i(LOG_TAG, "Device " + device.getAddress() + " supports " + allUUIDs.length + " UUIDs");
                for (ParcelUuid uuid : allUUIDs) {
                    Log.v(LOG_TAG, "Device " + device.getAddress() + " supports UUID " + uuid.getUuid().toString());
                }
            }
            if (uuidChecker.checkForSupportedUUIDs(allUUIDs)) {
                Log.i(LOG_TAG, "Device " + device.getAddress() + " supported");
                deviceManager.addSupportedDevice(device);
                deviceManager.addDeviceToCache(device);
            } else {
                Log.i(LOG_TAG, "Device " + device.getAddress() + " not supported");
            }


            BluetoothDevice nextDeviceToScan = deviceManager.getNearbyDevice();
            if (nextDeviceToScan != null) {
                nextDeviceToScan.fetchUuidsWithSdp();
            } else {
                Log.i(LOG_TAG, "Fetching UUIDs for all devices finished. Found " + deviceManager.getSupportedDevices().size() + " connect-worthy devices");
                internalMessageSender.sendNewSupportedDeviceList();
            }
        }

        if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.i(LOG_TAG, "New device bonded");
                Log.i(LOG_TAG, device.getAddress());
                Log.i(LOG_TAG, device.getName());
            } else {
                Log.i(LOG_TAG, "Bonding failed");
            }
        }
        if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);


            Log.i(LOG_TAG, "Device requesting pairing");
            Log.i(LOG_TAG, device.getAddress());
            Log.i(LOG_TAG, device.getName());


        }
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
            if (defaultAdapter != null) {
                if (defaultAdapter.isEnabled()) {
                    Log.i(LOG_TAG, "Bluetooth is now enabled");
                } else {
                    Log.i(LOG_TAG, "Bluetooth is now disabled");
                }
            } else {
                Log.w(LOG_TAG, "BluetoothAdapter is null after state change. Its bad");
            }


        }
        if (SPMAService.ACTION_NEW_MSG.equals(action)) {
            String msg = intent.getStringExtra("msg");
            Log.i(LOG_TAG, "New message");
            Log.i(LOG_TAG, msg);
            JSONObject msgData = null;
            try {
                msgData = new JSONObject(msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (msgData != null) {
                if (internalMessageParser.isInternalMessageValid(msgData)) {
                    internalMessageParser.parseInternalMessageForAction(msgData);
                }
            } else {
                Log.w(LOG_TAG, "Message not JSON");
            }


        }
    }
}
