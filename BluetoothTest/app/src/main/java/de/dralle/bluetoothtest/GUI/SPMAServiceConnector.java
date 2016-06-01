package de.dralle.bluetoothtest.GUI;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Messenger;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.dralle.bluetoothtest.BGS.SPMAService;
import de.dralle.bluetoothtest.BGS.deprecated.BluetoothServerService;

/**
 * Created by nils on 31.05.16.
 */
public class SPMAServiceConnector {
    private static final String LOG_TAG = SPMAServiceConnector.class.getName();
    private List<BluetoothDevice> devices;
    public static final String ACTION_NEW_MSG = "SPMAServiceConnector.ACTION_NEW_MSG";
    private final int REQUEST_ACCESS_COARSE_LOCATION = 1;
    private boolean listenersOnline=false;

    public boolean areListenersOnline() {
        return listenersOnline;
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra("msg");
            Log.i(LOG_TAG, "New message");
            Log.i(LOG_TAG, msg);
            JSONObject msgData = null;
            try {
                msgData = new JSONObject(msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (msgData != null) {
                if (checkMessage(msgData)) {
                    parseMessageForAction(msgData);
                }
            } else {
                Log.w(LOG_TAG, "Message not JSON");
            }

        }
    };

    private void parseMessageForAction(JSONObject msgData) {
        String action = getMessageAction(msgData);
        switch(action){
            case "NewDevice":
                handleNewDevice(msgData);
                break;
            case "ListenersStarted":
                listenersOnline=true;
                break;
            case "ListenersStopped":
                listenersOnline=false;
                break;
            default:
                break;
        }
    }

    /**
     *  handle a newly discovered bt device
     */
    private void handleNewDevice(JSONObject msgData) {
        String address="";
        try {
            address=msgData.getString("Address");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(BluetoothAdapter.checkBluetoothAddress(address)){
            BluetoothAdapter defaultAdapter=BluetoothAdapter.getDefaultAdapter();
            if(defaultAdapter!=null){
                BluetoothDevice device=defaultAdapter.getRemoteDevice(address);
                if(device!=null){
                    Log.i(LOG_TAG, "Added device with address "+address);
                    devices.add(device);
                }
            }else{
                Log.w(LOG_TAG,"Adapter is null. Now this is bad");
            }
        }else{
            Log.w(LOG_TAG,"Address "+address+" is not valid");
        }
    }


    private Activity parentActivity;

    public SPMAServiceConnector(Activity parentActivity) {
        this.parentActivity = parentActivity;
        devices=new ArrayList<>();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(parentActivity, Manifest.permission.ACCESS_COARSE_LOCATION.toString()) != PackageManager.PERMISSION_GRANTED) {

                parentActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION.toString()},REQUEST_ACCESS_COARSE_LOCATION); //need to request permission at runtime for android 6.0+

            }
        }

        if(isServiceRunning()){
            Log.i(LOG_TAG,"Service is running. Sending Scan");
            devices.clear(); //clear current device list
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
    /**
     * Checks if the message is plausible. Checks the attributes 'Extern' and 'Level'. Extern needs to be false, Level needs to be 0 (for non encrypted, cause not extern)
     *
     * @param msgData JSON formatted message to be checked
     * @return true if valid
     */

    public boolean checkMessage(JSONObject msgData) {
        boolean b = false;
        try {
            b = (!msgData.getBoolean("Extern") && msgData.getInt("Level") == 0);
        } catch (Exception e) {

        }
        return b;
    }

    public String getMessageAction(JSONObject msgData) {
        String action="";
        try {
            action=msgData.getString("Action");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return action;
    }

    /**
     * Calls the service to start listeners
     * @return true if service is running and message was sent
     */
    public boolean startListeners() {
        if(isServiceRunning()){
            Log.i(LOG_TAG,"Service is running. Sending startListeners");
            devices.clear(); //clear current device list
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "StartListeners");
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
     * Calls the service to stop listeners
     * @return true if service is running and message was sent
     */
    public boolean stopListeners() {
        if(isServiceRunning()){
            Log.i(LOG_TAG,"Service is running. Sending StopListeners");
            devices.clear(); //clear current device list
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "StopListeners");
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
