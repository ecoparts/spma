package de.dralle.bluetoothtest.BGS;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * Created by Nils on 26.05.2016.
 */
public class BluetoothConnection extends Observable implements Runnable{
    private static final String LOG_TAG = BluetoothConnection.class.getName();

    private BluetoothSocket socket;
    private BluetoothDevice device;
    private BufferedInputStream in=null;
    private BufferedOutputStream out=null;
    private boolean active=false;

    public boolean isActive() {
        return active;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public boolean isSecureConnection() {
        return secureConnection;
    }

    public BluetoothConnection(BluetoothSocket socket, boolean secureConnection) {
        this.socket = socket;
        this.device = socket.getRemoteDevice();
        this.secureConnection = secureConnection;
    }

    private boolean secureConnection=false;
    @Override
    public void run() {
        Log.i(LOG_TAG,"Starting new connection thread to "+device.getAddress());
        if(!socket.isConnected()){
            try {
                socket.connect();
                active=true;
            } catch (IOException e) {
                e.printStackTrace();
                active=false;
                Log.w(LOG_TAG,"Connection to "+device.getAddress()+" failed"+countObservers());

            }

        }else{
            active=true;
        }
        Log.i(LOG_TAG,"Socket connected");
        if(socket!=null&&active){
            try {
                in=new BufferedInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                active=false;
            }
            try {
                out=new BufferedOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                active=false;
            }
            if(in!=null&&out!=null){
                Log.i(LOG_TAG,"Input and outputstreams retrieved");
                notifyObserversAboutConnectionReady();
                while(active){
                    List<Byte> msgBytes=new ArrayList<>();
                    try {
                        while (in.available()>0) {
                            int received = -1;
                            try {
                                received = in.read();
                            } catch (IOException e) {
                                e.printStackTrace();
                                active=false;
                            }
                            if (received != -1){
                                msgBytes.add((byte)received);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        active=false;
                    }
                    if(msgBytes.size()>0){
                            byte[] msgBytesForRealThisTime=new byte[msgBytes.size()]; //TODO: rename

                        for(int i=0;i<msgBytes.size();i++){
                            msgBytesForRealThisTime[i]=msgBytes.get(i);
                        }
                        String message=new String(msgBytesForRealThisTime);
                        Log.i(LOG_TAG,"New message"+message);
                        notifyObserversAboutNewMessage(message);
                    }else{
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        notifyObserversAboutShutdown();
        Log.i(LOG_TAG,"Shutting down");


    }
    public void sendExternalMessage(String msg){
        Log.i(LOG_TAG,"Writing message");
        if(active){
            char[] msgChars=msg.toCharArray();
            for(int i=0;i<msgChars.length;i++){
                try {
                    out.write((int)msgChars[i]);
                } catch (IOException e) {
                    e.printStackTrace();
                    active=false;
                }
            }
        }else{
            Log.i(LOG_TAG,"Write failed");
            notifyObserversAboutShutdown();
        }

    }

    private void notifyObserversAboutShutdown() {
        JSONObject jso=new JSONObject();
        try {
        jso.put("Extern",false);
        jso.put("Level",0);
jso.put("Address",device.getAddress());
            jso.put("Action","Shutdown");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        setChanged();
        notifyObservers(jso.toString());
    }
    private void notifyObserversAboutConnectionReady() {
        JSONObject jso=new JSONObject();
        try {
            jso.put("Extern",false);
            jso.put("Level",0);
            jso.put("Address",device.getAddress());
            jso.put("Action","Ready");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        setChanged();
        notifyObservers(jso.toString());
    }
    private void notifyObserversAboutNewMessage(String msg) {
        JSONObject jso=new JSONObject();
        try {
            jso.put("Extern",false);
            jso.put("Level",0);
            jso.put("Address",device.getAddress());
            jso.put("Action","NewMessage");
            jso.put("Message",msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        setChanged();
        notifyObservers(jso.toString());
    }

    public void close() {
        try {
            socket.close();
            Log.i(LOG_TAG,"Socket closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket=null;
        active=false;
        Log.i(LOG_TAG,"Socket removed");
    }
}
