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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import de.dralle.bluetoothtest.BGS.deprecated.BluetoothServerService;
import de.dralle.bluetoothtest.R;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getName();
    private SPMAServiceConnector serviceConnector;
    private final int REQUEST_ENABLE_BT = 2;
    private final int REQUEST_ACCESS_COARSE_LOCATION = 1;

    private ArrayList<String> deviceNames;
    private ArrayAdapter<String> displayAdapter;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (SPMAServiceConnector.ACTION_NEW_MSG.equals(action)) {
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
                        if(serviceConnector.getMessageAction(msgData).equals("NewDevice")){
                            try {
                                deviceNames.add(msgData.getString("Name"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            displayAdapter.notifyDataSetChanged();
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

        serviceConnector=new SPMAServiceConnector(this);
        if(serviceConnector.isServiceRunning()){
            Log.i(LOG_TAG,"Service already running");
        }else{
            Log.v(LOG_TAG,"Service starting");
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

                deviceNames.clear(); //Clear current device name list
                displayAdapter.notifyDataSetChanged();
                //startDeviceScan();

            }
        });

        final Button btnCntrlServer = (Button) findViewById(R.id.ctrlBTserver);
        btnCntrlServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean serviceOnline=serviceConnector.isServiceRunning();
                Log.v(LOG_TAG,"Service is running "+serviceOnline);

               if(serviceOnline){
                   boolean listenersOnline=serviceConnector.areListenersOnline();
                   Log.v(LOG_TAG,"Listeners are "+listenersOnline);
                   if(listenersOnline){
                       serviceConnector.stopListeners();
                   }else{
                       serviceConnector.startListeners();

                   }
               }else{

               }
                /*Button clicked = (Button) v;
                if (clicked.getText().equals(getResources().getString(R.string.startBTserver))) {


                    JSONObject startServerCmd = new JSONObject();
                    try {
                        startServerCmd.put("Extern", false);
                        startServerCmd.put("Level", 0);
                        startServerCmd.put("Action", "StartListen");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        JSONArray srvArray = new JSONArray(new String[]{"insecure", "secure"});
                        startServerCmd.put("Servers", srvArray);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    sendCommandToBackgroundService(startServerCmd.toString());
                    Log.i(LOG_TAG, "Background servers starting");
                    clicked.setText(R.string.stopBTserver);
                } else if (clicked.getText().equals(getResources().getString(R.string.stopBTserver))) {


                    stopServers();
                    Log.i(LOG_TAG, "Background servers stopping");
                    clicked.setText(R.string.startBTserver);
                }*/

            }
        });

        ListView lvDevices = (ListView) findViewById(R.id.listViewDevices);
        lvDevices.setAdapter(displayAdapter);

        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice btDevice = null;
                startNewChatActivity(btDevice);
            }
        });



        IntentFilter filter = new IntentFilter(SPMAServiceConnector.ACTION_NEW_MSG);
        registerReceiver(broadcastReceiver, filter);




    }

    private void stopServers() {
        JSONObject stopServerCmd = new JSONObject();
        try {
            stopServerCmd.put("Extern", false);
            stopServerCmd.put("Level", 0);
            stopServerCmd.put("Action", "StopListen");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendCommandToBackgroundService(stopServerCmd.toString());
    }

    private void sendCommandToBackgroundService(String command) {
        Intent bgServiceIntent = new Intent(this, BluetoothServerService.class);
        bgServiceIntent.putExtra("command", command);
        startService(bgServiceIntent);
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

        serviceConnector.stopService();
        stopServers();
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null && btAdapter.isEnabled()) {
            if (btAdapter.isDiscovering()) {
                Log.w(LOG_TAG, "Discovery running. Cancelling discovery");
                btAdapter.cancelDiscovery();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            //startDeviceScan();
        }
    }
    @Deprecated
    private void startDeviceScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION.toString()) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION.toString()}, REQUEST_ACCESS_COARSE_LOCATION); //need to request permission at runtime for android 6.0+

            }
        }

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            if (btAdapter.isEnabled()) {


                if (btAdapter.isDiscovering()) {
                    Log.w(LOG_TAG, "Discovery running. Cancelling discovery");
                    btAdapter.cancelDiscovery();
                }
                Log.i(LOG_TAG, "Starting discovery");
                if (btAdapter.startDiscovery()) {
                    Log.i(LOG_TAG, "Started discovery");
                    deviceNames.clear();
                    displayAdapter.notifyDataSetChanged();
                } else {
                    Log.i(LOG_TAG, "Failed discovery");
                }
            } else {
                Log.i(LOG_TAG, "Bluetooth disabled");

                Intent btOnRequest = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(btOnRequest, REQUEST_ENABLE_BT);
            }
        } else {
            Log.e(LOG_TAG, "Bluetooth not enabled");
            Toast.makeText(getApplicationContext(), getText(R.string.btNotEnabled), Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
