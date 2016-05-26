package de.dralle.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Created by Nils on 26.05.2016.
 */
public class BluetoothListener implements Runnable {

    private static final String LOG_TAG = BluetoothListener.class.getName();
    private String uuid="";
    private boolean secure=false;
    private boolean continueListen=false; //continue to listen if new connection is accepted

    public boolean isListening() {
        return continueListen;
    }

    public BluetoothListener(boolean secure, String uuid, boolean continueListen) {
        this.secure = secure;
        this.uuid = uuid;
        this.continueListen = continueListen;
    }

    @Override
    public void run() {
        Log.i(LOG_TAG,"Bluetooth Listener starting");
        Log.i(LOG_TAG,"Selected UUID: "+uuid);
        Log.i(LOG_TAG,"Use secure mode: "+secure);
        BluetoothAdapter btAdapter=BluetoothAdapter.getDefaultAdapter();
        if(btAdapter!=null){
            BluetoothServerSocket serverSocket=null;
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
                    }
                    if(btSock!=null){
                        BluetoothConnection btCon=new BluetoothConnection(btSock);
                       Thread t=new Thread(btCon);
                        t.start();
                   //     BluetoothConnectionObserver.getInstance().
                    }else{
                        Log.e(LOG_TAG,"Listener failed");
                        continueListen=false;
                    }
                }
                Log.w(LOG_TAG,"Listener stopped");

            }else{
                Log.e(LOG_TAG,"Starting listener failed");
            }
        }

    }
    public void stopListener(){
        continueListen=false;
    }
}
