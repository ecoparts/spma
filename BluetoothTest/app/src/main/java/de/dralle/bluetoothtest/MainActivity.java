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
    @Override
    protected void onActivityResult (int requestCode,
                                int resultCode,
                                Intent data){
        if(requestCode==REQUEST_ENABLE_BT){
            startDeviceScan();
        }
    }
    private void startDeviceScan() {
        BluetoothAdapter btAdapter=BluetoothAdapter.getDefaultAdapter();
        if(btAdapter!=null&&btAdapter.isEnabled()){

        }else{
            Log.e(LOG_TAG,"Bluetooth not enabled");
            Toast.makeText(getApplicationContext(),getText(R.string.btNotEnabled),Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
