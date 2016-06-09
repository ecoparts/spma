package de.dralle.bluetoothtest.DB;

/**
 * Created by nils on 09.06.16.
 */
public class SPMADatabaseHelper {
    private static SPMADatabaseHelper ourInstance = new SPMADatabaseHelper();

    public static SPMADatabaseHelper getInstance() {
        return ourInstance;
    }

    private SPMADatabaseHelper() {
    }
}
