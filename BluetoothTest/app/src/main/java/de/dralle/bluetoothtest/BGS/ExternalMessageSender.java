package de.dralle.bluetoothtest.BGS;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import de.dralle.bluetoothtest.DB.DeviceDBData;
import de.dralle.bluetoothtest.DB.SPMADatabaseAccessHelper;
import de.dralle.bluetoothtest.DB.User;
import de.dralle.bluetoothtest.R;

/**
 * Created by nils on 20.06.16.
 */
public class ExternalMessageSender {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = ExternalMessageSender.class.getName();
    /**
     *
     */
    private Context con;
    /**
     * Selected local user
     */
    private LocalUserManager localUserManager = null;
    /**
     * Encryption
     */
    private Encryption enc = null;
    /**
     * Access saved devices
     */
    private RemoteBTDeviceManager deviceManager;

    public ExternalMessageSender(Context con) {
        this.con = con;
    }

    public void setLocalUserManager(LocalUserManager localUserManager) {
        this.localUserManager = localUserManager;
    }

    public void setEnc(Encryption enc) {
        this.enc = enc;
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
            jsoOut.put("Extern", true);//external message


        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsoOut;
    }

    /**
     * Prepare a new external data request and wrap it into a JSON string
     *
     * @param
     * @return
     */
    public void prepareNewExternalDataRequest(String address, String requestType) {

        User sender = localUserManager.getUserData();

        Log.i(LOG_TAG, "Sending new " + requestType + " data request to " + address);

        BluetoothConnectionObserver bco = BluetoothConnectionObserver.getInstance();
        BluetoothConnection connection = bco.getConnection(address);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && connection != null) {
            JSONObject jsoOut = getMessageFrame();
            try {
                jsoOut.put("Level", 0);
                jsoOut.put("Content", "DataRequest");
                DeviceDBData device = deviceManager.getCachedDevice(address);
                if (device != null) {
                    jsoOut.put("Receiver", device.getFriendlyName());
                }

                if (sender != null) {
                    jsoOut.put("Sender", sender.getName());//TODO: investigate
                } else {
                    jsoOut.put("Sender", adapter.getName());
                }
                jsoOut.put("ReceiverAddress", address);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    jsoOut.put("SenderAddress", adapter.getAddress());
                }

                jsoOut.put("SenderVersionAPI", Build.VERSION.SDK_INT);
                jsoOut.put("SenderVersionApp", con.getResources().getString(R.string.app_version));
                jsoOut.put("Timestamp", System.currentTimeMillis() / 1000);

                jsoOut.put("Secure", connection.isSecureConnection());
                jsoOut.put("RequestType", requestType);


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

    /**
     * Prepare a new external data response and wrap it into a JSON string
     *
     * @param
     * @return
     */
    public void prepareNewExternalDataResponseNoEncryption(String address, String requestType, String data) {
        prepareNewExternalDataResponse(address, requestType, data, 0);
    }

    /**
     * Prepare a new external data response and wrap it into a JSON string
     *
     * @param
     * @return
     */
    public void prepareNewExternalDataResponse(String address, String requestType, String data, int encryptionLevel) {


        User sender = localUserManager.getUserData();

        Log.i(LOG_TAG, "Sending new " + requestType + " data response to " + address + " with data " + data);

        BluetoothConnectionObserver bco = BluetoothConnectionObserver.getInstance();
        BluetoothConnection connection = bco.getConnection(address);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            JSONObject jsoOut = getMessageFrame();
            try {
                jsoOut.put("Level", encryptionLevel);
                jsoOut.put("Content", "DataResponse");
                DeviceDBData device = deviceManager.getCachedDevice(address);
                if (device != null) {
                    jsoOut.put("Receiver", device.getFriendlyName());
                }
                if (sender != null) {
                    jsoOut.put("Sender", sender.getName());//TODO: investigate
                } else {
                    jsoOut.put("Sender", adapter.getName());
                }
                jsoOut.put("ReceiverAddress", address);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    jsoOut.put("SenderAddress", adapter.getAddress());
                }

                jsoOut.put("SenderVersionAPI", Build.VERSION.SDK_INT);
                jsoOut.put("SenderVersionApp", con.getResources().getString(R.string.app_version));
                jsoOut.put("Timestamp", System.currentTimeMillis() / 1000);

                jsoOut.put("Secure", connection.isSecureConnection());
                jsoOut.put("RequestType", requestType);
                jsoOut.put("Data", data);


            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (connection != null) {
                connection.sendExternalMessage(jsoOut.toString());
                Log.i(LOG_TAG, "External data request send");
            } else {
                Log.i(LOG_TAG, "No suitable connection found");

            }
        } else {
            Log.w(LOG_TAG, "No bluetooth. Cant send.");
        }


    }

