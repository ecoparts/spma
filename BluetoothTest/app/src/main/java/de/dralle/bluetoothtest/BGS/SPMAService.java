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
 * SPMAService is the background service of this app
 */
public class SPMAService extends IntentService {
    /**
     * Local broadcast tag. Used to receive internal messages
     */
    public static final String ACTION_NEW_MSG = "SPMAService.ACTION_NEW_MSG";
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = SPMAService.class.getName();
    /**
     * List of all nearby devices discovered during the last scanning round
     */
    private List<BluetoothDevice> devices;
    /**
     * Secure listener. Secure connections are used for connections between already paired devices
     */
    private BluetoothListener secureListener;
    /**
     * Insecure listener. Insecure connections are used for connections between non paired devices
     */
    private BluetoothListener insecureListener;

    /**
     * Nested BroadcastReceiver. Receives some android system broadcasts and internal messages directed at the service
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                Log.i(LOG_TAG, "New device found");

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (addNewDevice(device)) { //Only send message if device is new
                    sendNewDeviceFoundInternalMessage(device);
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
                    if (checkInternalMessage(msgData)) {
                        parseInternalMessageForAction(msgData);
                    }
                } else {
                    Log.w(LOG_TAG, "Message not JSON");
                }


            }
        }
    };

    /**
     * Adds a new device to the list of devices.
     * Tries to ensure that every device is only added once by checking its address
     *
     * @param device Device to be added
     * @return True if the device is new
     */
    private boolean addNewDevice(BluetoothDevice device) {
        for (BluetoothDevice d : devices) {
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
     *
     * @param device newly discovered device
     */
    private void sendNewDeviceFoundInternalMessage(BluetoothDevice device) {
        JSONObject mdvCmd = new JSONObject();
        try {
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "NewDevice");
            mdvCmd.put("Name", device.getName());
            mdvCmd.put("Address", device.getAddress());
            boolean bonded = device.getBondState() == BluetoothDevice.BOND_BONDED;
            mdvCmd.put("Paired", bonded);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendInternalMessageForSPMAServiceConnector(mdvCmd.toString());


    }

    /**
     * Send a message that a connection is ready, either from a list of cached connection or newly created
     *
     * @param con newly acquired connection
     */
    private void sendNewConnectionRetrievedInternalMessage(BluetoothConnection con) {
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
        sendInternalMessageForChatActivity(mdvCmd.toString(), con.getDevice().getAddress());


    }

    /**
     * Send a message that a connection failed
     *
     * @param device The RemoteDevice which this service couldnt make a connection to
     */
    private void sendNewConnectionFailedInternalMessage(BluetoothDevice device) {
        JSONObject mdvCmd = new JSONObject();
        try {
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "ConnectionFailed");
            mdvCmd.put("Address", device.getAddress());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendInternalMessageForChatActivity(mdvCmd.toString(), device.getAddress());


    }

    /**
     * Checks the message action attribute and executes the appropriate action
     *
     * @param msgData JSON formatted message to be checked
     */
    public void parseInternalMessageForAction(JSONObject msgData) {
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
                handleInternalConnectionRequest(msgData);
                break;
            case "Ready":
                sendConnectionReadyInternalMessage(msgData);
                break;
            case "Shutdown":
                sendConnectionShutdownInternalMessage(msgData);
                break;
            case "NewMessage":
                handleNewExternalMessage(msgData);
                break;
            case "SendNewMessage":
                prepareNewExternalMessage(msgData);
                break;
            default:
                Log.w(LOG_TAG, "Action not recognized: " + action);
                break;
        }
    }

    /**
     * Handle a internal connection request by performing a lookup on cached connections or creating a new one
     *
     * @param msgData may contain additional data
     * @return
     */
    private void handleInternalConnectionRequest(JSONObject msgData) {
        String address = null;
        try {
            address = msgData.getString("Address");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (address != null) {
            Log.i(LOG_TAG, "Requesting new connection for address " + address);
            BluetoothConnectionObserver bco = BluetoothConnectionObserver.getInstance();
            BluetoothConnection connection = bco.getConnection(address);
            if (connection != null) {
                Log.i(LOG_TAG, "Connection already there");
                sendNewConnectionRetrievedInternalMessage(connection);
            } else {
                Log.i(LOG_TAG, "Connection needs to be made");
                makeNewConnection(address);
            }
        }


    }

    /**
     * Prepare a new external message and wrap it into a JSON string
     *
     * @param msgData may contain additional data
     * @return
     */
    private void prepareNewExternalMessage(JSONObject msgData) {
        String address = null;
        String msg = "";
        try {
            address = msgData.getString("Address");
            msg = msgData.getString("Message");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (address != null) {
            Log.i(LOG_TAG, "Sending new message to " + address);

            BluetoothConnectionObserver bco = BluetoothConnectionObserver.getInstance();
            BluetoothConnection connection = bco.getConnection(address);

            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                JSONObject jsoOut = new JSONObject();
                try {

                    jsoOut.put("Extern", true);
                    jsoOut.put("Level", 0);
                    jsoOut.put("Content", "Text");
                    jsoOut.put("Receiver", address);
                    jsoOut.put("Sender", adapter.getAddress());
                    jsoOut.put("ReceiverAddress", address);
                    jsoOut.put("SenderAddress", adapter.getAddress());
                    jsoOut.put("SenderAPIVersion", Build.VERSION.SDK_INT);
                    jsoOut.put("SkipSenderAddressTest", Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);// WifiInfo.getMacAddress() and the BluetoothAdapter.getAddress() were "removed" in Android 6. They now return a constant value. Testing will therefore return wrong values.


                    jsoOut.put("Secure", connection.isSecureConnection());
                    jsoOut.put("Message", msg);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (connection != null) {
                    connection.sendExternalMessage(jsoOut.toString());
                } else {
                    Log.i(LOG_TAG, "No suitable connection found");

                }
            } else {
                Log.w(LOG_TAG, "No bluetooth. Cant send.");
            }


        }


    }

    /**
     * Handle received new external message. Free it from its JSON container string and check some of the additional message attributes.
     *
     * @param msgData may contain additional data
     * @return
     */
    private void handleNewExternalMessage(JSONObject msgData) {
        String address = null;
        String msg = "";
        try {
            address = msgData.getString("Address");
            msg = msgData.getString("Message");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject jsoIn = null;
        try {
            jsoIn = new JSONObject(msg);
            Log.i(LOG_TAG, "Transmitted: " + jsoIn.toString());
            if (jsoIn.getBoolean("Extern")) { //is this an external message?
                String content = jsoIn.getString("Content");
                String senderAddress = jsoIn.getString("SenderAddress");
                String receiverAddress = jsoIn.getString("ReceiverAddress");
                Boolean skipSenderTest = jsoIn.getBoolean("SkipSenderAddressTest");
                msg = jsoIn.getString("Message");
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (senderAddress.equals(address)) {//does the message come from where it says it comes from?
                    Log.i(LOG_TAG, "Sender confirmed");
                    if (content.equals("Text")) {//right "contentType? only text is supposed to be displayed
                        Log.i(LOG_TAG, "Content confirmed");


                        if (adapter != null) {

                            if (receiverAddress.equals(adapter.getAddress())) { //is this message even for me?
                                Log.i(LOG_TAG, "Me confirmed");
                                sendNewMessageInternalMessage(msg, address);
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // WifiInfo.getMacAddress() and the BluetoothAdapter.getAddress() were "removed" in Android 6. They now return a constant value.
                                Log.i(LOG_TAG, "Me not confirmed. But Android 6");
                                sendNewMessageInternalMessage(msg, address);
                            } else if (skipSenderTest) {
                                Log.i(LOG_TAG, "Me not confirmed. Skipping test");
                                sendNewMessageInternalMessage(msg, address);
                            }
                        }
                    }

                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.w(LOG_TAG, "Couldnt parse message");
        }


    }

    /**
     * Send a message that a new external message has arrived, directed at the GUI
     */
    private void sendNewMessageInternalMessage(String msg, String address) {
        JSONObject mdvCmd = new JSONObject();
        try {
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "NewMessage");
            mdvCmd.put("Address", address);
            mdvCmd.put("Sender", address);
            mdvCmd.put("Message", msg);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendInternalMessageForChatActivity(mdvCmd.toString(), address);

    }

    /**
     * Create a new connection
     * @param address remote device address
     */
    private void makeNewConnection(String address) {
        BluetoothDevice device = null;
        for (BluetoothDevice d : devices) {
            if (d.getAddress().equals(address)) {
                device = d;
                break;
            }

        }
        if (device != null) {
            Log.i(LOG_TAG, "Device known");
        } else {
            Log.i(LOG_TAG, "Device unknown. Creating new");
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                device = adapter.getRemoteDevice(address);
            }
        }
        if (device != null) {
            BluetoothConnection connection = BluetoothConnectionMaker.createConnection(device, getResources());
            if (connection != null) {
                connection.addObserver(BluetoothConnectionObserver.getInstance());
                BluetoothConnectionObserver.getInstance().registerConnection(connection);
                Thread t = new Thread(connection);
                t.start();
                sendNewConnectionRetrievedInternalMessage(connection);
            } else {
                sendNewConnectionFailedInternalMessage(device);
            }

        }

    }

    /**
     * Starts the 2 listeners, secure and insecure
     *
     * @param msgData may contain additional data
     *
     */
    private void startListeners(JSONObject msgData) {
        if (!secureListener.isListening()) {
            Thread t = new Thread(secureListener);
            t.start();
            Log.i(LOG_TAG, "Secure listener started");
        } else {
            secureListener.stopListener();
            Thread t = new Thread(secureListener);
            t.start();
            Log.i(LOG_TAG, "Secure listener restarted");
        }

        if (!insecureListener.isListening()) {
            Thread t = new Thread(insecureListener);
            t.start();
            Log.i(LOG_TAG, "Insecure listener started");
        } else {
            insecureListener.stopListener();
            Thread t = new Thread(insecureListener);
            t.start();
            Log.i(LOG_TAG, "Insecure listener restarted");
        }


        sendListenerStartInternalMessage();

    }

    /**
     * Send a message that Listeners started
     */
    private void sendListenerStartInternalMessage() {
        JSONObject mdvCmd = new JSONObject();
        try {
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "ListenersStarted");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendInternalMessageForSPMAServiceConnector(mdvCmd.toString());
    }

    /**
     * Send a internal message that a connection is ready
     *
     * @param msgData may contain additional data
     */
    private void sendConnectionReadyInternalMessage(JSONObject msgData) {
        JSONObject mdvCmd = new JSONObject();
        String address = "";
        try {
            address = msgData.getString("Address");
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "ConnectionReady");
            mdvCmd.put("Address", address);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendInternalMessageForSPMAServiceConnector(mdvCmd.toString());
        sendInternalMessageForChatActivity(mdvCmd.toString(), address);
    }

    /**
     * Send a internal message that a connection is shutdown
     *
     * @param msgData may contain additional data
     */
    private void sendConnectionShutdownInternalMessage(JSONObject msgData) {
        JSONObject mdvCmd = new JSONObject();
        String address = "";
        try {
            address = msgData.getString("Address");
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "ConnectionShutdown");
            mdvCmd.put("Address", address);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendInternalMessageForChatActivity(mdvCmd.toString(), address);
    }

    /**
     * Stops the listeners
     *
     * @param msgData may contain additional data
     */
    private void stopListeners(JSONObject msgData) {
        boolean stopped = true;
        if (secureListener.isListening()) {
            stopped = stopped && secureListener.stopListener();
            Log.i(LOG_TAG, "Secure listener stopped");
        }

        if (insecureListener.isListening()) {
            stopped = stopped && insecureListener.stopListener();
            Log.i(LOG_TAG, "Insecure listener stopped");
        }
        if (stopped) {
            Log.i(LOG_TAG, "Listeners stopped");
            sendListenerStopInternalMessage();
        }
    }

    /**
     * Send a message that Listeners stopped
     */
    private void sendListenerStopInternalMessage() {
        JSONObject mdvCmd = new JSONObject();
        try {
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "ListenersStopped");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendInternalMessageForSPMAServiceConnector(mdvCmd.toString());
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
        Log.w(LOG_TAG, "No bluetooth. Cant scan");
        return false;
    }

    /**
     * Checks if the message is plausible. Checks the attributes 'Extern' and 'Level'. Extern needs to be false, Level needs to be 0 (for non encrypted, cause not extern)
     *
     * @param msgData JSON formatted message to be checked
     * @return true if valid
     */

    public boolean checkInternalMessage(JSONObject msgData) {
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

    /**
     * Constructor. Gives a name this service. Initializes listeners and connection observer
     */
    public SPMAService() {
        super("SPMAService");
        devices = new ArrayList<>(); //initialize device list
        secureListener = new BluetoothListener(true, getResources().getString(R.string.uuid_secure));
        insecureListener = new BluetoothListener(false, getResources().getString(R.string.uuid_insecure));
        BluetoothConnectionObserver.getInstance().setService(this);
    }

    /**
     * Sends a message to the SPMAServiceConnector
     * @param msg Internal message to be sent
     */
    public void sendInternalMessageForSPMAServiceConnector(String msg) {
        sendInternalMessage(msg, SPMAServiceConnector.ACTION_NEW_MSG);


    }
    /**
     * Sends a message to the ChatActivity
     * @param msg Internal message to be sent
     *            @param btAddressRemoteDevice Address of the remote device that particular ChatActivity handles
     */
    public void sendInternalMessageForChatActivity(String msg, String btAddressRemoteDevice) {
        sendInternalMessage(msg, ChatActivity.ACTION_NEW_MSG + "_" + btAddressRemoteDevice);


    }

    /**
     * Sends a new internal message. Despite the name broadcast its actually somewhat directed
     * @param msg Message to be send
     * @param receiverBroadcastTag Receivers´ broadcast tag
     */
    private void sendInternalMessage(String msg, String receiverBroadcastTag) {
        Intent bgServiceIntent = new Intent(receiverBroadcastTag);
        bgServiceIntent.putExtra("msg", msg);
        sendBroadcast(bgServiceIntent);
        Log.v(LOG_TAG, "Send new broadcast to " + receiverBroadcastTag);


    }


    @Override
    protected void onHandleIntent(Intent intent) {


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
     *
     * @param intent  The Starting intent
     * @param flags
     * @param startId
     * @return START_STICKY, that android doesn´t remove this
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
}
