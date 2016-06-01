package de.dralle.bluetoothtest.GUI;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import de.dralle.bluetoothtest.BGS.SPMAService;
import de.dralle.bluetoothtest.BGS.deprecated.BluetoothServerService;

/**
 * Created by nils on 31.05.16.
 */
public class SPMAServiceConnector {
    private static final String LOG_TAG = SPMAServiceConnector.class.getName();
    public static final String ACTION_NEW_MSG = "SPMAServiceConnector.ACTION_NEW_MSG";
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SPMAServiceConnector.ACTION_NEW_MSG.equals(action)) {
              Log.i(LOG_TAG,"New message from service");

            }

        }
    };



    private Activity parentActivity;

    public SPMAServiceConnector(Activity parentActivity) {
        this.parentActivity = parentActivity;
    }



    public void startService(){
        Intent bgServiceIntent = new Intent(parentActivity, SPMAService.class);
        parentActivity.startService(bgServiceIntent);

        //register broadcast receiver for messages from the service
        IntentFilter filter=new IntentFilter(SPMAServiceConnector.ACTION_NEW_MSG);
        try {
            parentActivity.registerReceiver(broadcastReceiver, filter);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to the service to make the device visible
     * @return true if service is running and message was sent
     */
    public boolean makeDeviceVisible(){
        if(isServiceRunning()){
            Log.i(LOG_TAG,"Service is running. Sending MakeVisible");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "MakeVisible");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sendMessage(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG,"Service not running");
        return false;


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

        //unregister receiver
        parentActivity.unregisterReceiver(broadcastReceiver);
    }

    /**
     * Checks if SPMAService is running
     * @return true if running
     */
    public boolean isServiceRunning(){
        ActivityManager am=(ActivityManager)parentActivity.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(Integer.MAX_VALUE);
        for(ActivityManager.RunningServiceInfo rsi:services){

            if(rsi.service.getClassName().equals(SPMAService.class.getName())){
                return true;
            }
        }
        return false;
    }

    /**
     * Sends a message to the service to turn bluetooth on
     * @return true if service is running and message was sent
     */
    public boolean turnBluetoothOn() {
        if(isServiceRunning()){
            Log.i(LOG_TAG,"Service is running. Sending TurnOn");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "TurnOn");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sendMessage(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG,"Service not running");
        return false;
    }
    /**
     * Sends a message to the service to scan for nearby devices
     * @return true if service is running and message was sent
     */
    public boolean scanForNearbyDevices() {
        if(isServiceRunning()){
            Log.i(LOG_TAG,"Service is running. Sending Scan");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "Scan");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sendMessage(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG,"Service not running");
        return false;
    }
}
