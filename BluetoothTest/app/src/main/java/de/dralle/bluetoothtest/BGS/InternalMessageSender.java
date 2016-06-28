package de.dralle.bluetoothtest.BGS;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import de.dralle.bluetoothtest.DB.DeviceDBData;
import de.dralle.bluetoothtest.DB.SPMADatabaseAccessHelper;
import de.dralle.bluetoothtest.DB.User;
import de.dralle.bluetoothtest.GUI.SPMAServiceConnector;

/**
 * Created by nils on 20.06.16.
 */
public class InternalMessageSender {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = InternalMessageSender.class.getName();
    /**
     *
     */
    private Context con;

    /**
     * Access saved devices
     */
    private RemoteBTDeviceManager deviceManager;

    public InternalMessageSender(Context con) {
        this.con = con;
    }

    public void setDeviceManager(RemoteBTDeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    /**
     *
     */
    private JSONObject getMessageFrame() {
        JSONObject jsoOut = new JSONObject();
        try {
            jsoOut.put("Extern", false);//internal message
            jsoOut.put("Level", 0);//not encrypted

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsoOut;
    }

    /**
     * Send a message that a new device was recovered from cache
     *
     * @param device newly recovered device
     */
    public void sendSupportedDevice(DeviceDBData device) {
        JSONObject mdvCmd = getMessageFrame();
        try {

            mdvCmd.put("Action", "NewDevice");
            mdvCmd.put("Name", device.getDeviceName());
            mdvCmd.put("SuperFriendlyName", device.getFriendlyName());
            mdvCmd.put("Address", device.getAddress());
            boolean bonded = device.isPaired();
            mdvCmd.put("Paired", bonded);
            mdvCmd.put("LastSeen", device.getLastSeen());
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
    public void sendNewConnectionRetrieved(BluetoothConnection con) {
        JSONObject mdvCmd = getMessageFrame();
        try {

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
    public void sendNewConnectionFailed(BluetoothDevice device) {
        JSONObject mdvCmd = getMessageFrame();
        try {

            mdvCmd.put("Action", "ConnectionFailed");
            mdvCmd.put("Address", device.getAddress());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendInternalMessageForSPMAServiceConnector(mdvCmd.toString());


    }

    /**
     * Send a message that the GUI should clear its device list
     */
    public void sendClearDevices() {
        JSONObject mdvCmd = getMessageFrame();
        try {

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
    @Deprecated
    public void sendNewDeviceFound(BluetoothDevice device) {
        JSONObject mdvCmd = getMessageFrame();
        try {

            mdvCmd.put("Action", "NewDevice");
            mdvCmd.put("Name", device.getName());
            mdvCmd.put("SuperFriendlyName", SPMADatabaseAccessHelper.getInstance(con).getDeviceFriendlyName(device.getAddress()));
            mdvCmd.put("Address", device.getAddress());
            boolean bonded = device.getBondState() == BluetoothDevice.BOND_BONDED;
            mdvCmd.put("Paired", bonded);
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
    public void sendConnectionShutdown(JSONObject msgData) {
        JSONObject mdvCmd = getMessageFrame();
        String address = "";
        try {
            address = msgData.getString("Address");
            mdvCmd.put("Action", "ConnectionShutdown");
            mdvCmd.put("Address", address);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendInternalMessageForSPMAServiceConnector(mdvCmd.toString());
    }


    /**
     * Send a message that Listeners stopped
     */
    public void sendListenerStop() {
        JSONObject mdvCmd = getMessageFrame();
        try {

            mdvCmd.put("Action", "ListenersStopped");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendInternalMessageForSPMAServiceConnector(mdvCmd.toString());
    }

    /**
     * Send a message that Listeners started
     */
    public void sendListenerStart() {
        JSONObject mdvCmd = getMessageFrame();
        try {
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
    public void sendConnectionReady(JSONObject msgData) {
        JSONObject mdvCmd = getMessageFrame();
        String address = "";
        try {
            address = msgData.getString("Address");
            mdvCmd.put("Action", "ConnectionReady");
            mdvCmd.put("Address", address);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendInternalMessageForSPMAServiceConnector(mdvCmd.toString());


    }

    public void sendNewSupportedDeviceList() {
        sendClearDevices();
        List<DeviceDBData> allDevices = deviceManager.getAllCachedDevices();
        for (DeviceDBData device : allDevices)
            sendSupportedDevice(device);
    }

    /**
     * Send a message that a new external message has arrived, directed at the GUI
     */
    public void sendNewMessageReceivedMessage(String msg, String address) {
        JSONObject mdvCmd = new JSONObject();
        try {
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "NewExternalMessage");
            mdvCmd.put("Address", address);
            DeviceDBData device = deviceManager.getCachedDevice(address);
            if (device != null) {
                mdvCmd.put("Sender", device.getFriendlyName());
            } else {
                mdvCmd.put("Sender", address);
            }

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
    public void sendLocalUserSelected(User u) {
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
     * Sends a message to the SPMAServiceConnector
     *
     * @param msg Internal message to be sent
     */
    private void sendInternalMessageForSPMAServiceConnector(String msg) {
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
        con.sendBroadcast(bgServiceIntent);
        Log.v(LOG_TAG, "Send new broadcast to " + receiverBroadcastTag);


    }


    /**
     * Send a message that the device scan has finished
     */
    public void sendDeviceScanFinished(int cntResults) {
        JSONObject mdvCmd = new JSONObject();
        try {
            mdvCmd.put("Extern", false);
            mdvCmd.put("Level", 0);
            mdvCmd.put("Action", "ScanFinished");
            mdvCmd.put("CntResults", cntResults);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendInternalMessageForSPMAServiceConnector(mdvCmd.toString());

    }
}
