package de.dralle.bluetoothtest.BGS;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import de.dralle.bluetoothtest.GUI.SPMAServiceConnector;

/**
 * Created by nils on 31.05.16.
 */
public class SPMAService extends IntentService {
    public static final String ACTION_NEW_MSG = "SPMAService.ACTION_NEW_MSG";
    private static final String LOG_TAG = SPMAService.class.getName();


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(LOG_TAG, "New device found");
                Log.i(LOG_TAG, device.getAddress());
                //Log.i(LOG_TAG, device.getName());

            }
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.i(LOG_TAG, "New device bonded");
                    Log.i(LOG_TAG, device.getAddress());
                    Log.i(LOG_TAG, device.getName());
                } else {
                    Log.i(LOG_TAG, "Bonding failed");
                }
            }
            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);


                Log.i(LOG_TAG, "Device requesting pairing");
                Log.i(LOG_TAG, device.getAddress());
                Log.i(LOG_TAG, device.getName());


            }
            if (SPMAService.ACTION_NEW_MSG.equals(action)) {
                String msg = intent.getStringExtra("msg");
                Log.i(LOG_TAG, "New message");
                Log.i(LOG_TAG, msg);
                sendMessage("Hello");
                JSONObject msgData = null;
                try {
                    msgData = new JSONObject(msg);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(msgData!=null){
                    if(checkMessage(msgData)){
                        parseMessageForAction(msgData);
                    }
                }else{
                    Log.w(LOG_TAG,"Message not JSON");
                }


            }
        }
    };

    /**
     * Checks the message action attribute and executes the appropriate action
     * @param msgData JSON formatted message to be checked
     */
    private void parseMessageForAction(JSONObject msgData) {
        String action="";
        try {
            action=msgData.getString("Action");
        } catch (JSONException e) {
            e.printStackTrace();

        }
        if(action==null){
            action="";
        }
        switch(action){
            case "MakeVisible":
                makeDeviceVisible();
                break;
            case "TurnOn":
                turnBluetoothOn();
                break;
            default:
                Log.w(LOG_TAG,"Action not recognized: "+action);
                break;
        }
    }

    /**
     * Checks if the message is plausible. Checks the attributes 'Extern' and 'Level'. Extern needs to be false, Level needs to be 0 (for non encrypted, cause not extern)
     * @param msgData JSON formatted message to be checked
     * @return true if valid
     */
    private boolean checkMessage(JSONObject msgData) {
        boolean b= false;
        try {
            b = (!msgData.getBoolean("Extern") && msgData.getInt("Level") == 0);
        }catch (Exception e){

        }
        return b;
    }

    /**
     * Checks if the device is discoverable, and if no requests it
     */
    private void makeDeviceVisible() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            Log.i(LOG_TAG, "Making device discoverable");
            if (btAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent discoverableIntent = new
                        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0); //0 for always visible
                discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(discoverableIntent);
            }else{
                Log.v(LOG_TAG,"Device already visible");
            }

        }
    }
    /**
     * Checks if bluetooth is on, and if no requests permission to turn it on
     */
    private void turnBluetoothOn() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            Log.i(LOG_TAG, "Turning bluetooth on");
            if (btAdapter.isEnabled()) {
                Intent btOnRequest = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                btOnRequest.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //Flag needed when starting from a service
                startActivity(btOnRequest);

            }else{
                Log.v(LOG_TAG,"Bluetooth already on");
            }

        }
    }


    public SPMAService() {
        super("SPMAService");
    }

    public void sendMessage(String msg){
        Intent bgServiceIntent = new Intent(SPMAServiceConnector.ACTION_NEW_MSG);
        bgServiceIntent.putExtra("msg", msg);
        sendBroadcast(bgServiceIntent);


        //parentActivity.startService(bgServiceIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(LOG_TAG, "New work request");
        String data = intent.getStringExtra("msg");
        Log.i(LOG_TAG, "Data: " + data);

    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter);

        filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(broadcastReceiver, filter);

        filter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(broadcastReceiver, filter);

        filter = new IntentFilter(SPMAService.ACTION_NEW_MSG);
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
}
