package de.dralle.bluetoothtest.GUI;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import de.dralle.bluetoothtest.R;

public class ChatActivity extends AppCompatActivity {
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (localBroadcastTag.equals(action)) {
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
                        action=serviceConnector.getMessageAction(msgData);
                        Log.i(LOG_TAG,"New message with action "+action);
                        handleBroadcast(action,msgData);

                    }
                } else {
                    Log.w(LOG_TAG, "Message not JSON");
                }
            }
        }
    };

    private void handleBroadcast(String action, JSONObject msgData) {

        switch(action){
            case "ConnectionShutdown":
                if(mainTextView!=null){
                    mainTextView.setText(mainTextView.getText()+System.getProperty("line.separator")+getResources().getString(R.string.connectionshutdown));
                }
                finish();
                break;
            case "ConnectionReady":
                if(mainTextView!=null){
                    mainTextView.setText(mainTextView.getText()+System.getProperty("line.separator")+getResources().getString(R.string.connectionReady));
                }
                break;
            case "ConnectionRetrieved":
                if(mainTextView!=null){
                    mainTextView.setText(mainTextView.getText()+System.getProperty("line.separator")+getResources().getString(R.string.connectionRetrieved));
                }
                break;
            case "ConnectionFailed":
                if(mainTextView!=null){
                    mainTextView.setText(mainTextView.getText()+System.getProperty("line.separator")+getResources().getString(R.string.connectionFailed));
                }
                break;
            default:
                break;
        }
    }


    private static final String LOG_TAG = ChatActivity.class.getName();
    private TextView mainTextView=null;
    private SPMAServiceConnector serviceConnector;
    public static final String ACTION_NEW_MSG="ChatActivity.ACTION_NEW_MSG";
    private String localBroadcastTag=ACTION_NEW_MSG; //will listen for broadcasts with tag
    private String deviceAddress=null;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(deviceAddress!=null){
            unregisterReceiver(broadcastReceiver);
        }

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mainTextView=(TextView)findViewById(R.id.lblChatHistory);

        serviceConnector=new SPMAServiceConnector(this); //initialize a service connector

        Intent startIntent=getIntent();
        if(startIntent!=null){
            Log.i(LOG_TAG,"Activity started with intent");
            String remoteBTDeviceAddress=startIntent.getStringExtra("address");
            deviceAddress=remoteBTDeviceAddress;

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
        if(deviceAddress!=null){
            localBroadcastTag=ChatActivity.ACTION_NEW_MSG+"_"+deviceAddress;
            IntentFilter filter = new IntentFilter(localBroadcastTag);
            registerReceiver(broadcastReceiver, filter);
        }

    }

    /**
     * Request a new connection
     * @param remoteBTDeviceAddress
     */
    private void requestNewConnection(String remoteBTDeviceAddress) {
        if(serviceConnector.requestNewConnection(remoteBTDeviceAddress)){
            Log.i(LOG_TAG,"Connection requested");
            if(mainTextView!=null){
                mainTextView.setText(mainTextView.getText()+System.getProperty("line.separator")+getResources().getString(R.string.connectionRequested));
            }
        }else{
            Log.w(LOG_TAG,"Connection not requested");
            finish();
        }
    }
}