    public void sendExternalDataRequest(String address) {
        prepareNewExternalDataRequest(address, "Name");
        prepareNewExternalDataRequest(address, "RSAPublic");
    }

    /**
     * Prepare a new external message and wrap it into a JSON string
     *
     * @param msgData may contain additional data
     * @return
     */
    public void prepareNewExternalMessage(JSONObject msgData) {
        String address = null;
        String msg = "";
        int senderID = -1;
        try {
            address = msgData.getString("Address");
            msg = msgData.getString("Message");
            senderID = msgData.getInt("SenderID");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        int encryptionLevel = 0;
        User sender = SPMADatabaseAccessHelper.getInstance(con).getUser(senderID);
        if (address != null) {
            if (sender != null) {
                if (sender.getAes() != null) {
                    String encMsg = enc.encryptWithAES(msg, sender.getAes());
                    if (encMsg != null) {
                        msg = encMsg;
                        encryptionLevel = 1;
                    } else {
                        Log.w(LOG_TAG, "Encryption failed");
                    }
                }
            }
            Log.i(LOG_TAG, "Sending new message to " + address);

            BluetoothConnectionObserver bco = BluetoothConnectionObserver.getInstance();
            BluetoothConnection connection = bco.getConnection(address);

            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                JSONObject jsoOut = getMessageFrame();
                try {
                    jsoOut.put("Level", encryptionLevel);
                    jsoOut.put("Content", "Text");
                    DeviceDBData device = deviceManager.getCachedDevice(address);
                    if (device != null) {
                        jsoOut.put("Receiver", device.getFriendlyName());
                    }
                    if (sender != null) {
                        jsoOut.put("Sender", sender.getName());
                    } else {
                        jsoOut.put("Sender", adapter.getName());
                    }
                    jsoOut.put("ReceiverAddress", address);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        jsoOut.put("SenderAddress", adapter.getAddress());
                    }

                    jsoOut.put("SenderVersionAPI", Build.VERSION.SDK_INT);
                    jsoOut.put("SenderVersionApp", con.getResources().getString(R.string.app_version));
                    jsoOut.put("Timestamp", System.currentTimeMillis() / 1000);

                    jsoOut.put("Secure", connection.isSecureConnection());
                    jsoOut.put("Message", msg);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (connection != null) {
                    connection.sendExternalMessage(jsoOut.toString());
                    SPMADatabaseAccessHelper.getInstance(con).addSendMessage(address, msg, senderID, encryptionLevel == 1);
                } else {
                    Log.i(LOG_TAG, "No suitable connection found");

                }
            } else {
                Log.w(LOG_TAG, "No bluetooth. Cant send.");
            }


        }
    }

    public JSONObject enrichData(JSONObject original, int encryptionLevel, String contentType, User sender, DeviceDBData remoteDevice, BluetoothConnection connection) {
        try {
            original.put("Level", encryptionLevel);

            original.put("Content", contentType);
            if (remoteDevice != null) {
                original.put("Receiver", remoteDevice.getFriendlyName());
            }

            original.put("Sender", sender.getName());

            original.put("ReceiverAddress", remoteDevice.getAddress());
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                original.put("SenderAddress", localUserManager.getSenderAddress());
            }

            original.put("SenderVersionAPI", Build.VERSION.SDK_INT);
            original.put("SenderVersionApp", con.getResources().getString(R.string.app_version));
            original.put("Timestamp", System.currentTimeMillis() / 1000);

            original.put("Secure", connection.isSecureConnection());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return original;
    }


}
