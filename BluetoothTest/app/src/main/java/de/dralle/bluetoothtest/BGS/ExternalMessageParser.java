package de.dralle.bluetoothtest.BGS;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;

import de.dralle.bluetoothtest.DB.SPMADatabaseAccessHelper;
import de.dralle.bluetoothtest.DB.User;
import de.dralle.bluetoothtest.R;

/**
 * Created by nils on 20.06.16.
 */
public class ExternalMessageParser {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = ExternalMessageParser.class.getName();
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
    /**
     * Send internal messages.
     */
    private InternalMessageSender internalMessageSender = null;
    /**
     * External message sender
     */
    private ExternalMessageSender externalMessageSender = null;

    public ExternalMessageParser(Context con) {
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

    public void setExternalMessageSender(ExternalMessageSender externalMessageSender) {
        this.externalMessageSender = externalMessageSender;
    }

    public void setInternalMessageSender(InternalMessageSender internalMessageSender) {
        this.internalMessageSender = internalMessageSender;
    }

    /**
     * Handle received new external message. Might contain multiple json strings
     *
     * @param extMessage Contains JSON formatted JSON string. might contain multiple JSON strings
     * @return
     */
    public void handleNewExternalMessage(String fromAddress, String extMessage) {
        List<JSONObject> jsoInS = Util.getInstance().splitJSON(extMessage);
        Log.i(LOG_TAG, jsoInS.size() + " messages received at once");
        for (JSONObject jsoIn : jsoInS) {
            try {

                Log.i(LOG_TAG, "Transmitted: " + jsoIn.toString());
                if (jsoIn.getBoolean("Extern")) { //is this an external message?
                    String content = jsoIn.getString("Content");
                    int level = jsoIn.getInt("Level");

                    String senderName = jsoIn.getString("Sender");
                    SPMADatabaseAccessHelper.getInstance(con).updateDeviceFriendlyName(fromAddress, senderName);
                    int senderAPIVersion = jsoIn.getInt("SenderVersionAPI");
                    String senderAppVersion = jsoIn.getString("SenderVersionApp");
                    if (!con.getResources().getString(R.string.app_version).equals(senderAppVersion)) {
                        Log.w(LOG_TAG, "App version mismatch. Things might go horribly wrong. You have been warned");
                    }
                    String senderAddress = null;
                    if (senderAPIVersion < Build.VERSION_CODES.M) {
                        senderAddress = jsoIn.getString("SenderAddress");
                    }
                    String receiverAddress = jsoIn.getString("ReceiverAddress");

                    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

                    if (confirmSender(senderAddress, fromAddress, senderAPIVersion)) {
                        if (confirmReceiver(receiverAddress)) {
                            if (content.equals("Text")) {//right "contentType? only text is supposed to be displayed
                                Log.i(LOG_TAG, "New text");
                                String msg = jsoIn.getString("Message");
                                if (level == 0) {
                                    Log.i(LOG_TAG, "Message is not encrypted");
                                    try {
                                        msg= new String(Base64.decode(msg,Base64.DEFAULT),"utf-8");
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    internalMessageSender.sendNewMessageReceivedMessage(msg, fromAddress);
                                    SPMADatabaseAccessHelper.getInstance(con).addReceivedMessage(fromAddress, msg, localUserManager.getUserId(), false);
                                } else if (level == 1) {
                                    Log.i(LOG_TAG, "Message is AES encrypted");
                                    //decrypt
                                    String deviceKey = SPMADatabaseAccessHelper.getInstance(con).getDeviceAESKey(fromAddress);
                                    if (deviceKey != null) {
                                        String decMsg = enc.decryptWithAES(msg, deviceKey);
                                        if (decMsg != null) {
                                            try {
                                                decMsg= new String(Base64.decode(decMsg,Base64.DEFAULT),"utf-8");
                                            } catch (UnsupportedEncodingException e) {
                                                e.printStackTrace();
                                            }
                                            internalMessageSender.sendNewMessageReceivedMessage(decMsg, fromAddress);
                                            SPMADatabaseAccessHelper.getInstance(con).addReceivedMessage(fromAddress, msg, localUserManager.getUserId(), true);
                                        } else {
                                            Log.i(LOG_TAG, "Decryption failed: Decryption failed");
                                        }
                                    } else {
                                        Log.i(LOG_TAG, "Decryption failed: Key missing");
                                    }
                                }


                            }
                            if (content.equals("DataRequest")) {
                                Log.i(LOG_TAG, "New dataRequest");
                                parseDataRequest(fromAddress, jsoIn);
                            }
                            if (content.equals("DataResponse")) {
                                Log.i(LOG_TAG, "New dataResponce");
                                parseDataResponse(fromAddress, jsoIn);
                            }
                        }
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.w(LOG_TAG, "Couldnt parse message");
            }
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

    private void parseDataRequest(String address, JSONObject msgData) {
        String requestType = null;
        try {
            requestType = msgData.getString("RequestType");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        User u = localUserManager.getUserData();
        switch (requestType) {
            case "Name":
                Log.i(LOG_TAG, "Name has been requested");
                if (u != null) {
                    externalMessageSender.prepareNewExternalDataResponseNoEncryption(address, requestType, u.getName());
                } else {
                    Log.w(LOG_TAG, "Answer for request " + requestType + " couldnt be send");
                }
                break;
            case "RSAPublic":
                Log.i(LOG_TAG, "RSAPublic key has been requested");
                if (u == null) {
                    Log.v(LOG_TAG, "RSA Public key request user is null");
                } else {
                    if (u.getRsaPublic() == null) {
                        Log.v(LOG_TAG, "RSA Public key request key is null");
                    }
                }
                if (u != null && u.getRsaPublic() != null) {
                    externalMessageSender.prepareNewExternalDataResponseNoEncryption(address, requestType, u.getRsaPublic());
                } else {
                    Log.w(LOG_TAG, "Answer for request " + requestType + " couldnt be send");
                }
                break;
            case "AESEncrypted":
                Log.i(LOG_TAG, "AES key has been requested");
                String remoteDeviceRSAPublicKey = SPMADatabaseAccessHelper.getInstance(con).getDeviceRSAPublicKey(address);
                if (u != null && u.getAes() != null) {
                    String myAESKey = u.getAes();
                    String aesEncrypted = enc.encryptWithRSA(myAESKey, remoteDeviceRSAPublicKey);
                    externalMessageSender.prepareNewExternalDataResponse(address, requestType, aesEncrypted, 2); //RSA encrypted
                } else {
                    Log.w(LOG_TAG, "Answer for request " + requestType + " couldnt be send");
                }
            default:
                break;
        }
    }

    private void parseDataResponse(String address, JSONObject msgData) {
        String requestType = null;
        String data = null;
        try {
            requestType = msgData.getString("RequestType");
            data = msgData.getString("Data");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        switch (requestType) {
            case "Name":
                Log.i(LOG_TAG, "Name has been reported by " + address);
                SPMADatabaseAccessHelper.getInstance(con).updateDeviceFriendlyName(address, data);
                break;
            case "RSAPublic":
                Log.i(LOG_TAG, "RSAPublic has been reported by " + address);
                SPMADatabaseAccessHelper.getInstance(con).insertDeviceRSAPublicKey(address, data);
                externalMessageSender.prepareNewExternalDataRequest(address, "AESEncrypted");
                break;
            case "AESEncrypted":
                Log.i(LOG_TAG, "AES key has been reported by " + address);
                int encryptionLevel = 0;
                try {
                    encryptionLevel = msgData.getInt("Level");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (encryptionLevel == 2) { //aes key rsa encrypted for transport
                    User u = localUserManager.getUserData();
                    if (u != null) {
                        String rsaPrivate = u.getRsaPrivate();
                        if (rsaPrivate != null) {
                            String remoteDeviceAesKey = enc.decryptWithRSA(data, rsaPrivate);
                            Log.i(LOG_TAG, "AES key received");
                            SPMADatabaseAccessHelper.getInstance(con).insertDeviceAESKey(address, remoteDeviceAesKey);
                            Log.i(LOG_TAG, "Key exchange finished");
                        }
                    }
                }

                break;
            default:
                break;
        }
    }


}
