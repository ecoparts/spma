package de.dralle.bluetoothtest.BGS;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Nils on 26.05.2016.
 */
public class BluetoothListener implements Runnable {

    private static final String LOG_TAG = BluetoothListener.class.getName();
    private String uuid="";
    private boolean secure=false;
    private boolean continueListen=false; //continue to listen if new connection is accepted

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }





    private BluetoothServerSocket serverSocket=null;
    public boolean isListening() {
        return continueListen;
    }

    public BluetoothListener(boolean secure, String uuid) {
        this.secure = secure;
        this.uuid = uuid;
        this.continueListen = false;
    }

    @Override
    public void run() {
        continueListen=true;
        Log.i(LOG_TAG,"Bluetooth Listener starting");
        Log.i(LOG_TAG,"Selected UUID: "+uuid);
        Log.i(LOG_TAG,"Use secure mode: "+secure);
        BluetoothAdapter btAdapter=BluetoothAdapter.getDefaultAdapter();
        if(btAdapter!=null){

            if(secure) {
                try {
                    Log.i(LOG_TAG,"Starting listener using secure mode");
                    serverSocket = btAdapter.listenUsingRfcommWithServiceRecord("secure_serversocket", UUID.fromString(uuid));
                } catch (IOException e) {
                    e.printStackTrace();

                }
            }else{
                try {
                    Log.i(LOG_TAG,"Starting listener using insecure mode");
                    serverSocket=btAdapter.listenUsingInsecureRfcommWithServiceRecord("insecure_serversocket", UUID.fromString(uuid));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(serverSocket!=null){
                while (continueListen){
                    BluetoothSocket btSock=null;
                    try {
                        btSock=serverSocket.accept();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(LOG_TAG,"Listener failed");
                        continueListen=false;
                    }
                    if(btSock!=null) {
                        Log.i(LOG_TAG, "New connection");
                        BluetoothConnection btCon = new BluetoothConnection(btSock, secure);
                        btCon.addObserver(BluetoothConnectionObserver.getInstance());
                        BluetoothConnectionObserver.getInstance().registerConnection(btCon);
                        Thread t = new Thread(btCon);
                        if(continueListen){
                            t.start();
                        }
                    }

                }
                Log.w(LOG_TAG,"Listener stopped");

            }else{
                continueListen=false;
                Log.e(LOG_TAG,"Starting listener failed");
            }
        }else{
            continueListen=false;
            Log.w(LOG_TAG,"Couldnt start Listener. No bluetooth");
        }

    }

    /**
     * Stops the listener
     * @return Only true, when the listener was stopped by this
     */
    public boolean stopListener(){
        boolean value=false;
        try {
            if(serverSocket!=null){
                serverSocket.close();
                value=true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            value=false;
        }
        continueListen=false;
        Log.i(LOG_TAG,"listener stopped. Listener was secure: "+secure+". Listener was running: "+value);
        return value;
    }
}
