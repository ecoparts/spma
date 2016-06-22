package de.dralle.bluetoothtest.BGS;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import de.dralle.bluetoothtest.DB.SPMADatabaseAccessHelper;
import de.dralle.bluetoothtest.DB.User;

/**
 * Created by nils on 20.06.16.
 */
public class LocalUserManager {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = LocalUserManager.class.getName();
    /**
     * userid of selected user
     */
    private int userId = -1;
    /**
     * Context to be ujsed
     */
    private Context context;
    /**
     * Help with device handling
     */
    private RemoteBTDeviceManager deviceManager = null;
    /**
     *
     */
    private InternalMessageSender internalMessageSender = null;
    /**
     * Manages Bluetooth functions
     */
    private LocalBluetoothManager bluetoothManager = null;

    public LocalUserManager() {

    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setBluetoothManager(LocalBluetoothManager bluetoothManager) {
        this.bluetoothManager = bluetoothManager;
    }

    public void setInternalMessageSender(InternalMessageSender internalMessageSender) {
        this.internalMessageSender = internalMessageSender;
    }

    public void setDeviceManager(RemoteBTDeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    public User getUserData(int userID) {
        return SPMADatabaseAccessHelper.getInstance(context).getUser(userID);
    }

    public User getUserData() {
        return getUserData(userId);
    }

    @Deprecated
    public void requestLocalUserData(JSONObject msgData) {
        int id = -1;
        try {
            id = msgData.getInt("ID");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(LOG_TAG, "Now trying to get the data of user " + id);
        if (id > -1) {
            SPMADatabaseAccessHelper db = SPMADatabaseAccessHelper.getInstance(context);
            User u = null;
            u = db.getUser(id);
            if (u == null) {
                Log.i(LOG_TAG, "No user found. Adding new.");
                u = new User();
                u.setId(id);
                String name = bluetoothManager.getLocalDeviceName();
                if (name != null) {
                    u.setName(name);
                    db.createOrUpdateUser(u);

                } else {
                    Log.w(LOG_TAG, "Looks like BT is not available...");
                }


            }
            internalMessageSender.sendLocalUserSelected(u);
        } else {
            Log.w(LOG_TAG, "Local user selection failed");
        }

    }


    public void changeLocalUserName(JSONObject msgData) {
        String newUserName = null;
        int id = -1;
        try {
            id = msgData.getInt("ID");
            newUserName = msgData.getString("Name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (id > -1) {
            Log.i(LOG_TAG, "Now trying to add new user with name " + newUserName);
            SPMADatabaseAccessHelper db = SPMADatabaseAccessHelper.getInstance(context);
            User u = db.getUser(id);
            if (u == null) {
                u = new User();
            }
            u.setId(id);
            u.setName(newUserName);
            db.createOrUpdateUser(u);
            internalMessageSender.sendLocalUserSelected(u);
        } else {
            Log.w(LOG_TAG, "No id given");
        }


    }

    @Deprecated
    public void addNewLocalUser(JSONObject msgData) {
        String newUserName = null;
        try {
            newUserName = msgData.getString("Name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(LOG_TAG, "Now trying to add new user with name " + newUserName);
        SPMADatabaseAccessHelper.getInstance(context).addUser(newUserName);

    }
}