package de.dralle.bluetoothtest.BGS;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Observable;
import java.util.UUID;

/**
 * Created by Nils on 26.05.2016.
 */
public class BluetoothListener extends Observable implements Runnable {

    private static final String LOG_TAG = BluetoothListener.class.getName();
    private boolean continueListen = false; //continue to listen if new connection is accepted
    private boolean secure=false;

    public boolean isSecure() {
        return secure;
    }

    public BluetoothListener(boolean secure, BluetoothServerSocket serverSocket) {
        this.secure = secure;

        this.serverSocket = serverSocket;
    }

    private BluetoothServerSocket serverSocket = null;

    public boolean isListening() {
        return continueListen;
    }



    @Override
    public void run() {
        continueListen = true;
        Log.i(LOG_TAG, "Bluetooth Listener starting");
        notifyObserversAboutStartup();

        if (serverSocket != null) {
            while (continueListen) {
                BluetoothSocket btSock = null;
                try {
                    btSock = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, "Listener failed");
                    continueListen = false;
                }
                if (btSock != null) {
                    Log.i(LOG_TAG, "New connection");
                    notifyObserversAboutNewConnectionAccepted();
                    BluetoothConnection btCon = new BluetoothConnection(btSock, secure);
                    btCon.addObserver(BluetoothConnectionObserver.getInstance());
                    BluetoothConnectionObserver.getInstance().registerConnection(btCon);
                    Thread t = new Thread(btCon);
                    if (continueListen) {
                        t.start();
                    }
                }

            }
            Log.w(LOG_TAG, "Listener stopped");

        } else {
            continueListen = false;
            Log.e(LOG_TAG, "Starting listener failed");
        }
        notifyObserversAboutShutdown();


    }

    private void notifyObserversAboutNewConnectionAccepted() {
        JSONObject jso=new JSONObject();
        try {
            jso.put("Extern",false);
            jso.put("Level",0);
            jso.put("Action","NewConnectionAccepted");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        setChanged();
        notifyObservers(jso.toString());
    }

    private void notifyObserversAboutStartup() {
        JSONObject jso=new JSONObject();
        try {
            jso.put("Extern",false);
            jso.put("Level",0);
            jso.put("Action","Startup");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        setChanged();
        notifyObservers(jso.toString());
    }

    private void notifyObserversAboutShutdown() {
        JSONObject jso=new JSONObject();
        try {
            jso.put("Extern",false);
            jso.put("Level",0);
            jso.put("Action","Shutdown");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        setChanged();
        notifyObservers(jso.toString());
    }

    /**
     * Stops the listener
     *
     * @return Only true, when the listener was stopped by this
     */
    public boolean stopListener() {
        boolean value = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
                value = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            value = false;
        }
        continueListen = false;
        Log.i(LOG_TAG, "listener stopped. Listener was secure: " + secure + ". Listener was running: " + value);
        return value;
    }
}
