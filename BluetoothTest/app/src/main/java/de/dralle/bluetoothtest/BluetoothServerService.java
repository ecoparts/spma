package de.dralle.bluetoothtest;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Nils on 26.05.2016.
 */
public class BluetoothServerService extends IntentService {
    private static final String LOG_TAG = BluetoothServerService.class.getName();
    public BluetoothServerService(){
        super("BluetoothServerService");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(LOG_TAG,"New work request");
        String data=intent.getDataString();
        Log.i(LOG_TAG,"Data: "+data);
    }
}
