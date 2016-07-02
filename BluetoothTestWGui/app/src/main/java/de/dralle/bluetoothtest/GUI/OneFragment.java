package de.dralle.bluetoothtest.GUI;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Interpolator;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v4.widget.SwipeRefreshLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import de.dralle.bluetoothtest.R;



public class OneFragment extends Fragment {

    private static final String LOG_TAG = OneFragment.class.getName();
    private SPMAServiceConnector serviceConnector;
    public static final String ACTION_NEW_MSG = "OneFragment.ACTION_NEW_MSG";

    private ArrayList<String> deviceNames;
    private ArrayAdapter<String> displayAdapter;

    SwipeRefreshLayout mSwipeRefreshLayout;

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
                        if (serviceConnector.getMessageAction(msgData).equals("NewDevice")) {
                            try {
                                deviceNames.add(msgData.getString("SuperFriendlyName"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            displayAdapter.notifyDataSetChanged();
                        }
                        if (serviceConnector.getMessageAction(msgData).equals("ClearDevices")) {
                            deviceNames.clear();
                            displayAdapter.notifyDataSetChanged();
                        }
                        if (serviceConnector.getMessageAction(msgData).equals("ConnectionReady")) {
                            String address=null;
                            try {
                                address=msgData.getString("Address");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
                            if(adapter!=null&&address!=null){
                                BluetoothDevice device=adapter.getRemoteDevice(address);
                                startNewChatActivity(device);
                            }

                        }
                    }
                } else {
                    Log.w(LOG_TAG, "Message not JSON");
                }
            }
        }
    };

    public OneFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }


    /**
     * Request a new connection
     * @param remoteBTDeviceAddress
     */
    private void requestNewConnection(String remoteBTDeviceAddress) {
        if(serviceConnector.requestNewConnection(remoteBTDeviceAddress)){
            Log.i(LOG_TAG,"Connection requested");

        }else{
            Log.w(LOG_TAG,"Connection not requested");
            //TODO: error message
        }
    }

    private void startNewChatActivity(BluetoothDevice remoteDevice) {
        if (remoteDevice != null) {
            serviceConnector.selectUser(0); //select user 0 (default) every time a new connection is created
            Intent newChatIntent = new Intent(getActivity(), ChatActivity.class);
            newChatIntent.putExtra("address", remoteDevice.getAddress());
            startActivity(newChatIntent);
        }

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_main, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View v= getView();

        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.scan_and_refresh);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                scanAndRefresh();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });


        serviceConnector=SPMAServiceConnector.getInstance(getActivity());

        if (serviceConnector.isServiceRunning()) {
            Log.i(LOG_TAG, "Service already running");
        } else {
            Log.v(LOG_TAG, "Service starting");
            serviceConnector.startService();
        }
        serviceConnector.selectUser(0); //always select user 0, since its the default one

        deviceNames = new ArrayList<>();
        displayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, deviceNames) {
            @Override
            public View getView(int position, View convertView,
                                ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView textView = (TextView) view.findViewById(android.R.id.text1);

                textView.setTextColor(Color.BLUE);

                return view;
            }
        };

        IntentFilter filter = new IntentFilter(ACTION_NEW_MSG);
        getActivity().registerReceiver(broadcastReceiver, filter);
    }

    //@Override
    public boolean onOptionsTemSelected(MenuItem item){
        super.onOptionsItemSelected(item);
        int id = item.getItemId();
        if (id == R.id.scan_and_refresh) {
            scanAndRefresh();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void scanAndRefresh(){

        //------------Scan----------
        serviceConnector.turnBluetoothOn();
        serviceConnector.makeDeviceVisible();
        serviceConnector.scanForNearbyDevices();

        //-----------devices----------------
        ListView lvDevices = (ListView) getView().findViewById(R.id.listViewDevices);
        lvDevices.setAdapter(displayAdapter);

        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(LOG_TAG,"Clicked device "+id);
                BluetoothDevice btDevice = serviceConnector.getDeviceByIndex((int)id);
                if(btDevice!=null){
                    requestNewConnection(btDevice.getAddress());
                }else{
                    Log.w(LOG_TAG,"Clicked device is null");
                }
            }
        });

        //------------Service starten------------
        boolean serviceOnline = serviceConnector.isServiceRunning();
        Log.v(LOG_TAG, "Service is running " + serviceOnline);

        if (serviceOnline) {
            boolean listenersOnline = serviceConnector.areListenersOnline();
            Log.v(LOG_TAG, "Listeners are " + listenersOnline);
            if (listenersOnline) {
                serviceConnector.stopListeners();
            } else {
                serviceConnector.startListeners();
            }
        } else {
            Log.w(LOG_TAG,"service not online. Starting now");
            serviceConnector.startService();
        }

    }

    // TODO: Rename method, update argument and hook method into UI event
    /*public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    *//**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     *//*
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }*/
}
