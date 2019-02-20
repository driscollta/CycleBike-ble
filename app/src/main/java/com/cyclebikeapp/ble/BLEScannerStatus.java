package com.cyclebikeapp.ble;

import android.util.SparseArray;

/**
 * Created by TommyD on 2/19/2018.
 */

enum BLEScannerStatus {

    DEAD(0, "DEAD"),
    NO_DEVICES(1, "NO_DEVICES"),
    STOPPED(2, "STOPPED"),
    OKAY(3, "OKAY");

    private final int value;
    private final String name;
    private static final SparseArray<BLEScannerStatus> map = new SparseArray<>();
    static {
        for (BLEScannerStatus typeEnum : BLEScannerStatus.values()) {
            map.put(typeEnum.value, typeEnum);
        }
    }

    /**
     * Returns the enum constant of this type with the specified number.
     *
     * @param type integer equivalent of the enum constant to be returned.
     * @return the enum constant with the specified number
     */
    public static BLEScannerStatus valueOf(int type) {
        //Log.w("DeviceStatus", "type: " + type);
        //Log.w("DeviceStatus", "valueOf: " + map.get(type, UNRECOGNIZED).toString());
        return map.get(type, DEAD);
    }
    BLEScannerStatus(int value, String name) {
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
