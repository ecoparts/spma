package de.dralle.bluetoothtest.GUI;

import android.content.Context;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;

/**
 * Created by nils on 05.07.16.
 */
public class FriendSelectPreference extends MultiSelectListPreference{
    public FriendSelectPreference(Context context) {
        super(context);
    }

    public FriendSelectPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

}
