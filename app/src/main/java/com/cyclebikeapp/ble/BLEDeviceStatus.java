package com.cyclebikeapp.ble;

import android.util.SparseArray;

/**
 * Created by TommyD on 12/16/2016.
 * Defines the possible states of a device.
 */

enum BLEDeviceStatus {
        /*
CLOSED
The device is not connected and not trying to connect but will still respond to commands.
DEAD
The device is no longer usable and will not respond to commands.
PROCESSING_REQUEST
The plugin is currently processing a command request from a client.
SEARCHING
The device is attempting to establish a connection.
TRACKING
The device has an open connection, and can receive and transmit data.
UNRECOGNIZED
The value sent by the plugin service was unrecognized indicating an upgrade of the PCC may be required to handle the value.
            */

    CLOSED(0, "CLOSED"),
    DEAD(1, "DEAD"),
    PROCESSING_REQUEST(2, "PROCESSING_REQUEST"),
    SEARCHING(3, "SEARCHING"),
    TRACKING(4, "TRACKING"),
    UNRECOGNIZED(5, "UNRECOGNIZED");

    private final int value;
    private final String name;
    private static final SparseArray<BLEDeviceStatus> map = new SparseArray<>();
    static {
        for (BLEDeviceStatus typeEnum : BLEDeviceStatus.values()) {
            map.put(typeEnum.value, typeEnum);
        }
    }

    /**
     * Returns the enum constant of this type with the specified number.
     *
     * @param type integer equivalent of the enum constant to be returned.
     * @return the enum constant with the specified number
     */
    public static BLEDeviceStatus valueOf(int type) {
        //Log.w("DeviceStatus", "type: " + type);
        //Log.w("DeviceStatus", "valueOf: " + map.get(type, UNRECOGNIZED).toString());
        return map.get(type, UNRECOGNIZED);
    }
    BLEDeviceStatus(int value, String name) {
        this.value = value;
        this.name = name;
    }
    public String toString() {
        return this.name;
    }

    /**
     * Convert enum to equivalent int value
     * Returns:
     * integer value equivalent
     *
     * @return integer value equivalent
     */
    public int intValue() {
        return value;
    }
}
