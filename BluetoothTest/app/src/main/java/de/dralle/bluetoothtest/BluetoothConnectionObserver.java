package de.dralle.bluetoothtest;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by Nils on 26.05.2016.
 */
public class BluetoothConnectionObserver implements Observer {
    private static BluetoothConnectionObserver ourInstance = new BluetoothConnectionObserver();

    public static BluetoothConnectionObserver getInstance() {
        return ourInstance;
    }

    private BluetoothConnectionObserver() {
    }

    @Override
    public void update(Observable observable, Object data) {

    }
}
