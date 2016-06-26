package de.dralle.bluetoothtest.BGS;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by Nils on 26.05.2016.
 */
public class BluetoothConnectionObserver implements Observer {
    private static final String LOG_TAG = BluetoothConnectionObserver.class.getName();
    private static BluetoothConnectionObserver ourInstance = new BluetoothConnectionObserver();
    /**
     * for parsing internal messages
     */
    private InternalMessageParser internalMessageParser = null;
    /**
     * Send internal messages.
     */
    private InternalMessageSender internalMessageSender = null;
    /**
     * Help with device handling
     */
    private RemoteBTDeviceManager deviceManager = null;
    private Map<String, BluetoothConnection> btConnectionMap;

    private BluetoothConnectionObserver() {

        btConnectionMap = new HashMap<>();
    }

    public static BluetoothConnectionObserver getInstance() {
        return ourInstance;
    }

    public void setDeviceManager(RemoteBTDeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    public void setInternalMessageSender(InternalMessageSender internalMessageSender) {
        this.internalMessageSender = internalMessageSender;
    }

    public void setInternalMessageParser(InternalMessageParser internalMessageParser) {
        this.internalMessageParser = internalMessageParser;
    }

    public BluetoothConnection getConnection(String address) {
        if (btConnectionMap.containsKey(address)) {
            return btConnectionMap.get(address);
        }
        return null;
    }

    @Override
    public void update(Observable observable, Object data) {
        Log.v(LOG_TAG, "Something happened");
        if (observable instanceof BluetoothConnection) {
            if (data instanceof String) {
                Log.v(LOG_TAG, "New string " + data);
                JSONObject jso = null;
                try {
                    jso = new JSONObject((String) data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (jso != null && internalMessageParser.isInternalMessageValid(jso)) {
                    try {
                        if (jso.getString("Action").equals("Shutdown")) {
                            BluetoothConnection con = (BluetoothConnection) observable;
                            con.close();
                            Log.v(LOG_TAG, "Connection removed " + btConnectionMap.remove(con.getDevice().getAddress()));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    internalMessageParser.parseInternalMessageForAction(jso);
                }

            }
        }
    }

    private void parseMessageForAction(JSONObject jso) {
    }

    public void registerConnection(BluetoothConnection btCon) {
        BluetoothDevice btDevice = btCon.getDevice();
        btConnectionMap.put(btDevice.getAddress(), btCon);
        Log.i(LOG_TAG, "New connection added. " + btDevice.getAddress());
    }

    public void removeConnection(BluetoothConnection btCon) {
        BluetoothDevice btDevice = btCon.getDevice();
        btConnectionMap.remove(btDevice.getAddress());
        Log.i(LOG_TAG, "Connection removed. " + btDevice.getAddress());
    }

    public void disconnectAll() {
        for (String btAdr : btConnectionMap.keySet()) {
            BluetoothConnection btCon = btConnectionMap.get(btAdr);
            if (btCon != null) {
                btCon.close();
            }
        }
        Log.i(LOG_TAG, "Disconnect all");
    }

    /**
     * Handle a internal connection request by performing a lookup on cached connections or creating a new one
     *
     * @param msgData may contain additional data
     * @return
     */
    public void handleInternalConnectionRequest(JSONObject msgData) {
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
                internalMessageSender.sendConnectionReady(msgData);
            } else {
                Log.i(LOG_TAG, "Connection needs to be made");
                makeNewConnection(address);
            }
        }
    }

    /**
     * Create a new connection
     *
     * @param address remote device address
     */
    public void makeNewConnection(String address) {
        BluetoothDevice device = deviceManager.getSupportedDeviceByAddress(address);
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
                internalMessageSender.sendNewConnectionRetrieved(connection);
            } else {
                internalMessageSender.sendNewConnectionFailed(device);
            }

        }

    }
}
