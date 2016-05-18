package de.dralle.blechattest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static String LOG_TAG=MainActivity.class.getName();

    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private final AdvertiseCallback adCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(LOG_TAG, "Started advertising at " + settingsInEffect.getTxPowerLevel() + "dBm.\n");
        }

        @Override
        public void onStartFailure(int errorCode) {
            switch (errorCode) {
                case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                    Log.w(LOG_TAG, "Advertise failed: already started.\n");
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                    Log.w(LOG_TAG, "Advertise failed: data too large.\n");
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    Log.w(LOG_TAG, "Advertise failed: feature unsupported.\n");
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                    Log.w(LOG_TAG, "Advertise failed: internal error.\n");
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    Log.w(LOG_TAG, "Advertise failed: too many advertisers.\n");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button btnSend=(Button)findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText edtSend=(EditText)findViewById(R.id.edtSend);
                TextView lblChatView=(TextView)findViewById(R.id.lblChatView);

                CharSequence curText=lblChatView.getText();
                CharSequence addText=edtSend.getText();
                CharSequence newText=curText+System.getProperty("line.separator")+addText;

                edtSend.setText("");
                lblChatView.setText(newText);

                // Choose advertise settings for long range, but infrequent advertising.
                AdvertiseSettings.Builder settings = new AdvertiseSettings.Builder();
                settings.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
                settings.setConnectable(false); // We are not handling connections.
                settings.setTimeout(100);
                settings.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH); // Long range.

                AdvertiseData.Builder data = new AdvertiseData.Builder();
                data.addServiceUuid(new ParcelUuid(UUID.fromString(getResources().getString(R.string.uuid))));
                data.setIncludeDeviceName(false);
                data.setIncludeTxPowerLevel(true);

                BluetoothLeAdvertiser bleAdvertiser = btAdapter.getBluetoothLeAdvertiser();
                if(bleAdvertiser==null){
                    Log.e(LOG_TAG,"BLE Advertiser not available");
                    finish();
                }

                bleAdvertiser.startAdvertising(settings.build(),data.build(),adCallback);



            }
        });

        btManager=(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter=btManager.getAdapter();
        if(btAdapter==null||!btAdapter.isEnabled()){
            Log.e(LOG_TAG,"Bluetooth not available");
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
