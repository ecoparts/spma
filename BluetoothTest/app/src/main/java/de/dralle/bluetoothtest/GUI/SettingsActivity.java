package de.dralle.bluetoothtest.GUI;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import de.dralle.bluetoothtest.R;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    private static final String LOG_TAG = SettingsActivity.class.getName();
    public static final String ACTION_NEW_MSG = "SettingsActivity.ACTION_NEW_MSG";
    public static final String KEY_BGSON = "bgs_on";
    private SPMAServiceConnector serviceConnector;
    public static final String KEY_USERNAME="user_name";
    public static final String KEY_BTON="bt_on";
    public static final String KEY_BTDISCOVERABLE="bt_discoverable_on";
    public static final String KEY_BTLISTENERS="bt_listeners_on";
    public static final String KEY_BTCLR="bt_clr_device_cache";
    public static final String KEY_CLRCHIST="clr_chat_hist";
    public static final String KEY_FRIENDLIST="friend_list";

    private static SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener=new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key){
                case KEY_USERNAME:
                    SPMAServiceConnector.getInstance(null).changeUserName(sharedPreferences.getString(KEY_USERNAME,""));
                    break;
                case KEY_BGSON:
                    if(sharedPreferences.getBoolean(KEY_BGSON,true)){
                        SPMAServiceConnector.getInstance(null).startService();
                    }else{
                        SPMAServiceConnector.getInstance(null).stopService();
                    }
                    break;
                case KEY_BTON:
                    if(sharedPreferences.getBoolean(KEY_BTON,true)){
                        SPMAServiceConnector.getInstance(null).turnBluetoothOn();
                    }else{
                        SPMAServiceConnector.getInstance(null).turnBluetoothOff();
                    }
                    break;
                case KEY_BTDISCOVERABLE:
                    if(sharedPreferences.getBoolean(KEY_BTDISCOVERABLE,false)){
                        SPMAServiceConnector.getInstance(null).makeDeviceVisible(300);
                    }else{
                        SPMAServiceConnector.getInstance(null).makeDeviceVisible(1);
                    }
                    break;
                case KEY_BTLISTENERS:
                    if(sharedPreferences.getBoolean(KEY_BTLISTENERS,false)){
                        SPMAServiceConnector.getInstance(null).startListeners();
                    }else{
                        SPMAServiceConnector.getInstance(null).stopListeners();
                    }
                    break;
                case KEY_BTCLR:
                    if(sharedPreferences.getBoolean(KEY_BTCLR,false)){
                        SPMAServiceConnector.getInstance(null).clearCachedDevices();
                    }
                    break;
                case KEY_CLRCHIST:
                    if(sharedPreferences.getBoolean(KEY_CLRCHIST,false)){
                        SPMAServiceConnector.getInstance(null).clearCachedHist();
                    }
                    break;
                default:
                    break;
            }
        }
    };


    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        serviceConnector.changeUserName(sharedPref.getString(KEY_USERNAME,""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        serviceConnector=SPMAServiceConnector.getInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        serviceConnector.requestCachedDevices();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                ||BluetoothPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName)
                ||ChatPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(prefChangeListener);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(prefChangeListener);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);


        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
    /**
     * This fragment shows chat preferences only.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class ChatPreferenceFragment extends PreferenceFragment {

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(prefChangeListener);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(prefChangeListener);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_chat);
            setHasOptionsMenu(true);


        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class BluetoothPreferenceFragment extends PreferenceFragment {

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(prefChangeListener);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(prefChangeListener);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_bt);
            setHasOptionsMenu(true);


        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);


        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);


        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
