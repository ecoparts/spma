package de.dralle.bluetoothtest.BGS;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by Nils on 26.05.2016.
 */
public class BluetoothConnectionObserver implements Observer {
    private static final String LOG_TAG = BluetoothConnectionObserver.class.getName();
    private static BluetoothConnectionObserver ourInstance = new BluetoothConnectionObserver();
    private SPMAService service=null;

    public SPMAService getService() {
        return service;
    }

    public void setService(SPMAService service) {
        this.service = service;
    }

    public static BluetoothConnectionObserver getInstance() {
        return ourInstance;
    }
    private Map<String, BluetoothConnection> btConnectionMap;

    private BluetoothConnectionObserver() {

        btConnectionMap=new HashMap<>();
    }
    public BluetoothConnection getConnection(String address){
        if(btConnectionMap.containsKey(address)){
            return btConnectionMap.get(address);
        }
        return null;
    }

    @Override
    public void update(Observable observable, Object data) {
        Log.v(LOG_TAG,"Something happened");
        if(observable instanceof BluetoothConnection){
            if(data instanceof String){
                Log.v(LOG_TAG,"New string "+data);
                JSONObject jso= null;
                try {
                    jso = new JSONObject((String)data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(jso!=null&&service.checkMessage(jso)){
                    try {
                        if(jso.getString("Action").equals("Shutdown")){
                            ((BluetoothConnection)observable).close();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    service.parseMessageForAction(jso);
                }

            }
        }
    }

    private void parseMessageForAction(JSONObject jso) {
    }

    public void registerConnection(BluetoothConnection btCon){
        BluetoothDevice btDevice=btCon.getDevice();
        btConnectionMap.put(btDevice.getAddress(),btCon);
        Log.i(LOG_TAG,"New connection added. "+btDevice.getAddress());
    }
    public void removeConnection(BluetoothConnection btCon){
        BluetoothDevice btDevice=btCon.getDevice();
        btConnectionMap.remove(btDevice.getAddress());
        Log.i(LOG_TAG,"Connection removed. "+btDevice.getAddress());
    }
    public void disconnectAll(){
       for (String btAdr:btConnectionMap.keySet()){
          BluetoothConnection btCon=btConnectionMap.get(btAdr);
           if(btCon!=null){
               btCon.close();
           }
       }
        Log.i(LOG_TAG,"Disconnect all");
    }
}
