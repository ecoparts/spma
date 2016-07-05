package de.dralle.bluetoothtest.GUI.ToStringAdapters;

/**
 * Created by nils on 04.07.16.
 */
public class Device {
    private String deviceName;
    private String deviceAddress;
    private boolean paired;
    private int lastSeen;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public int getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(int lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isPaired() {
        return paired;
    }

    public void setPaired(boolean paired) {
        this.paired = paired;
    }

    @Override
    public String toString() {
        return deviceName;
    }
}
