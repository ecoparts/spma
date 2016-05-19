package de.dralle.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG=MainActivity.class.getName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter btAdapter=BluetoothAdapter.getDefaultAdapter();
        if(btAdapter!=null){

        }else{
            Log.e(LOG_TAG,"Bluetooth not supported");
            Toast.makeText(getApplicationContext(),getText(R.string.btNotSupported),Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
