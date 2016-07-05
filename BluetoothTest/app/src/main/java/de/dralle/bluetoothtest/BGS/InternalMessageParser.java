package de.dralle.bluetoothtest.BGS;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.List;

import de.dralle.bluetoothtest.DB.DeviceDBData;
import de.dralle.bluetoothtest.DB.SPMADatabaseAccessHelper;

/**
 * Created by nils on 20.06.16.
 */
public class InternalMessageParser {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = InternalMessageParser.class.getName();
    /**
     *
     */
    private Context con;
    /**
     * Encryption
     */
    private Encryption enc = null;
    /**
     * External message sender
     */
    private ExternalMessageSender externalMessageSender = null;
    /**
     * Parse external messages
     */
    private ExternalMessageParser externalMessageParser = null;
    /**
     * Selected local user
     */
    private LocalUserManager localUserManager = null;
    /**
     * Access saved devices
     */
    private RemoteBTDeviceManager deviceManager;
    /**
     * Manages Bluetooth functions
     */
    private LocalBluetoothManager bluetoothManager = null;
    /**
     * Class to help with sending internal messages
     */
    private InternalMessageSender internalMessageSender = null;

    public InternalMessageParser(Context con) {
        this.con = con;
    }

    public void setExternalMessageParser(ExternalMessageParser externalMessageParser) {
        this.externalMessageParser = externalMessageParser;
    }

    public void setExternalMessageSender(ExternalMessageSender externalMessageSender) {
        this.externalMessageSender = externalMessageSender;
    }

    public LocalUserManager getLocalUserManager() {
        return localUserManager;
    }

    public void setLocalUserManager(LocalUserManager localUserManager) {
        this.localUserManager = localUserManager;
    }

    public Encryption getEnc() {
        return enc;
    }

    public void setEnc(Encryption enc) {
        this.enc = enc;
    }

    public void setDeviceManager(RemoteBTDeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    public void setBluetoothManager(LocalBluetoothManager bluetoothManager) {
        this.bluetoothManager = bluetoothManager;
    }

    public InternalMessageSender getInternalMessageSender() {
        return internalMessageSender;
    }

    public void setInternalMessageSender(InternalMessageSender internalMessageSender) {
        this.internalMessageSender = internalMessageSender;
    }

    /**
     * Checks if the message is plausible. Checks the attributes 'Extern' and 'Level'. Extern needs to be false, Level needs to be 0 (for non encrypted, cause not extern)
     *
     * @param msgData JSON formatted message to be checked
     * @return true if valid
     */

    public boolean isInternalMessageValid(JSONObject msgData) {
        boolean b = false;
        try {
            b = (!msgData.getBoolean("Extern") && msgData.getInt("Level") == 0);
        } catch (Exception e) {

        }
        return b;
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
                bluetoothManager.makeDeviceVisible(msgData);
                break;
            case "TurnOn":
                bluetoothManager.turnBluetoothOn();
                break;
            case "TurnOff":
                bluetoothManager.turnBluetoothOff();
                break;
            case "Scan":
                if(!bluetoothManager.scanForNearbyDevices(msgData)){
                    internalMessageSender.sendDeviceScanFinished(0);
                }
                break;
            case "ResendCachedDevices":
                List<DeviceDBData> devicesData = deviceManager.getAllCachedDevices();
                for (DeviceDBData dd : devicesData) {
                    internalMessageSender.sendCachedDevice(dd);
                }
                break;
            case "INeedFriends":
                List<DeviceDBData> friends = deviceManager.getAllCachedDevices();
                for (DeviceDBData dd : friends) {
                    if(SPMADatabaseAccessHelper.getInstance(con).isDeviceFriend(dd.getAddress(),localUserManager.getUserId()))
                        internalMessageSender.sendFriendDevice(dd);
                }
                break;
            case "RefreshFriendStatus":
                try {
                    SPMADatabaseAccessHelper.getInstance(con).befriendDevice(msgData.getString("Address"),localUserManager.getUserId(),msgData.getBoolean("Friend"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case "ClearCachedDevices":
                deviceManager.clearCachedDevices();
                break;
            case "ResendCachedConnections":
                Collection<BluetoothConnection> connections = BluetoothConnectionObserver.getInstance().getBtConnections();
                for(BluetoothConnection c: connections){
                    internalMessageSender.sendCachedConnection(c);
                }
                break;
            case "StartListeners":
                BluetoothListenerObserver.getInstance().startListeners(msgData);
                break;
            case "StopListeners":
                BluetoothListenerObserver.getInstance().shutdownAll();
                break;
            case "RequestConnection":
                BluetoothConnectionObserver.getInstance().handleInternalConnectionRequest(msgData);
                break;
            case "Ready":
                internalMessageSender.sendConnectionReady(msgData);
                enc.generateKeys(localUserManager.getUserId(), false);
                String address = "";
                try {
                    address = msgData.getString("Address");
                    externalMessageSender.sendExternalDataRequest(address);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                break;
            case "Shutdown":
                internalMessageSender.sendConnectionShutdown(msgData);
                break;
            case "NewMessage":
                handleNewInternalMessageContainingExternalMessage(msgData);
                break;
            case "SendNewMessage":
                externalMessageSender.prepareNewExternalMessage(msgData);
                break;
            case "AddNewLocalUser":
                localUserManager.addNewLocalUser(msgData);
                break;
            case "ChangeLocalUserName":
                enc.generateKeys(localUserManager.getUserId(), false);
                localUserManager.changeLocalUserName(msgData);
                break;
            case "RequestLocalUser":
                Log.i(LOG_TAG,"Local user requested");
                try {
                    localUserManager.setUserId(msgData.getInt("ID"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                enc.generateKeys(localUserManager.getUserId(), false);
                localUserManager.requestLocalUserData(msgData);
                break;
            case "RegenerateKeys":
                enc.generateKeys(localUserManager.getUserId(), true);
                break;
            default:
                Log.w(LOG_TAG, "Action not recognized: " + action);
                break;
        }
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
                internalMessageSender.sendListenerStart();
                break;
            case "Shutdown":
                internalMessageSender.sendListenerStop();
                break;
            default:
                Log.w(LOG_TAG, "Action not recognized: " + action);
                break;
        }
    }

    /**
     * Handle received new external message. Free it from its JSON container string and check some of the additional message attributes.
     *
     * @param msgData may contain additional data
     * @return
     */
    private void handleNewInternalMessageContainingExternalMessage(JSONObject msgData) {
        Log.i(LOG_TAG, "New external message thing");
        String address = null;
        String msg = "";
        try {
            address = msgData.getString("Address");
            msg = msgData.getString("Message");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SPMADatabaseAccessHelper.getInstance(con).updateDeviceLastSeen(address);
        externalMessageParser.handleNewExternalMessage(address, msg);


    }
}