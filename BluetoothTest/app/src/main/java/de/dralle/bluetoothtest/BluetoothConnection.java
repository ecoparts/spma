package de.dralle.bluetoothtest;

import android.bluetooth.BluetoothSocket;

import java.util.Observable;

/**
 * Created by Nils on 26.05.2016.
 */
public class BluetoothConnection extends Observable implements Runnable{
    public BluetoothConnection(BluetoothSocket socket) {
        this.socket = socket;
    }

    private BluetoothSocket socket;
    @Override
    public void run() {

    }
}
