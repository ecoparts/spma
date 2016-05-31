package de.dralle.bluetoothtest.BGS;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by Nils on 26.05.2016.
 */
public class BluetoothConnectionObserver implements Observer {
    private static final String LOG_TAG = BluetoothConnectionObserver.class.getName();
    private static BluetoothConnectionObserver ourInstance = new BluetoothConnectionObserver();

    public static BluetoothConnectionObserver getInstance() {
        return ourInstance;
    }
    private List<BluetoothConnection> btConnections;

    private BluetoothConnectionObserver() {
        btConnections=new ArrayList<>();
    }

    @Override
    public void update(Observable observable, Object data) {

    }

    public void registerConnection(BluetoothConnection btCon){
        btConnections.add(btCon);
    }
    public void removeConnection(BluetoothConnection btCon){
       btConnections.remove(btCon);
    }
    public void disconnectAll(){
       for (BluetoothConnection btCon:btConnections){
           btCon.close();
       }
    }
}
