package de.dralle.bluetoothtest.DB;

import android.bluetooth.BluetoothDevice;

/**
 * Created by nils on 09.06.16.
 */
public class DeviceDBData {
    private int id;
    private String address;
    private String deviceName;
    private String friendlyName;
    private boolean paired;
    private int lastSeen;

    public DeviceDBData() {

    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public int getId() {
        return id;
    }

    public DeviceDBData(String address, String deviceName, String friendlyName, int lastSeen, int id, boolean paired) {
        this.address = address;
        this.deviceName = deviceName;
        this.friendlyName = friendlyName;
        this.lastSeen = lastSeen;
        this.id = id;
        this.paired = paired;
    }
    public DeviceDBData(BluetoothDevice device) {
        this.address = device.getAddress();
        this.deviceName = device.getName();
        this.friendlyName = device.getName();
        this.lastSeen = 0;
        this.id = 0;
        this.paired = device.getBondState()==BluetoothDevice.BOND_BONDED;
    }

    public void setId(int id) {
        this.id = id;
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
}
