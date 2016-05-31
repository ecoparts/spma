package de.dralle.bluetoothtest.GUI;

/**
 * Created by nils on 31.05.16.
 */
public class SPMAServiceConnector {
    private static SPMAServiceConnector ourInstance = new SPMAServiceConnector();

    public static SPMAServiceConnector getInstance() {
        return ourInstance;
    }

    private SPMAServiceConnector() {
    }
}
