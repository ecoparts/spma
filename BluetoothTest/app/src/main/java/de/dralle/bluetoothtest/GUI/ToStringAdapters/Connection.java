package de.dralle.bluetoothtest.GUI.ToStringAdapters;

/**
 * Created by nils on 04.07.16.
 */
public class Connection {
    private String connectionName;
    private boolean secure;
    private int lastSeen;
    private boolean encrypted;

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getConnectionTargetDeviceAddress() {
        return connectionTargetDeviceAddress;
    }

    public void setConnectionTargetDeviceAddress(String connectionTargetDeviceAddress) {
        this.connectionTargetDeviceAddress = connectionTargetDeviceAddress;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public int getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(int lastSeen) {
        this.lastSeen = lastSeen;
    }

    private String connectionTargetDeviceAddress;

    @Override
    public String toString() {
        return connectionName;
    }
}
