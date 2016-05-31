package de.dralle.bluetoothtest.GUI;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import de.dralle.bluetoothtest.R;

public class ChatActivity extends AppCompatActivity {
    private static final String LOG_TAG = ChatActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent startIntent=getIntent();
        if(startIntent!=null){
            Log.i(LOG_TAG,"Activity started with intent");
            String remoteBTDeviceAddress=startIntent.getStringExtra("address");
            if(remoteBTDeviceAddress!=null&&remoteBTDeviceAddress!=""){
                Log.i(LOG_TAG,"Remote device bt address "+remoteBTDeviceAddress);
                requestNewConnection(remoteBTDeviceAddress);
            }else{
                Log.w(LOG_TAG,"No remote device given");
                finish();
            }
        }else{
            Log.e(LOG_TAG,"Activity has no intent");
            finish();
        }
    }

    private void requestNewConnection(String remoteBTDeviceAddress) {

    }
}
