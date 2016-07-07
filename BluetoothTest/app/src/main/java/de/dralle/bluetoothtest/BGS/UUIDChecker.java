package de.dralle.bluetoothtest.BGS;

import android.os.ParcelUuid;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by nils on 20.06.16.
 */
public class UUIDChecker {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = UUIDChecker.class.getName();
    /**
     * UUIDs to check for
     */
    private Set<UUID> uuidsToCheckFor = null;

    /**
     * Dirty hack for android 6.0.1
     */
    private boolean android6hack = false;

    public UUIDChecker(boolean android6hack) {
        this.android6hack = android6hack;
        uuidsToCheckFor = new HashSet<>();
    }

    public void addUUID(UUID u) {
        if (u != null) {
            uuidsToCheckFor.add(u);
            Log.i(LOG_TAG, "UUID added for checking " + u.toString());
        }
    }

    public boolean checkForSupportedUUIDs(ParcelUuid[] allUUIDs) {
        if(allUUIDs==null){
            return false;
        }else {
            UUID[] uuids = new UUID[allUUIDs.length];
            for (int i = 0; i < allUUIDs.length; i++) {
                uuids[i] = allUUIDs[i].getUuid();
            }
            return checkForSupportedUUIDs(uuids);
        }
    }

    public boolean checkForSupportedUUIDs(UUID[] allUUIDs) {
        if (allUUIDs != null) {
            for (UUID uuid : allUUIDs) {
                for (UUID checkAgainst : uuidsToCheckFor) {
                    if (uuid.compareTo(checkAgainst) == 0) {
                        return true;
                    }
                    if (android6hack) {
                        UUID reversed = reverseUuid(uuid); //https://code.google.com/p/android/issues/detail?id=198238
                        if (reversed.compareTo(checkAgainst) == 0) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }


    private UUID reverseUuid(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        byte[] uuidBytes = bb.array();

        byte[] uuidNewBytes = new byte[uuidBytes.length];
        for (int i = 0; i < uuidBytes.length; i++) {
            uuidNewBytes[i] = uuidBytes[uuidBytes.length - i - 1];
        }
        ByteBuffer bb2 = ByteBuffer.wrap(uuidNewBytes);
        return new UUID(bb2.getLong(), bb2.getLong());
    }
}
