package de.dralle.bluetoothtest;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.Observable;

/**
 * Created by Nils on 26.05.2016.
 */
public class BluetoothConnection extends Observable implements Runnable{
    private static final String LOG_TAG = BluetoothConnection.class.getName();

    private BluetoothSocket socket;
    private BluetoothDevice device;

    public BluetoothConnection(BluetoothSocket socket,   boolean secureConnection) {
        this.socket = socket;
        this.device = socket.getRemoteDevice();
        this.secureConnection = secureConnection;
    }

    private boolean secureConnection=false;
    @Override
    public void run() {

    }

    public void close() {
        try {
            socket.close();
            Log.i(LOG_TAG,"Socket closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket=null;
        Log.i(LOG_TAG,"Socket removed");
    }
}
