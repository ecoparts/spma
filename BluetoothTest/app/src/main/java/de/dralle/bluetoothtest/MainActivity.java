package de.dralle.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG=MainActivity.class.getName();

    private int REQUEST_ENABLE_BT=42;
    private ArrayList<BluetoothDevice> devices;
    private ArrayList<String> deviceNames;
    private ArrayAdapter<String> displayAdapter;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(LOG_TAG,"New device found");
                Log.i(LOG_TAG,device.getAddress());
                Log.i(LOG_TAG,device.getName());
                Log.i(LOG_TAG,device.getBluetoothClass().toString());
                Log.i(LOG_TAG,"Bond state: "+device.getBondState()+"");
                Log.i(LOG_TAG,"Type: "+device.getBondState()+"");
                devices.add(device);
                deviceNames.add(device.getName());
                displayAdapter.notifyDataSetChanged();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UUID rid=UUID.randomUUID();
        Log.i(LOG_TAG,rid.toString());

        devices=new ArrayList<>();
        deviceNames=new ArrayList<>();
        displayAdapter=new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNames){
            @Override
            public View getView(int position, View convertView,
                                ViewGroup parent) {
                View view =super.getView(position, convertView, parent);

                TextView textView=(TextView) view.findViewById(android.R.id.text1);

                textView.setTextColor(Color.BLUE);

                return view;
            }
        };

        Button btnScan=(Button)findViewById(R.id.btnScn);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDeviceScan();
            }
        });
        ListView lvDevices=(ListView)findViewById(R.id.listViewDevices);
        lvDevices.setAdapter(displayAdapter);



    }
    protected void onStart(){
        super.onStart();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter); // Don't forget to unregister during onDestroy


        BluetoothAdapter btAdapter=BluetoothAdapter.getDefaultAdapter();
        if(btAdapter!=null){
            if(!btAdapter.isEnabled()){
                Log.e(LOG_TAG,"Bluetooth not enabled");
                Toast.makeText(getApplicationContext(),getText(R.string.btNotEnabled),Toast.LENGTH_LONG).show();

                Intent btOnRequest = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(btOnRequest, REQUEST_ENABLE_BT);
            }else{
                startDeviceScan();
            }
        }else{
            Log.e(LOG_TAG,"Bluetooth not supported");
            Toast.makeText(getApplicationContext(),getText(R.string.btNotSupported),Toast.LENGTH_LONG).show();
            finish();
        }
    }

    protected void onRestart(){
        super.onRestart();
    }

    protected void onResume(){
        super.onResume();
    }

    protected void onPause(){
        super.onPause();
    }

    protected void onStop(){
        super.onStop();
    }

    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);

        BluetoothAdapter btAdapter=BluetoothAdapter.getDefaultAdapter();
        if(btAdapter!=null&&btAdapter.isEnabled()){
            if(btAdapter.isDiscovering()){
                Log.w(LOG_TAG, "Discovery running. Cancelling discovery");
                btAdapter.cancelDiscovery();
            }
        }
    }
    @Override
    protected void onActivityResult (int requestCode,
                                int resultCode,
                                Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQUEST_ENABLE_BT){
            startDeviceScan();
        }
    }
    private void startDeviceScan() {
        BluetoothAdapter btAdapter=BluetoothAdapter.getDefaultAdapter();
        if(btAdapter!=null&&btAdapter.isEnabled()){
            if(btAdapter.isDiscovering()){
                Log.w(LOG_TAG, "Discovery running. Cancelling discovery");
                btAdapter.cancelDiscovery();
            }
            Log.i(LOG_TAG,"Starting discovery");
            if(btAdapter.startDiscovery()){
                Log.i(LOG_TAG,"Started discovery");
                devices.clear();
                deviceNames.clear();
                displayAdapter.notifyDataSetChanged();
            }else{
                Log.i(LOG_TAG,"Failed discovery");
            }
        }else{
            Log.e(LOG_TAG,"Bluetooth not enabled");
            Toast.makeText(getApplicationContext(),getText(R.string.btNotEnabled),Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
