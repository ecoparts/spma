package de.dralle.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG=MainActivity.class.getName();

    private int REQUEST_ENABLE_BT=42;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }
    protected void onStart(){
        super.onStart();
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
