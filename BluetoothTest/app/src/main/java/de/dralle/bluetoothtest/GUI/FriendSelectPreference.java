package de.dralle.bluetoothtest.GUI;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.dralle.bluetoothtest.DB.DeviceDBData;

/**
 * Created by nils on 05.07.16.
 */
public class FriendSelectPreference extends MultiSelectListPreference{
    private static final String LOG_TAG = FriendSelectPreference.class.getName();
    private SPMAServiceConnector serviceConnector=null;
    public FriendSelectPreference(Context context) {
        super(context);

        serviceConnector=SPMAServiceConnector.getInstance(null);
    }

    public FriendSelectPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        serviceConnector=SPMAServiceConnector.getInstance(null);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        super.onSetInitialValue(restoreValue, defaultValue);

        serviceConnector.requestCachedDevices();
        List<DeviceDBData> devices = serviceConnector.getCachedDevices();
       CharSequence[] friendListEntries=new CharSequence[devices.size()];
        CharSequence[] friendListValues=new CharSequence[devices.size()];
        for(int i=0;i<devices.size();i++){
            friendListEntries[i]=devices.get(i).getFriendlyName();
            friendListValues[i]=devices.get(i).getAddress();
        }
        setEntries(friendListEntries);
        setEntryValues(friendListValues);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if(positiveResult){
            CharSequence[] friendListEntries=getEntries();
            CharSequence[] friendListValues=getEntryValues();
            SharedPreferences pref=getSharedPreferences();
            Set<String> friendList = pref.getStringSet(SettingsActivity.KEY_FRIENDLIST, new HashSet<String>());
            for(int i=0;i<friendListValues.length;i++){
                if(friendList.contains(friendListValues[i])) {
                    Log.v(LOG_TAG, "Entry " + friendListEntries[i] + " Value " + friendListValues[i]+" shall be added as friend");
                }else{
                    Log.v(LOG_TAG, "Entry " + friendListEntries[i] + " Value " + friendListValues[i]+" shall not be added as friend");
                }
            }
        }
    }
}
