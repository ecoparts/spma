package de.dralle.bluetoothtest.BGS;

import android.Manifest;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.dralle.bluetoothtest.DB.SPMADatabaseAccessHelper;
import de.dralle.bluetoothtest.DB.User;
import de.dralle.bluetoothtest.GUI.ChatActivity;
import de.dralle.bluetoothtest.GUI.MainActivity;
import de.dralle.bluetoothtest.GUI.SPMAServiceConnector;
import de.dralle.bluetoothtest.R;

/**
 * Created by nils on 31.05.16.
 * SPMAService is the background service of this app TODO: maybe merge liistener classes with connection classes through inheritance
 */
public class SPMAService extends IntentService {
    /**
     * Notification id for the main notification
     */
    private static final int SPMA_NOTIFICATION_ID = 91;
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
     * List of all nearby devices which support the chat service
     */
    private List<BluetoothDevice> supportedDevices;
    /**
     * Secure listener. Secure connections are used for connections between already paired devices
     */
    private BluetoothListener secureListener;
    /**
     * Insecure listener. Insecure connections are used for connections between non paired devices
     */
    private BluetoothListener insecureListener;
    private SPMADatabaseAccessHelper db;

    /**
     * Nested BroadcastReceiver. Receives some android system broadcasts and internal messages directed at the service
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        private BluetoothDevice nextDeviceToScan = null;

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                Log.i(LOG_TAG, "New device found");

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                ParcelUuid[] allUUIDs = device.getUuids();

                addNewDevice(device);

                if (checkForSupportedUUIDs(allUUIDs)) {

                    sendNewDeviceFoundInternalMessage(device);
                }


            }
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.i(LOG_TAG, "Discovery started");
                sendClearDevicesInternalMessage();
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(LOG_TAG, "Discovery finished. " + devices.size() + " devices found");
                if (devices.size() > 0) {
                    nextDeviceToScan = devices.get(0);
                    devices.remove(0);
                    nextDeviceToScan.fetchUuidsWithSdp();
                }
            }
            if (BluetoothDevice.ACTION_UUID.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(LOG_TAG, "Sdp scan for device " + device.getAddress());
                ParcelUuid[] allUUIDs = device.getUuids();

                if (allUUIDs == null) {
                    Log.i(LOG_TAG, "Device " + device.getAddress() + " supports null UUIDs");
                } else {
                    Log.i(LOG_TAG, "Device " + device.getAddress() + " supports " + allUUIDs.length + " UUIDs");
                    for (ParcelUuid uuid : allUUIDs) {
                        Log.v(LOG_TAG, "Device " + device.getAddress() + " supports UUID " + uuid.getUuid().toString());
                    }
                }
                if (checkForSupportedUUIDs(allUUIDs)) {
                    Log.i(LOG_TAG, "Device " + device.getAddress() + " supported");
                    addNewSupportedDevice(device);
                } else {
                    Log.i(LOG_TAG, "Device " + device.getAddress() + " not supported");
                }
                sendNewSupportedDeviceListInternalMessage();
                if (devices.size() > 0) {
                    nextDeviceToScan = devices.get(0);
                    devices.remove(0);
                    nextDeviceToScan.fetchUuidsWithSdp();
                } else {
                    Log.i(LOG_TAG, "Fetching UUIDs for all devices finished. Found " + supportedDevices.size() + " connect-worthy devices");
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

    private void sendNewSupportedDeviceListInternalMessage() {
        sendClearDevicesInternalMessage();
        for (BluetoothDevice device : supportedDevices)
            sendNewDeviceFoundInternalMessage(device);
    }


    private boolean checkForSupportedUUIDs(ParcelUuid[] allUUIDs) {
        if (allUUIDs != null) {
            for (ParcelUuid uuid : allUUIDs) {
                if (uuid.getUuid().compareTo(UUID.fromString(getResources().getString(R.string.uuid_secure))) == 0) {
                    return true;
                }
                if (uuid.getUuid().compareTo(UUID.fromString(getResources().getString(R.string.uuid_insecure))) == 0) {
                    return true;
                }

                UUID reversed = reverseUuid(uuid.getUuid()); //https://code.google.com/p/android/issues/detail?id=198238
                if (reversed.compareTo(UUID.fromString(getResources().getString(R.string.uuid_secure))) == 0) {
                    return true;
                }
                if (reversed.compareTo(UUID.fromString(getResources().getString(R.string.uuid_insecure))) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private UUID reverseUuid(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        byte[] uuidBytes = bb.array();

        byte[] uuidNewBytes = new byte[uuidBytes.length];
        for (int i = 0; i < uuidBytes.length; i++) {
            uuidNewBytes[i] = uuidBytes[uuidBytes.length - i - 1];
        }
        ByteBuffer bb2 = ByteBuffer.wrap(uuidNewBytes);
        return new UUID(bb2.getLong(), bb2.getLong());
    }

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
     * Adds a new device to the list of supported devices.
     * Tries to ensure that every device is only added once by checking its address
     *
     * @param device Device to be added
     * @return True if the device is new
     */
    private boolean addNewSupportedDevice(BluetoothDevice device) {
        for (BluetoothDevice d : supportedDevices) {
            if (d.getAddress().equals(device.getAddress())) {
                Log.w(LOG_TAG, device.getAddress() + " already known");
                return false;
            }

        }
        supportedDevices.add(device);
        return true;

    }

