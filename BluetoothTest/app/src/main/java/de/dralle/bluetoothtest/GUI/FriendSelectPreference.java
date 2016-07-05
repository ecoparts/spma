package de.dralle.bluetoothtest.GUI;

import android.content.Context;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.dralle.bluetoothtest.DB.DeviceDBData;

/**
 * Created by nils on 05.07.16.
 */
public class FriendSelectPreference extends MultiSelectListPreference{
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
    }
}
