package de.dralle.bluetoothtest.GUI;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;

import java.util.List;

import de.dralle.bluetoothtest.BGS.SPMAService;
import de.dralle.bluetoothtest.BGS.deprecated.BluetoothServerService;

/**
 * Created by nils on 31.05.16.
 */
public class SPMAServiceConnector implements ServiceConnection{
    private static final String LOG_TAG = SPMAServiceConnector.class.getName();
    private boolean serviceBound=false;
    private Messenger serviceMessenger=null;

    public boolean isServiceBound() {
        return serviceBound;
    }

    private Activity parentActivity;

    public SPMAServiceConnector(Activity parentActivity) {
        this.parentActivity = parentActivity;
    }



    public void startService(){
        Intent bgServiceIntent = new Intent(parentActivity, SPMAService.class);
        parentActivity.startService(bgServiceIntent);
    }
    public void bindService(){
        if(!serviceBound){
            Intent bgServiceIntent = new Intent(parentActivity, SPMAService.class);
            parentActivity.bindService(bgServiceIntent,this,Context.BIND_ABOVE_CLIENT);

        }

    }
    public void unbindService(){
        if(serviceBound){
            Intent bgServiceIntent = new Intent(parentActivity, SPMAService.class);
            parentActivity.unbindService(this);

        }


    }
    public void sendMessage(String msg){
        Intent bgServiceIntent = new Intent(SPMAService.ACTION_NEW_MSG);
        bgServiceIntent.putExtra("msg", msg);
        parentActivity.sendBroadcast(bgServiceIntent);
        //parentActivity.startService(bgServiceIntent);
    }
    public void stopService(){
        Intent bgServiceIntent = new Intent(parentActivity, SPMAService.class);
        parentActivity.stopService(bgServiceIntent);
    }
    public boolean isServiceRunning(){
        ActivityManager am=(ActivityManager)parentActivity.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(Integer.MAX_VALUE);
        for(ActivityManager.RunningServiceInfo rsi:services){
            Log.v(LOG_TAG,"Comparing "+rsi.service.getClassName()+" to "+SPMAService.class.getName());
            if(rsi.service.getClassName().equals(SPMAService.class.getName())){
                return true;
            }
        }
        return false;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.i(LOG_TAG,"Service "+componentName+" bound");
        serviceBound=true;


    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.i(LOG_TAG,"Service "+componentName+" unbound");
        serviceBound=false;
    }
}
