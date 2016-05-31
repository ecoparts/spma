package de.dralle.bluetoothtest;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.net.Uri;
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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getName();

    private final int REQUEST_ENABLE_BT = 2;
    private final int REQUEST_ACCESS_COARSE_LOCATION = 1;

    private ArrayList<BluetoothDevice> devices;
    private ArrayList<String> deviceNames;
    private ArrayAdapter<String> displayAdapter;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(LOG_TAG, "New device found");
                Log.i(LOG_TAG, device.getAddress());
                Log.i(LOG_TAG, device.getName());
                devices.add(device);
                deviceNames.add(device.getName());
                displayAdapter.notifyDataSetChanged();
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
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        devices = new ArrayList<>();
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
                startDeviceScan();

            }
        });

        final Button btnCntrlServer=(Button)findViewById(R.id.ctrlBTserver);
        btnCntrlServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button clicked=(Button)v;
                if(clicked.getText().equals(getResources().getString(R.string.startBTserver))){


                    JSONObject startServerCmd=new JSONObject();
                    try {
                        startServerCmd.put("Extern",false);
                        startServerCmd.put("Level",0);
                        startServerCmd.put("Action","StartListen");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        JSONArray srvArray=new JSONArray(new String[]{"insecure","secure"});
                        startServerCmd.put("Servers",srvArray);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Intent btServerIntent=new Intent(MainActivity.this,BluetoothServerService.class);
                    btServerIntent.setData(Uri.parse(startServerCmd.toString()));
                    MainActivity.this.startService(btServerIntent);
                    Log.i(LOG_TAG,"Background servers starting");
                    clicked.setText(R.string.stopBTserver);
                }else if(clicked.getText().equals(getResources().getString(R.string.stopBTserver))){


                    JSONObject stopServerCmd=new JSONObject();
                    try {
                        stopServerCmd.put("Extern",false);
                        stopServerCmd.put("Level",0);
                        stopServerCmd.put("Action","StopListen");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Intent btServerIntent=new Intent(MainActivity.this,BluetoothServerService.class);
                    btServerIntent.setData(Uri.parse(stopServerCmd.toString()));
                    MainActivity.this.startService(btServerIntent);
                    Log.i(LOG_TAG,"Background servers stopping");
                    clicked.setText(R.string.startBTserver);
                }

            }
        });

        ListView lvDevices = (ListView) findViewById(R.id.listViewDevices);
        lvDevices.setAdapter(displayAdapter);

        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice btDevice = devices.get((int) id);
                startNewChatActivity(btDevice);
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter);

        filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(broadcastReceiver, filter);

        filter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(broadcastReceiver, filter);

        makeDeviceVisible();


    }

    private void startNewChatActivity(BluetoothDevice remoteDevice) {
        if(remoteDevice!=null){
            Intent newChatIntent=new Intent(this,ChatActivity.class);
            newChatIntent.putExtra("address",remoteDevice.getAddress());
            startActivity(newChatIntent);
        }

    }

    private void makeDeviceVisible() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            Log.i(LOG_TAG,"Making device discoverable");
            if(btAdapter.getScanMode()!=BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
                Intent discoverableIntent = new
                        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0); //0 for always visible
                startActivity(discoverableIntent);
            }

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
        startDeviceScan();
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
            startDeviceScan();
        }
    }

    private void startDeviceScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION.toString()) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
                    devices.clear();
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
