package de.dralle.bluetoothtest.BGS;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by Nils on 26.05.2016.
 */
public class BluetoothListenerObserver implements Observer {
    private static final String LOG_TAG = BluetoothListenerObserver.class.getName();
    private static BluetoothListenerObserver ourInstance = new BluetoothListenerObserver();
    /**
     * for parsing internal messages
     */
    private InternalMessageParser internalMessageParser = null;
    private BluetoothListener secureListener = null;
    private BluetoothListener insecureListener = null;

    private BluetoothListenerObserver() {


    }

    public static BluetoothListenerObserver getInstance() {
        return ourInstance;
    }

    public void setInternalMessageParser(InternalMessageParser internalMessageParser) {
        this.internalMessageParser = internalMessageParser;
    }

    public BluetoothListener getSecureListener() {
        return secureListener;
    }

    public void setSecureListener(BluetoothListener secureListener) {
        if (this.secureListener != null && this.secureListener.isListening()) {
            this.secureListener.stopListener();
        }
        this.secureListener = secureListener;
    }

    public BluetoothListener getInsecureListener() {
        return insecureListener;
    }

    public void setInsecureListener(BluetoothListener insecureListener) {
        if (this.insecureListener != null && this.insecureListener.isListening()) {
            this.insecureListener.stopListener();
        }
        this.insecureListener = insecureListener;
    }

    @Override
    public void update(Observable observable, Object data) {
        Log.v(LOG_TAG, "Something happened");
        if (observable instanceof BluetoothListener) {
            if (data instanceof String) {
                Log.v(LOG_TAG, "New string " + data);
                JSONObject jso = null;
                try {
                    jso = new JSONObject((String) data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (jso != null && internalMessageParser.isInternalMessageValid(jso)) {
                    try {
                        if (jso.getString("Action").equals("Shutdown")) {
                            if (secureListener != null) {
                                secureListener.stopListener();
                            }
                            secureListener = null;
                            if (insecureListener != null) {
                                insecureListener.stopListener();
                            }

                            insecureListener = null;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    internalMessageParser.parseInternalListenerMessageForAction(jso);
                }

            }
        }
    }

    public void addListener(BluetoothListener lis) {
        if (lis.isSecure()) {
            setSecureListener(lis);
        } else {
            setInsecureListener(lis);
        }
        Log.i(LOG_TAG, "Listener set");
    }

    /**
     * Starts the 2 listeners, secure and insecure
     *
     * @param msgData may contain additional data
     */
    public void startListeners(JSONObject msgData) {
        if (secureListener == null) {
            secureListener = BluetoothListenerMaker.getInstance().createListener(true); //can return null in case BT is off
            if (secureListener != null) {
                secureListener.addObserver(this);
                addListener(secureListener);
            }

        }
        if (insecureListener == null) {
            insecureListener = BluetoothListenerMaker.getInstance().createListener(false);//can return null in case BT is off
            if (insecureListener != null) {
                insecureListener.addObserver(this);
                addListener(insecureListener);
            }
        }
        if (secureListener != null) {
            if (!secureListener.isListening()) {
                Thread t = new Thread(secureListener);
                t.start();
                Log.i(LOG_TAG, "Secure listener started");
            } else {
                secureListener.stopListener();
                Thread t = new Thread(secureListener);
                t.start();
                Log.i(LOG_TAG, "Secure listener restarted");
            }
        }
        if (insecureListener != null) {
            if (!insecureListener.isListening()) {
                Thread t = new Thread(insecureListener);
                t.start();
                Log.i(LOG_TAG, "Insecure listener started");
            } else {
                insecureListener.stopListener();
                Thread t = new Thread(insecureListener);
                t.start();
                Log.i(LOG_TAG, "Insecure listener restarted");
            }
        }

    }


    public void shutdownAll() {
        if (secureListener != null) {
            secureListener.stopListener();
        }
        secureListener = null;
        if (insecureListener != null) {
            insecureListener.stopListener();
        }
        insecureListener = null;
        Log.i(LOG_TAG, "Disconnect all");
    }
}
