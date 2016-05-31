package de.dralle.bluetoothtest.BGS;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by nils on 31.05.16.
 */
public class SPMAService extends Service {

    private static final String LOG_TAG = SPMAService.class.getName();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
}