    /**
     * Send a message that the GUI should clear its device list
     */
    private void sendClearDevicesInternalMessage() {
        JSONObject mdvCmd = new JSONObject();
        try {
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "ClearDevices");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendInternalMessageForSPMAServiceConnector(mdvCmd.toString());


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
            mdvCmd.put("Name", device.getName()); //TODO: Friendly name
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
        sendInternalMessageForSPMAServiceConnector(mdvCmd.toString());


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
        sendInternalMessageForSPMAServiceConnector(mdvCmd.toString());


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
            case "TurnOff":
                turnBluetoothOff();
                break;
            case "Scan":
                scanForNearbyDevices(msgData);
                break;
            case "ResendCachedDevices":
                sendClearDevicesInternalMessage();
                for(BluetoothDevice d:supportedDevices){
                    sendNewDeviceFoundInternalMessage(d);
                }
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
            case "AddNewLocalUser":
                addNewLocalUser(msgData);
                break;
            case "RequestLocalUser":
                requestLocalUserData(msgData);
                break;
            default:
                Log.w(LOG_TAG, "Action not recognized: " + action);
                break;
        }
    }

    private void requestLocalUserData(JSONObject msgData) {
        int id=-1;
        try {
            id=msgData.getInt("ID");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(LOG_TAG,"Now trying to get the data of user "+id);
        if(id>-1){
           User u=null;
            u=db.getUser(id);
            if(u==null){
                Log.i(LOG_TAG,"No user found. Adding new.");
                u=db.addUser(getLocalDeviceName());
            }
            sendLocalUserSelectedInternalMessage(u);
        }else{
            Log.w(LOG_TAG,"Local user selection failed");
        }

    }

    /**
     *
     * @return The local bluetooth device name
     */
    private String getLocalDeviceName() {
        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
        if(adapter!=null){
            return adapter.getName();
        }
        return null;
    }

    public void addNewLocalUser(JSONObject msgData){
        String newUserName= null;
        try {
            newUserName = msgData.getString("Name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(LOG_TAG,"Now trying to add new user with name "+newUserName);
        db.addUser(newUserName);

    }
    /**
     * Checks the message action attribute and executes the appropriate action
     *
     * @param msgData JSON formatted message to be checked
     */
    public void parseInternalListenerMessageForAction(JSONObject msgData) {
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
            case "Startup":
                sendListenerStartInternalMessage();
                break;
            case "Shutdown":
                sendListenerStopInternalMessage();
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
                sendConnectionReadyInternalMessage(msgData);
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
        int senderID=-1;
        try {
            address = msgData.getString("Address");
            msg = msgData.getString("Message");
            senderID=msgData.getInt("SenderID");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        User sender=db.getUser(senderID);
        if (address != null) {
            //verschlüsselun!!!!
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
                    jsoOut.put("Receiver", db.getDeviceFriendlyName(address));
                    jsoOut.put("Sender", sender.getName());
                    jsoOut.put("ReceiverAddress", address);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        jsoOut.put("SenderAddress", adapter.getAddress());
                    }

                    jsoOut.put("SenderVersionAPI", Build.VERSION.SDK_INT);
                    jsoOut.put("SenderVersionApp", getResources().getString(R.string.app_version));
                    jsoOut.put("Timestamp", System.currentTimeMillis() / 1000);

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
                //decrypt
                int senderAPIVersion = jsoIn.getInt("SenderVersionAPI");
                String senderAppVersion = jsoIn.getString("SenderVersionApp");
                if (!getResources().getString(R.string.app_version).equals(senderAppVersion)) {
                    Log.w(LOG_TAG, "App version mismatch. Things might go horribly wrong. You have been warned");
                }
                String senderAddress = null;
                if (senderAPIVersion < Build.VERSION_CODES.M) {
                    senderAddress = jsoIn.getString("SenderAddress");
                }
                String receiverAddress = jsoIn.getString("ReceiverAddress");
                msg = jsoIn.getString("Message");
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (content.equals("Text")) {//right "contentType? only text is supposed to be displayed
                    Log.i(LOG_TAG, "Content confirmed");
                    if (confirmSender(senderAddress, address, senderAPIVersion)) {
                        if (confirmReceiver(receiverAddress)) {

                            sendNewMessageInternalMessage(msg, address);
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
     * Confirm the receiver of the message. Since Android 6 it isnt possible anymore to get the own device address. In that case the test is skipped.
     *
     * @param receiverAddress The receivers´ hw address as reported by the message
     * @return true if test passed or android 6
     */
    private boolean confirmReceiver(String receiverAddress) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // WifiInfo.getMacAddress() and the BluetoothAdapter.getAddress() were "removed" in Android 6. They now return a constant value.
            Log.i(LOG_TAG, "Skipped receiver confirmation, because android 6");
            return true;
        } else {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                if (adapter.getAddress().equals(receiverAddress)) {
                    Log.i(LOG_TAG, "Receiver confirmed");
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Confirm the sender of the message. Since Android 6 it isnt possible anymore to get the own device address. In that case the test is skipped.
     *
     * @param senderAddress    The senders´ hw address as reported by the message
     * @param address          The senders´ hw address as reported by the receiving connection
     * @param senderAPIVersion The senders´ API version. If this indicates Android 6 or above, the test is skipped.
     * @return true if test passed or android 6
     */
    private boolean confirmSender(String senderAddress, String address, int senderAPIVersion) {
        if (senderAPIVersion >= Build.VERSION_CODES.M) { //if android 6, return true and ignore check, since the method to get the own hw address was removed
            Log.i(LOG_TAG, "Skipped sender confirmation, because android 6");
            return true;
        } else {
            if (senderAddress.equals(address)) {
                Log.i(LOG_TAG, "Sender confirmed");
                return true;
            }
            return false;
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
            mdvCmd.put("Action", "NewExternalMessage");
            mdvCmd.put("Address", address);
            mdvCmd.put("Sender", address); //sendername
            mdvCmd.put("Timestamp", System.currentTimeMillis() / 1000);
            mdvCmd.put("Message", msg);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendInternalMessageForSPMAServiceConnector(mdvCmd.toString());

    }
    /**
     * Send a message that a local user has been selected, directed at the GUI
     */
    private void sendLocalUserSelectedInternalMessage(User u) {
        JSONObject mdvCmd = new JSONObject();
        try {
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "LocalUserSelected");
            mdvCmd.put("ID", u.getId());
            mdvCmd.put("Name", u.getName());


        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendInternalMessageForSPMAServiceConnector(mdvCmd.toString());

    }

    /**
     * Create a new connection
     *
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
            BluetoothConnection connection = BluetoothConnectionMaker.getInstance().createConnection(device);
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
     */
    private void startListeners(JSONObject msgData) {
        secureListener = BluetoothListenerObserver.getInstance().getSecureListener();
        if (secureListener == null) {
            secureListener = BluetoothListenerMaker.getInstance().createListener(true); //can return null in case BT is off
            if (secureListener != null) {
                secureListener.addObserver(BluetoothListenerObserver.getInstance());
                BluetoothListenerObserver.getInstance().addListener(secureListener);
            }

        }
        insecureListener = BluetoothListenerObserver.getInstance().getInsecureListener();
        if (insecureListener == null) {
            insecureListener = BluetoothListenerMaker.getInstance().createListener(false);//can return null in case BT is off
            if (insecureListener != null) {
                insecureListener.addObserver(BluetoothListenerObserver.getInstance());
                BluetoothListenerObserver.getInstance().addListener(insecureListener);
            }
        }
        if (secureListener != null) {
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
        }
        if (insecureListener != null) {
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
        }

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
        sendInternalMessageForSPMAServiceConnector(mdvCmd.toString());
    }

    /**
     * Stops the listeners
     *
     * @param msgData may contain additional data
     */
    private void stopListeners(JSONObject msgData) {
        BluetoothListenerObserver.getInstance().shutdownAll();
        Log.i(LOG_TAG, "Listeners stopped");

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
                supportedDevices.clear();
                if (btAdapter.isDiscovering()) {
                    Log.w(LOG_TAG, "Discovery running. Cancelling discovery");
                    btAdapter.cancelDiscovery();
                }
                Log.i(LOG_TAG, "Starting discovery");
                if (btAdapter.startDiscovery()) {
                    Log.i(LOG_TAG, "Started discovery");
                    sendClearDevicesInternalMessage();
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
     * No.
     */
    private void makeAmericaGreatAgain() {
        System.exit(1);
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
     * Turns Bluetooth off
     */
    private void turnBluetoothOff() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            Log.i(LOG_TAG, "Turning bluetooth off");
            btAdapter.disable();

        }
    }

    /**
     * Constructor. Gives a name this service. Initializes connection observer
     */
    public SPMAService() {
        super("SPMAService");
        devices = new ArrayList<>(); //initialize device list
        supportedDevices = new ArrayList<>();
        BluetoothConnectionObserver.getInstance().setService(this); //prepare observer for later use
        BluetoothListenerObserver.getInstance().setService(this);//prepare observer for later use
    }

    /**
     * Sends a message to the SPMAServiceConnector
     *
     * @param msg Internal message to be sent
     */
    public void sendInternalMessageForSPMAServiceConnector(String msg) {
        sendInternalMessage(msg, SPMAServiceConnector.ACTION_NEW_MSG);


    }

    /**
     * Sends a new internal message. Despite the name broadcast its actually somewhat directed
     *
     * @param msg                  Message to be send
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

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(broadcastReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, filter);

        filter = new IntentFilter(BluetoothDevice.ACTION_UUID);
        registerReceiver(broadcastReceiver, filter);

        filter = new IntentFilter(SPMAService.ACTION_NEW_MSG);
        registerReceiver(broadcastReceiver, filter);

        BluetoothConnectionMaker.getInstance(getResources()); //prepare BluetoothConnectionMaker for later use
        BluetoothListenerMaker.getInstance(getResources());//prepare BluetoothListenerMaker for later use


        db=new SPMADatabaseAccessHelper(this); //prepare database for use

        startNotification();


    }

    /**
     * Shows a notification, which informs the user about this service running
     */
    private void startNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //Prepare intent
        Intent intent = new Intent(this, MainActivity.class);
        //Wrap into pending intent
        PendingIntent pInten = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //Create Notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.n_icon_test)
                .setContentTitle(getResources().getString(R.string.BGSNotificationTitle))
                .setContentText(getResources().getString(R.string.BGSNotificationText))
                .setOngoing(true);
        //set intent
        notificationBuilder.setContentIntent(pInten);
        //send
        notificationManager.notify(SPMA_NOTIFICATION_ID, notificationBuilder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        endNotification();
    }

    private void endNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(SPMA_NOTIFICATION_ID);
    }

    /**
     * Called when the service is started.
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
