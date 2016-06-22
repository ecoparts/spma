package de.dralle.bluetoothtest.BGS;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.res.Resources;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

import de.dralle.bluetoothtest.R;

/**
 * Created by nils on 02.06.16.
 */
public class BluetoothConnectionMaker {
    private static final String LOG_TAG = BluetoothConnectionMaker.class.getName();
    private static BluetoothConnectionMaker instance = null;
    private Resources res = null;

    private BluetoothConnectionMaker(Resources res) {
        this.res = res;
    }

    private BluetoothConnectionMaker() {
        this.res = res;
    }

    /**
     * Singleton constructor
     *
     * @return Singleton instance
     */
    public static BluetoothConnectionMaker getInstance() {
        if (instance == null) {
            instance = new BluetoothConnectionMaker();
        }
        return instance;
    }

    /**
     * Singleton constructor
     *
     * @param res Local resource manager
     * @return Singleton instance
     */
    public static BluetoothConnectionMaker getInstance(Resources res) {
        if (instance == null) {
            instance = new BluetoothConnectionMaker(res);
        }
        return instance;
    }

    /**
     * Assists the user in creating a connection
     *
     * @param device
     * @return
     */
    public BluetoothConnection createConnection(BluetoothDevice device) {
        BluetoothSocket socket = null;
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {

            try {
                Log.i(LOG_TAG, "Trying to create secure connection");
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString(res.getString(R.string.uuid_secure)));
                Log.i(LOG_TAG, "Created socket");
            } catch (IOException e) {
                Log.w(LOG_TAG, "Socket creation failed");
                e.printStackTrace();
            }

        } else {
            try {
                Log.i(LOG_TAG, "Trying to create insecure connection");
                socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(res.getString(R.string.uuid_insecure)));
                Log.i(LOG_TAG, "Created socket");
            } catch (IOException e) {
                Log.w(LOG_TAG, "Socket creation failed");
                e.printStackTrace();
            }
        }
        if (socket != null) {
            return new BluetoothConnection(socket, device.getBondState() == BluetoothDevice.BOND_BONDED);
        }
        return null;
    }
}
