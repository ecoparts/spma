package de.dralle.bluetoothtest.GUI;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.dralle.bluetoothtest.BGS.SPMAService;
import de.dralle.bluetoothtest.DB.User;

/**
 * Created by nils on 31.05.16.
 */
public class SPMAServiceConnector {
    private static SPMAServiceConnector instance = null;
    /**
     * Selected user id.
     */
    private int userId;
    /**
     * The user from the DB which is in used by this app
     */
    private User u;

    public static SPMAServiceConnector getInstance(Activity parentActivity) {
        if (instance == null) {
            instance = new SPMAServiceConnector(parentActivity);
        }
        return instance;
    }

    private boolean receiveBroadcasts = false;
    private static final String LOG_TAG = SPMAServiceConnector.class.getName();
    private List<BluetoothDevice> supportedDevices;
    public static final String ACTION_NEW_MSG = "SPMAServiceConnector.ACTION_NEW_MSG";
    private final int REQUEST_ACCESS_COARSE_LOCATION = 1;
    private boolean listenersOnline = false;

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
                    parseInternalMessageForAction(msgData);
                }
            } else {
                Log.w(LOG_TAG, "Message not JSON");
            }

        }
    };

    private void parseInternalMessageForAction(JSONObject msgData) {
        String action = getMessageAction(msgData);
        String address = null;
        switch (action) {
            case "NewSupportedDevice":
                handleNewSupportedDevice(msgData);
                break;
            case "ClearDevices":
                supportedDevices.clear();
                Log.i(LOG_TAG, "Cached devices cleared");
                broadcastToNearbyDevicesFragment(msgData.toString());
                broadcastToGUI(msgData.toString());
                break;
            case "ListenersStarted":
                listenersOnline = true;
                break;
            case "ListenersStopped":
                listenersOnline = false;
                break;
            case "ConnectionReady":
                broadcastToGUI(msgData.toString());
                address = null;
                try {
                    address = msgData.getString("Address");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (address != null) {
                    broadcastToChatGUI(msgData.toString(), address);
                }
                break;
            case "CachedConnection":
                broadcastToConnectionsFragment(msgData.toString());
                break;
            case "ConnectionShutdown":
                broadcastToGUI(msgData.toString());
                address = null;
                try {
                    address = msgData.getString("Address");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (address != null) {
                    broadcastToChatGUI(msgData.toString(), address);
                }
                break;
            case "ConnectionRetrieved":
                broadcastToGUI(msgData.toString());
                address = null;
                try {
                    address = msgData.getString("Address");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (address != null) {
                    broadcastToChatGUI(msgData.toString(), address);
                }
                break;
            case "ConnectionFailed":
                broadcastToGUI(msgData.toString());
                address = null;
                try {
                    address = msgData.getString("Address");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (address != null) {
                    broadcastToChatGUI(msgData.toString(), address);
                }
                break;
            case "NewExternalMessage":
                broadcastToGUI(msgData.toString());
                address = null;
                try {
                    address = msgData.getString("Address");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (address != null) {
                    broadcastToChatGUI(msgData.toString(), address);
                }
                break;
            case "LocalUserSelected":
                saveNewUser(msgData);
                break;
            case "ServiceStarted":
                turnBluetoothOn();
                makeDeviceVisible(300);
                if(isBtOn()){
                    startListeners();
                }
                break;
            case "BluetoothTurnedOn":
                if(!listenersOnline){
                    startListeners();
                }
                break;
            case "ScanFinished":
                broadcastToNearbyDevicesFragment(msgData.toString());
            default:
                broadcastToGUI(msgData.toString());
                break;
        }
    }
    public void startChatActvity(String targetAddress){
        Intent newChatIntent = new Intent(parentActivity, ChatActivity.class);
        newChatIntent.putExtra("address", targetAddress);
        parentActivity.startActivity(newChatIntent);
    }
    public void startSettings(){

    }
    /**
     * Tries to extract the userdata from the received internal message string
     *
     * @param msgData received internal message
     */
    private void saveNewUser(JSONObject msgData) {
        if (u == null) {
            u = new User();
        }
        try {
            u.setId(msgData.getInt("ID"));
            u.setName(msgData.getString("Name"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        userId = u.getId();
    }

    /**
     * @return Local user name. Can be null.
     */
    public String getUserName() {

        if (u == null) {
            requestLocalUser(userId);
            return null;
        } else {
            return u.getName();
        }
    }

    /**
     * handle a newly discovered bt device
     */
    private void handleNewSupportedDevice(JSONObject msgData) {
        String address = "";
        try {
            address = msgData.getString("Address");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
            BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
            if (defaultAdapter != null) {
                BluetoothDevice device = defaultAdapter.getRemoteDevice(address);
                if (device != null) {
                    Log.i(LOG_TAG, "Added device with address " + address);
                    supportedDevices.add(device);
                    broadcastToNearbyDevicesFragment(msgData.toString());
                }
            } else {
                Log.w(LOG_TAG, "Adapter is null. Now this is bad");
            }
        } else {
            Log.w(LOG_TAG, "Address " + address + " is not valid");
        }
    }


    private Activity parentActivity;

    private SPMAServiceConnector(Activity parentActivity) {
        this.parentActivity = parentActivity;
        supportedDevices = new ArrayList<>();
        registerForBroadcasts();
    }

    public void registerForBroadcasts() {
        if (!receiveBroadcasts) {
            receiveBroadcasts = true;
            //register broadcast receiver for messages from the service
            IntentFilter filter = new IntentFilter(SPMAServiceConnector.ACTION_NEW_MSG);
            try {
                parentActivity.registerReceiver(broadcastReceiver, filter);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.i(LOG_TAG, "Receiver registered");
        }
    }

    public void unregisterForBroadcasts() {
        try {
            parentActivity.unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        receiveBroadcasts = false;
        Log.i(LOG_TAG, "Receiver unregistered");

    }


    public void startService() {
        if (!isServiceRunning()) {
            Intent bgServiceIntent = new Intent(parentActivity, SPMAService.class);
            parentActivity.startService(bgServiceIntent);


            Log.i(LOG_TAG, "Service started");
            try {
                Thread.sleep(50); //sleep a bit to give service time to come online
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a message to the service to make the device visible
     *
     * @return true if service is running and message was sent
     */
    @Deprecated
    public boolean makeDeviceVisible() {
        if (isServiceRunning()) {
            Log.i(LOG_TAG, "Service is running. Sending MakeVisible");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "MakeVisible");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            broadcastInternalMessageToService(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG, "Service not running");
        return false;


    }

    /**
     * Sends a message to the service to make the device visible
     *
     * @return true if service is running and message was sent
     */
    public boolean makeDeviceVisible(int duration) {
        if (isServiceRunning()) {
            Log.i(LOG_TAG, "Service is running. Sending MakeVisible");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "MakeVisible");
                mdvCmd.put("Duration", duration);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            broadcastInternalMessageToService(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG, "Service not running");
        return false;


    }

    public boolean isBtVisible() {
        BluetoothAdapter adaper = BluetoothAdapter.getDefaultAdapter();
        if (adaper != null) {
            return adaper.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
        }
        return false;
    }

    public boolean isBtOn() {
        BluetoothAdapter adaper = BluetoothAdapter.getDefaultAdapter();
        if (adaper != null) {
            return adaper.isEnabled();
        }
        return false;
    }


    public void broadcastInternalMessageToService(String msg) {
        broadcastTo(msg,SPMAService.ACTION_NEW_MSG);
    }
    public void broadcastTo(String msg, String target) {
        Intent bgServiceIntent = new Intent(target);
        bgServiceIntent.putExtra("msg", msg);
        parentActivity.sendBroadcast(bgServiceIntent);
    }

    public void broadcastToGUI(String msg) {
        broadcastTo(msg,MainActivity.ACTION_NEW_MSG);
    }

    public void broadcastToChatGUI(String msg, String address) {
        broadcastTo(msg,ChatActivity.ACTION_NEW_MSG+"_"+address);
    }

    public void broadcastToNearbyDevicesFragment(String msg) {
        broadcastTo(msg, OneFragment.ACTION_NEW_MSG);
    }
    public void broadcastToConnectionsFragment(String msg) {
        broadcastTo(msg, ThreeFragment.ACTION_NEW_MSG);
    }

    public void stopService() {
        Intent bgServiceIntent = new Intent(parentActivity, SPMAService.class);
        parentActivity.stopService(bgServiceIntent);
        Log.i(LOG_TAG, "Stopped service");
    }

    /**
     * Checks if SPMAService is running
     *
     * @return true if running
     */
    public boolean isServiceRunning() {
        ActivityManager am = (ActivityManager) parentActivity.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo rsi : services) {
            if (rsi.service.getClassName().equals(SPMAService.class.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sends a message to the service to turn bluetooth on
     *
     * @return true if service is running and message was sent
     */
    public boolean turnBluetoothOn() {
        if (isServiceRunning()) {
            Log.i(LOG_TAG, "Service is running. Sending TurnOn");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "TurnOn");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            broadcastInternalMessageToService(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG, "Service not running");
        return false;
    }

    /**
     * Sends a message to the service to turn bluetooth off
     *
     * @return true if service is running and message was sent
     */
    public boolean turnBluetoothOff() {
        if (isServiceRunning()) {
            Log.i(LOG_TAG, "Service is running. Sending TurnOff");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "TurnOff");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            broadcastInternalMessageToService(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG, "Service not running");
        return false;
    }

    /**
     * Sends a message to the service to scan for nearby devices
     *
     * @return true if service is running and message was sent
     */
    public boolean scanForNearbyDevices() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(parentActivity, Manifest.permission.ACCESS_COARSE_LOCATION.toString()) != PackageManager.PERMISSION_GRANTED) {
                parentActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION.toString()}, REQUEST_ACCESS_COARSE_LOCATION); //need to request permission at runtime for android 6.0+
            }
        }

        if (isServiceRunning()) {
            Log.i(LOG_TAG, "Service is running. Sending Scan");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "Scan");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            broadcastInternalMessageToService(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG, "Service not running");
        return false;
    }

    /**
     * Sends a message to the service to resend all cached devices
     *
     * @return true if service is running and message was sent
     */
    public boolean requestCachedDevices() {
        if (isServiceRunning()) {
            Log.i(LOG_TAG, "Service is running. Sending ResendCachedDevices");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "ResendCachedDevices");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            broadcastInternalMessageToService(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG, "Service not running");
        return false;
    }
    /**
     * Sends a message to the service to resend all cached connections
     *
     * @return true if service is running and message was sent
     */
    public boolean requestCachedConnections() {
        if (isServiceRunning()) {
            Log.i(LOG_TAG, "Service is running. Sending ResendCachedConnections");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "ResendCachedConnections");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            broadcastInternalMessageToService(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG, "Service not running");
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
        String action = "";
        try {
            action = msgData.getString("Action");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return action;
    }

    /**
     * Calls the service to start listeners
     *
     * @return true if service is running and message was sent
     */
    public boolean startListeners() {
        if (isServiceRunning()) {
            if (!listenersOnline) {
                Log.i(LOG_TAG, "Service is running. Sending startListeners");
                JSONObject mdvCmd = new JSONObject();
                try {
                    mdvCmd.put("Extern", false);
                    mdvCmd.put("Level", 0);
                    mdvCmd.put("Action", "StartListeners");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                broadcastInternalMessageToService(mdvCmd.toString());
                return true;
            }
            return false;
        }
        Log.w(LOG_TAG, "Service not running");
        return false;
    }

    /**
     * Calls the service to stop listeners
     *
     * @return true if service is running and message was sent
     */
    public boolean stopListeners() {
        if (isServiceRunning()) {
            Log.i(LOG_TAG, "Service is running. Sending StopListeners");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "StopListeners");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            broadcastInternalMessageToService(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG, "Service not running");
        return false;
    }

    /**
     * Gets a Bluetooth device by its index
     *
     * @param id the index
     * @return the device with this index
     */
    public BluetoothDevice getDeviceByIndex(int id) {
        if (id > -1 && id < supportedDevices.size()) {
            return supportedDevices.get(id);
        }
        return null;
    }

    /**
     * Calls the service to get a connection ready
     *
     * @return true if service is running and message was sent
     */
    public boolean requestNewConnection(String address) {
        if (isServiceRunning()) {
            Log.i(LOG_TAG, "Service is running. Sending RequestConnection");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "RequestConnection");
                mdvCmd.put("Address", address);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            broadcastInternalMessageToService(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG, "Service not running");
        return false;
    }

    /**
     * Calls the service to send a new external message
     *
     * @return true if service is running and message was sent
     */
    public boolean sendExternalMessage(String msg, String receiverDeviceAddress) {
        if (isServiceRunning()) {
            Log.i(LOG_TAG, "Service is running. Sending SendNewMessage");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "SendNewMessage");
                mdvCmd.put("Sender", getUserName());
                mdvCmd.put("SenderID", userId);
                mdvCmd.put("Address", receiverDeviceAddress);
                mdvCmd.put("Message", msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            broadcastInternalMessageToService(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG, "Service not running");
        return false;
    }

    /**
     * Calls the service to add a new user
     *
     * @return true if service is running and message was sent
     */
    @Deprecated
    public boolean addUser(String name) {
        if (isServiceRunning()) {
            Log.i(LOG_TAG, "Service is running. Sending AddNewLocalUser");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "AddNewLocalUser");
                mdvCmd.put("Name", name);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            broadcastInternalMessageToService(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG, "Service not running");
        return false;
    }

    /**
     * Calls the service to add a change the local username
     *
     * @return true if service is running and message was sent
     */

    public boolean changeUserName(String name) {
        if (isServiceRunning()) {
            Log.i(LOG_TAG, "Service is running. Sending ChangeLocalUserName");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "ChangeLocalUserName");
                mdvCmd.put("ID", userId);
                mdvCmd.put("Name", name);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            broadcastInternalMessageToService(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG, "Service not running");
        return false;
    }

    /**
     * Calls the service to add a regenerate encryption keys
     *
     * @return true if service is running and message was sent
     */

    public boolean regenerateKeys(String name) {
        if (isServiceRunning()) {
            Log.i(LOG_TAG, "Service is running. Sending RegenerateKeys");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "RegenerateKeys");
                mdvCmd.put("UserID", userId);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            broadcastInternalMessageToService(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG, "Service not running");
        return false;
    }

    /**
     * Calls the service to send the user data
     *
     * @return true if service is running and message was sent
     */
    private boolean requestLocalUser(int id) {
        if (isServiceRunning()) {
            Log.i(LOG_TAG, "Service is running. Sending RequestLocalUser");
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "RequestLocalUser");
                mdvCmd.put("ID", id);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            broadcastInternalMessageToService(mdvCmd.toString());
            return true;
        }
        Log.w(LOG_TAG, "Service not running");
        return false;
    }

    public void selectUser(int id) {
        if (id >= 0) {
            userId = 0;
            requestLocalUser(id);
        }
    }
}
