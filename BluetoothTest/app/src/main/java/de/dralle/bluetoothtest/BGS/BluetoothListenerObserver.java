package de.dralle.bluetoothtest.BGS;

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
public class BluetoothListenerObserver implements Observer {
    private static final String LOG_TAG = BluetoothListenerObserver.class.getName();
    private static BluetoothListenerObserver ourInstance = new BluetoothListenerObserver();
    private SPMAService service=null;

    public SPMAService getService() {
        return service;
    }

    public void setService(SPMAService service) {
        this.service = service;
    }

    public static BluetoothListenerObserver getInstance() {
        return ourInstance;
    }
    private BluetoothListener secureListener=null;
    private BluetoothListener insecureListener=null;

    public BluetoothListener getSecureListener() {
        return secureListener;
    }

    public void setSecureListener(BluetoothListener secureListener) {
        if(this.secureListener!=null&&this.secureListener.isListening()){
            this.secureListener.stopListener();
        }
        this.secureListener = secureListener;
    }

    public BluetoothListener getInsecureListener() {
        return insecureListener;
    }

    public void setInsecureListener(BluetoothListener insecureListener) {
        if(this.insecureListener!=null&&this.insecureListener.isListening()){
            this.insecureListener.stopListener();
        }
        this.insecureListener = insecureListener;
    }

    private BluetoothListenerObserver() {


    }


    @Override
    public void update(Observable observable, Object data) {
        Log.v(LOG_TAG,"Something happened");
        if(observable instanceof BluetoothListener){
            if(data instanceof String){
                Log.v(LOG_TAG,"New string "+data);
                JSONObject jso= null;
                try {
                    jso = new JSONObject((String)data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(jso!=null&&service.checkInternalMessage(jso)){
                    try {
                        if(jso.getString("Action").equals("Shutdown")){
                            if(secureListener!=null){
                                secureListener.stopListener();
                            }
                            secureListener=null;
                            if(insecureListener!=null){
                                insecureListener.stopListener();
                            }

                            insecureListener=null;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    service.parseInternalListenerMessageForAction(jso);
                }

            }
        }
    }

    public void addListener(BluetoothListener lis){
        if(lis.isSecure()){
            setSecureListener(lis);
        }else{
            setInsecureListener(lis);
        }
        Log.i(LOG_TAG,"Listener set");
    }


    public void shutdownAll(){
       if(secureListener!=null){
           secureListener.stopListener();
       }
        secureListener=null;
        if(insecureListener!=null){
            insecureListener.stopListener();
        }
        insecureListener=null;
        Log.i(LOG_TAG,"Disconnect all");
    }
}
