package de.dralle.bluetoothtest;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by Nils on 26.05.2016.
 */
public class BluetoothServerService extends IntentService {
    public BluetoothServerService(){
        super("BluetoothServerService");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        String data=intent.getDataString();
    }
}
