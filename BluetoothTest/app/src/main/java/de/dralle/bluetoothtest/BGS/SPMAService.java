package de.dralle.bluetoothtest.BGS;

import android.Manifest;
import android.app.IntentService;
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

import de.dralle.bluetoothtest.GUI.ChatActivity;
import de.dralle.bluetoothtest.GUI.SPMAServiceConnector;
import de.dralle.bluetoothtest.R;

/**
 * Created by nils on 31.05.16.
 */
public class SPMAService extends IntentService {
    public static final String ACTION_NEW_MSG = "SPMAService.ACTION_NEW_MSG";
    private static final String LOG_TAG = SPMAService.class.getName();
    private List<BluetoothDevice> devices;
    private BluetoothListener secureListener;
    private BluetoothListener insecureListener;


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                Log.i(LOG_TAG, "New device found");

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
               if(addNewDevice(device)){ //Only send message if device is new
                   sendNewDeviceFoundMessage(device);
               }


            }
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.i(LOG_TAG, "New device bonded");
                    Log.i(LOG_TAG, device.getAddress());
                    Log.i(LOG_TAG, device.getName());
                } else {
                    Log.i(LOG_TAG, "Bonding failed");
                }
            }
            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);


                Log.i(LOG_TAG, "Device requesting pairing");
                Log.i(LOG_TAG, device.getAddress());
                Log.i(LOG_TAG, device.getName());


            }
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
                if (defaultAdapter != null) {
                    if (defaultAdapter.isEnabled()) {
                        Log.i(LOG_TAG, "Bluetooth is now enabled");
                    } else {
                        Log.i(LOG_TAG, "Bluetooth is now disabled");
                    }
                } else {
                    Log.w(LOG_TAG, "BluetoothAdapter is null after state change. Its bad");
                }


            }
            if (SPMAService.ACTION_NEW_MSG.equals(action)) {
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
        }
    };

    /**
     * Adds a new device to the List of devices.
     * Tries to ensure that every device is only added once by checking its address
     * @param device Device to be added
     * @return True if the device is new
     */
    private boolean addNewDevice(BluetoothDevice device) {
        for(BluetoothDevice d:devices) {
            if (d.getAddress().equals(device.getAddress())) {
                Log.w(LOG_TAG, device.getAddress() + " already known");
                return false;
            }

        }
        devices.add(device);
        return true;

    }

    /**
     * Send a message that a new device was discovered
     * @param device newly discovered device
     */
    private void sendNewDeviceFoundMessage(BluetoothDevice device) {
            JSONObject mdvCmd = new JSONObject();
            try {
                mdvCmd.put("Extern", false);
                mdvCmd.put("Level", 0);
                mdvCmd.put("Action", "NewDevice");
                mdvCmd.put("Name", device.getName());
                mdvCmd.put("Address", device.getAddress());
                boolean bonded=device.getBondState()==BluetoothDevice.BOND_BONDED;
                mdvCmd.put("Paired",bonded);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sendMessageForSPMAServiceConnector(mdvCmd.toString());


    }
    /**
     * Send a message that a connection is ready
     * @param con newly acquired connection
     */
    private void sendNewConnectionRetrieved(BluetoothConnection con) {
        JSONObject mdvCmd = new JSONObject();
        try {
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "ConnectionRetrieved");
            mdvCmd.put("Secure", con.isSecureConnection());
            mdvCmd.put("Address", con.getDevice().getAddress());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendMessageForChatActivity(mdvCmd.toString(),con.getDevice().getAddress());


    }
    /**
     * Send a message that a connection failed
     * @param device The RemoteDevice which this service couldnt make a connection to
     */
    private void sendNewConnectionFailed(BluetoothDevice device) {
        JSONObject mdvCmd = new JSONObject();
        try {
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "ConnectionFailed");
            mdvCmd.put("Address", device.getAddress());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendMessageForChatActivity(mdvCmd.toString(),device.getAddress());


    }

    /**
     * Checks the message action attribute and executes the appropriate action
     *
     * @param msgData JSON formatted message to be checked
     */
    public void parseMessageForAction(JSONObject msgData) {
        String action = "";
        try {
            action = msgData.getString("Action");
        } catch (JSONException e) {
            e.printStackTrace();

        }
        if (action == null) {
            action = "";
        }
        switch (action) {
            case "MakeVisible":
                makeDeviceVisible(msgData);
                break;
            case "TurnOn":
                turnBluetoothOn();
                break;
            case "Scan":
                scanForNearbyDevices(msgData);
                break;
            case "StartListeners":
                startListeners(msgData);
                break;
            case "StopListeners":
                stopListeners(msgData);
                break;
            case "RequestConnection":
                handleConnectionRequest(msgData);
                break;
            case "Ready":
                sendConnectionReadyMessage(msgData);
                break;
            case "Shutdown":
                sendConnectionShutdownMessage(msgData);
                break;
            case "NewMessage":
                break;
            default:
                Log.w(LOG_TAG, "Action not recognized: " + action);
                break;
        }
    }
    /**
     * Request a connection
     *
     * @param msgData may contain additional data
     * @return
     */
    private void handleConnectionRequest(JSONObject msgData) {
        String address= null;
        try {
            address = msgData.getString("Address");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(address!=null){
            Log.i(LOG_TAG,"Requesting new connection for address "+address);
            BluetoothConnectionObserver bco=BluetoothConnectionObserver.getInstance();
            BluetoothConnection connection=bco.getConnection(address);
            if(connection!=null){
                Log.i(LOG_TAG,"Connection already there");
                sendNewConnectionRetrieved(connection);
            }else{
                Log.i(LOG_TAG,"Connection needs to be made");
                makeNewConnection(address);
            }
        }


    }

    private void makeNewConnection(String address) {
        BluetoothDevice device=null;
        for(BluetoothDevice d:devices) {
            if (d.getAddress().equals(address)) {
                device=d;
                break;
            }

        }
        if(device!=null){
            Log.i(LOG_TAG,"Device known");
        }else{
            Log.i(LOG_TAG,"Device unknown. Creating new");
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if(adapter!=null){
                device=adapter.getRemoteDevice(address);
            }
        }
        if(device!=null){
            BluetoothConnection connection = BluetoothConnectionMaker.createConnection(device,getResources());
            if(connection!=null){
                connection.addObserver(BluetoothConnectionObserver.getInstance());
                BluetoothConnectionObserver.getInstance().registerConnection(connection);
                Thread t=new Thread(connection);
                t.start();
                sendNewConnectionRetrieved(connection);
            }else{
                sendNewConnectionFailed(device);
            }

        }

    }

    /**
     * Starts the listeners
     *
     * @param msgData may contain additional data
     * @return scan was successfully initialized
     */
    private void startListeners(JSONObject msgData) {
        if(!secureListener.isListening()){
            Thread t=new Thread(secureListener);
            t.start();
            Log.i(LOG_TAG,"Secure listener started");
        }else{
            secureListener.stopListener();
            Thread t=new Thread(secureListener);
            t.start();
            Log.i(LOG_TAG,"Secure listener restarted");
        }

        if(!insecureListener.isListening()){
            Thread t=new Thread(insecureListener);
            t.start();
            Log.i(LOG_TAG,"Insecure listener started");
        }else{
            insecureListener.stopListener();
            Thread t=new Thread(insecureListener);
            t.start();
            Log.i(LOG_TAG,"Insecure listener restarted");
        }


            sendListenerStartMessage();

    }
    /**
     * Send a message that Listeners started
     */
    private void sendListenerStartMessage() {
        JSONObject mdvCmd = new JSONObject();
        try {
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "ListenersStarted");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendMessageForSPMAServiceConnector(mdvCmd.toString());
    }
    /**
     * Send a message that a connection is ready
     *
     * @param msgData may contain additional data
     */
    private void sendConnectionReadyMessage(JSONObject msgData) {
        JSONObject mdvCmd = new JSONObject();
        String address="";
        try {
            address=msgData.getString("Address");
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "ConnectionReady");
            mdvCmd.put("Address", address);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendMessageForSPMAServiceConnector(mdvCmd.toString());
        sendMessageForChatActivity(mdvCmd.toString(),address);
    }
    /**
     * Send a message that a connection is shutdown
     *
     * @param msgData may contain additional data
     */
    private void sendConnectionShutdownMessage(JSONObject msgData) {
        JSONObject mdvCmd = new JSONObject();
        String address="";
        try {
            address=msgData.getString("Address");
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "ConnectionShutdown");
            mdvCmd.put("Address", address);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendMessageForChatActivity(mdvCmd.toString(),address);
    }

    /**
     * Stops the listeners
     *
     * @param msgData may contain additional data
     * @return scan was successfully initialized
     */
    private void stopListeners(JSONObject msgData) {
        boolean stopped=true;
        if(secureListener.isListening()){
            stopped = stopped&&secureListener.stopListener();
            Log.i(LOG_TAG,"Secure listener stopped");
        }

        if(insecureListener.isListening()){
            stopped = stopped&&insecureListener.stopListener();
            Log.i(LOG_TAG,"Insecure listener stopped");
        }
        if(stopped){
            Log.i(LOG_TAG,"Listeners stopped");
            sendListenerStopMessage();
        }
    }
    /**
     * Send a message that Listeners stopped
     */
    private void sendListenerStopMessage() {
        JSONObject mdvCmd = new JSONObject();
        try {
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "ListenersStopped");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendMessageForSPMAServiceConnector(mdvCmd.toString());
    }

    /**
     * Starts scanning for bluetooth devices
     *
     * @param msgData may contain additional data
     * @return scan was successfully initialized
     */
    private boolean scanForNearbyDevices(JSONObject msgData) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION.toString()) == PackageManager.PERMISSION_GRANTED) {

                Log.i(LOG_TAG, "Permission ACCESS_COARSE_LOCATION granted");
            } else {
                Log.w(LOG_TAG, "Permission ACCESS_COARSE_LOCATION denied");
                return false;
            }

        }
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            if (btAdapter.isEnabled()) {

                devices.clear(); //clear current device list
                if (btAdapter.isDiscovering()) {
                    Log.w(LOG_TAG, "Discovery running. Cancelling discovery");
                    btAdapter.cancelDiscovery();
                }
                Log.i(LOG_TAG, "Starting discovery");
                if (btAdapter.startDiscovery()) {
                    Log.i(LOG_TAG, "Started discovery");
                    return true;
                } else {
                    Log.i(LOG_TAG, "Failed to start discovery");
                }
            } else {
                Log.i(LOG_TAG, "Bluetooth disabled. Cant scan");

            }
        }
        Log.w(LOG_TAG,"No bluetooth. Cant scan");
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

    /**
     * Checks if the device is discoverable, and if no requests it
     *
     * @param msgData may contain additional data
     */
    private void makeDeviceVisible(JSONObject msgData) {
        //if a duration is given, use that
        int duration = 0; //0 for always visible
        try {
            duration = msgData.getInt("Duration");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            Log.i(LOG_TAG, "Making device discoverable");
            if (btAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent discoverableIntent = new
                        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
                discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(discoverableIntent);
            } else {
                Log.v(LOG_TAG, "Device already visible");
            }

        }
    }

    /**
     * Checks if bluetooth is on, and if no requests permission to turn it on
     */
    private void turnBluetoothOn() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            Log.i(LOG_TAG, "Turning bluetooth on");
            if (btAdapter.isEnabled()) {
                Intent btOnRequest = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                btOnRequest.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //Flag needed when starting from a service
                startActivity(btOnRequest);

            } else {
                Log.v(LOG_TAG, "Bluetooth already on");
            }

        }
    }


    public SPMAService() {
        super("SPMAService");
    }

    public void sendMessageForSPMAServiceConnector(String msg) {
       sendMessage(msg,SPMAServiceConnector.ACTION_NEW_MSG);



    }
    public void sendMessageForChatActivity(String msg,String btAddressRemoteDevice) {
       sendMessage(msg, ChatActivity.ACTION_NEW_MSG+"_"+btAddressRemoteDevice);



    }
    private void sendMessage(String msg,String receiverBroadcastTag) {
        Intent bgServiceIntent = new Intent(receiverBroadcastTag);
        bgServiceIntent.putExtra("msg", msg);
        sendBroadcast(bgServiceIntent);
        Log.v(LOG_TAG,"Send new broadcast to "+receiverBroadcastTag);



    }


    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(LOG_TAG, "New work request");
        String data = intent.getStringExtra("msg");
        Log.i(LOG_TAG, "Data: " + data);

    }

    @Override
    public void onCreate() {
        super.onCreate();

        //register a receiver for broadcasts
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter);

        filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(broadcastReceiver, filter);

        filter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(broadcastReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(broadcastReceiver, filter);

        filter = new IntentFilter(SPMAService.ACTION_NEW_MSG);
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    /**
     * Called when the service is started
     * @param intent The Starting intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
       devices=new ArrayList<>(); //initialize device list
        secureListener=new BluetoothListener(true,getResources().getString(R.string.uuid_secure));
        insecureListener=new BluetoothListener(false,getResources().getString(R.string.uuid_insecure));
        BluetoothConnectionObserver.getInstance().setService(this);
        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
}
