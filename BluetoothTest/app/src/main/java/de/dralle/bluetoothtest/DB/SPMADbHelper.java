package de.dralle.bluetoothtest.DB;

/**
 * Created by nils on 09.06.16.
 */
public class SPMADbHelper {
    private static SPMADbHelper ourInstance = new SPMADbHelper();

    public static SPMADbHelper getInstance() {
        return ourInstance;
    }

    private SPMADbHelper() {
    }
}
