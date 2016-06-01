package de.dralle.bluetoothtest.BGS.deprecated;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.dralle.bluetoothtest.BGS.BluetoothListener;
import de.dralle.bluetoothtest.R;

/**
 * Created by Nils on 26.05.2016.
 */
public class BluetoothServerService extends IntentService {
    private static final String LOG_TAG = BluetoothServerService.class.getName();
    private List<BluetoothListener> listeners;

    public BluetoothServerService() {
        super("BluetoothServerService");
        listeners = new ArrayList<>();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(LOG_TAG, "New work request");
        String data = intent.getStringExtra("command");
        Log.i(LOG_TAG, "Data: " + data);
        JSONObject requestData = null;
        try {
            requestData = new JSONObject(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (requestData != null) {
            Log.i(LOG_TAG, "Request valid");
            try {
                if (!requestData.getBoolean("Extern")) {
                    Log.i(LOG_TAG, "Request is intern");

                    parseRequestForAction(requestData);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(LOG_TAG, "Request invalid");
        }
    }

    private void parseRequestForAction(JSONObject requestData) throws JSONException {
        String action = (String) requestData.get("Action");
        switch (action) {
            case "StartListen":
                startListeningForConnections(requestData.getJSONArray("Servers"));
                break;
            case "StopListen":
                Log.i(LOG_TAG,"Stopping "+listeners.size()+" listeners");
               for (BluetoothListener l:listeners){
                   l.stopListener();
               }
                break;
            default:
                Log.w(LOG_TAG, "Action " + action + " not recognized");
                break;
        }
    }

    private void startListeningForConnections(JSONArray servers) throws JSONException {
        for (int i = 0; i < servers.length(); i++) {
            Object o = servers.get(i);
            if (o.equals("secure")) {
                BluetoothListener secureListener = new BluetoothListener(true, getResources().getString(R.string.uuid_secure));
                listeners.add(secureListener);
                Thread t = new Thread(secureListener);
                t.start();
                Log.i(LOG_TAG, "Secure listener started");
            }
            if (o.equals("insecure")) {
                BluetoothListener insecureListener = new BluetoothListener(false, getResources().getString(R.string.uuid_insecure));
                listeners.add(insecureListener);
                Thread t = new Thread(insecureListener);
                t.start();
                Log.i(LOG_TAG, "Insecure listener started");

            }
        }
    }
}
