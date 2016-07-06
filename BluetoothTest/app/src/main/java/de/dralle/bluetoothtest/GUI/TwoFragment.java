package de.dralle.bluetoothtest.GUI;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import de.dralle.bluetoothtest.GUI.ToStringAdapters.Connection;
import de.dralle.bluetoothtest.GUI.ToStringAdapters.Device;
import de.dralle.bluetoothtest.R;

/**
 * Created by Niklas on 18.06.2016.
 */
public class TwoFragment extends Fragment {

    private static final String LOG_TAG = TwoFragment.class.getName();
    private SPMAServiceConnector serviceConnector;
    public static final String ACTION_NEW_MSG = "TwoFragment.ACTION_NEW_MSG";

    private ArrayList<Device> friends;
    private ArrayAdapter<Device> displayAdapter;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_NEW_MSG.equals(action)) {
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
                        switch (serviceConnector.getMessageAction(msgData)) {
                            case "FriendDevice":
                                Device d=new Device();
                                try {

                                        d.setDeviceName(msgData.getString("SuperFriendlyName"));
                                        d.setDeviceAddress(msgData.getString("Address"));
                                        d.setLastSeen(msgData.getInt("LastSeen"));
                                        d.setPaired(msgData.getBoolean("Paired"));
                                        friends.add(d);
                                        Log.i(LOG_TAG,"Friend added "+d);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                displayAdapter.notifyDataSetChanged();
                                Log.v(LOG_TAG,"Friend added "+"Dataset changed");
                                break;
                            default:
                                break;
                        }
                    }
                } else {
                    Log.w(LOG_TAG, "Message not JSON");
                }
            }
        }
    };

    public TwoFragment() {

    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(ACTION_NEW_MSG);
        getActivity().registerReceiver(broadcastReceiver, filter);

        friends.clear();
        displayAdapter.notifyDataSetChanged();
        serviceConnector.requestFriends();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_two, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View v = getView();


        serviceConnector = SPMAServiceConnector.getInstance(getActivity());

        if (serviceConnector.isServiceRunning()) {
            Log.i(LOG_TAG, "Service already running");
        } else {
            Log.v(LOG_TAG, "Service starting");
            serviceConnector.startService();
        }
        serviceConnector.selectUser(0); //always select user 0, since its the default one

        friends = new ArrayList<>();
        displayAdapter = new ArrayAdapter<Device>(getActivity(), android.R.layout.simple_list_item_1, friends) {
            @Override
            public View getView(int position, View convertView,
                                ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView textView = (TextView) view.findViewById(android.R.id.text1);

                textView.setTextColor(Color.BLUE);

                return view;
            }
        };

        //cached connections
        ListView lvDevices = (ListView) getView().findViewById(R.id.listViewFriendDevices);
        lvDevices.setAdapter(displayAdapter);

        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Device d=friends.get((int) id);
                if(d!=null){
                    serviceConnector.requestNewConnection(d.getDeviceAddress());
                }

            }
        });


    }


}
