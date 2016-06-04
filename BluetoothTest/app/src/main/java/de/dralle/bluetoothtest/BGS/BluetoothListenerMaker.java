package de.dralle.bluetoothtest.BGS;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.res.Resources;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

import de.dralle.bluetoothtest.R;

/**
 * Created by nils on 02.06.16.
 */
public class BluetoothListenerMaker {
    private static final String LOG_TAG = BluetoothListenerMaker.class.getName();
    private static BluetoothListenerMaker instance = null;

    /**
     * Singleton constructor
     *
     * @return Singleton instance
     */
    public static BluetoothListenerMaker getInstance() {
        if (instance == null) {
            instance = new BluetoothListenerMaker();
        }
        return instance;
    }

    /**
     * Singleton constructor
     *
     * @param res Local resource manager
     * @return Singleton instance
     */
    public static BluetoothListenerMaker getInstance(Resources res) {
        if (instance == null) {
            instance = new BluetoothListenerMaker(res);
        }
        return instance;
    }

    private Resources res = null;

    private BluetoothListenerMaker(Resources res) {
        this.res = res;
    }

    private BluetoothListenerMaker() {
        this.res = res;
    }

    /**
     * Assists the user in creating a listener
     *
     * @param secure Create a secure or an insecure socket
     * @return
     */
    public BluetoothListener createListener(boolean secure) {
        BluetoothListener listener = null;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothServerSocket serverSocket = null;
        if (adapter != null) {
            if (secure) {

                try {
                    Log.i(LOG_TAG, "Trying to create secure serversocket");
                    serverSocket=adapter.listenUsingRfcommWithServiceRecord("SecureListener",UUID.fromString(res.getString(R.string.uuid_secure)));
                    Log.i(LOG_TAG, "Created serversocket");
                } catch (IOException e) {
                    Log.w(LOG_TAG, "serversocket creation failed");
                    e.printStackTrace();
                }

            } else {
                try {
                    Log.i(LOG_TAG, "Trying to create secure serversocket");
                    serverSocket=adapter.listenUsingRfcommWithServiceRecord("SecureListener",UUID.fromString(res.getString(R.string.uuid_insecure)));
                    Log.i(LOG_TAG, "Created serversocket");
                } catch (IOException e) {
                    Log.w(LOG_TAG, "serversocket creation failed");
                    e.printStackTrace();
                }
            }
        }
        if (serverSocket != null) {
            return new BluetoothListener(secure,serverSocket);
        }
        return null;
    }
}
