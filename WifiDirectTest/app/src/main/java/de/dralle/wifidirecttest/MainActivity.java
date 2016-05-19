package de.dralle.wifidirecttest;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wp2pManager=(WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);

        channel= WifiP2pManager.
    }
}
