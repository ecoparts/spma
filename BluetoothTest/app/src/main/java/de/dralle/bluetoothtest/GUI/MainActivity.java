package de.dralle.bluetoothtest.GUI;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.KeyGenerator;

import de.dralle.bluetoothtest.BGS.Encryption;
import de.dralle.bluetoothtest.R;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getName();
    private SPMAServiceConnector serviceConnector;
    public static final String ACTION_NEW_MSG = "MainActivity.ACTION_NEW_MSG";
    private final int REQUEST_ENABLE_BT = 2;
    private final int REQUEST_ACCESS_COARSE_LOCATION = 1;

    private ArrayList<String> deviceNames;
    private ArrayAdapter<String> displayAdapter;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_NEW_MSG.equals(action)) {
                String msg = intent.getStringExtra("msg");
                Log.i(LOG_TAG, "New message");
                Log.i(LOG_TAG, msg);
                JSONObject msgData = null;
                try {
                    msgData = new JSONObject(msg);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (msgData != null) {
                    if (serviceConnector.checkMessage(msgData)) {
                        if (serviceConnector.getMessageAction(msgData).equals("NewDevice")) {
                            try {
                                deviceNames.add(msgData.getString("Name"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            displayAdapter.notifyDataSetChanged();
                        }
                        if (serviceConnector.getMessageAction(msgData).equals("ClearDevices")) {
                            deviceNames.clear();
                            displayAdapter.notifyDataSetChanged();
                        }
                        if (serviceConnector.getMessageAction(msgData).equals("ConnectionReady")) {
                            String address=null;
                            try {
                                address=msgData.getString("Address");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
                            if(adapter!=null&&address!=null){
                                BluetoothDevice device=adapter.getRemoteDevice(address);
                                startNewChatActivity(device);
                            }
                          
                        }
                    }
                } else {
                    Log.w(LOG_TAG, "Message not JSON");
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //generate encryption keys.
        try {
            //128bit AES key
            Encryption aes = new Encryption(Encryption.newAESkey(128),"AES");
            Log.v("AES Generation", aes.getKey().toString());
            aes.saveAES(aes.getKey(), "aes", getApplicationContext());
            aes.readAES("aes", getApplicationContext());

            //RSA keys

            KeyPair rsaKeys = Encryption.newRSAkeys(1024);
            Encryption rsaPub = new Encryption(rsaKeys.getPublic(), "RSA");
            Encryption rsaPri = new Encryption(rsaKeys.getPrivate(), "RSA");

            rsaPub.saveRSAPublicKey(rsaPub.getKey(),"rsa",getApplicationContext());
            rsaPri.saveRSAPrivateKey(rsaPri.getKey(),"rsa", getApplicationContext());



        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        serviceConnector = new SPMAServiceConnector(this);
        if (serviceConnector.isServiceRunning()) {
            Log.i(LOG_TAG, "Service already running");
        } else {
            Log.v(LOG_TAG, "Service starting");
            serviceConnector.startService();
        }


        deviceNames = new ArrayList<>();
        displayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNames) {
            @Override
            public View getView(int position, View convertView,
                                ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView textView = (TextView) view.findViewById(android.R.id.text1);

                textView.setTextColor(Color.BLUE);

                return view;
            }
        };

        Button btnScan = (Button) findViewById(R.id.btnScn);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serviceConnector.turnBluetoothOn();
                serviceConnector.makeDeviceVisible();
                serviceConnector.scanForNearbyDevices();



            }
        });

        final Button btnCntrlServer = (Button) findViewById(R.id.ctrlBTserver);
        btnCntrlServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean serviceOnline = serviceConnector.isServiceRunning();
                Log.v(LOG_TAG, "Service is running " + serviceOnline);

                if (serviceOnline) {
                    boolean listenersOnline = serviceConnector.areListenersOnline();
                    Log.v(LOG_TAG, "Listeners are " + listenersOnline);
                    if (listenersOnline) {
                        serviceConnector.stopListeners();
                    } else {
                        serviceConnector.startListeners();

                    }
                } else {
                    Log.w(LOG_TAG,"service not online. Starting now");
                    serviceConnector.startService();
                }


            }
        });

        ListView lvDevices = (ListView) findViewById(R.id.listViewDevices);
        lvDevices.setAdapter(displayAdapter);

        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(LOG_TAG,"Clicked device "+id);


                BluetoothDevice btDevice = serviceConnector.getDeviceByIndex((int)id);
                if(btDevice!=null){
                    requestNewConnection(btDevice.getAddress());
                }
            }
        });


        IntentFilter filter = new IntentFilter(ACTION_NEW_MSG);
        registerReceiver(broadcastReceiver, filter);


    }

    /**
     * Request a new connection
     * @param remoteBTDeviceAddress
     */
    private void requestNewConnection(String remoteBTDeviceAddress) {
        if(serviceConnector.requestNewConnection(remoteBTDeviceAddress)){
            Log.i(LOG_TAG,"Connection requested");

        }else{
            Log.w(LOG_TAG,"Connection not requested");
            finish();
        }
    }


    private void startNewChatActivity(BluetoothDevice remoteDevice) {
        if (remoteDevice != null) {
            Intent newChatIntent = new Intent(this, ChatActivity.class);
            newChatIntent.putExtra("address", remoteDevice.getAddress());
            startActivity(newChatIntent);
        }

    }


    protected void onStart() {
        super.onStart();


    }

    protected void onRestart() {
        super.onRestart();
    }

    protected void onResume() {
        super.onResume();
        //startDeviceScan();
    }

    protected void onPause() {
        super.onPause();
    }

    protected void onStop() {
        super.onStop();
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);

        serviceConnector.stopListeners();
        serviceConnector.stopService();

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null && btAdapter.isEnabled()) {
            if (btAdapter.isDiscovering()) {
                Log.w(LOG_TAG, "Discovery running. Cancelling discovery");
                btAdapter.cancelDiscovery();
            }
        }
    }

}
