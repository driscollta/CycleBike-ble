package com.cyclebikeapp.ble;

import android.util.SparseArray;

/**
 * Created by TommyD on 12/16/2016.
 * Defines the possible states of a device.
 */

enum BLESensorLocation {
    /*0  Other   1 	Top of shoe  2 	In shoe
3 	Hip          4 	Front Wheel
5 	Left Crank   6 	Right Crank  7 	Left Pedal   8 	Right Pedal  9 	Front Hub   10 	Rear Dropout
11 	Chainstay   12 	Rear Wheel  13 	Rear Hub
14 	Chest       15 	Spider      16 	Chain Ring*/

    OTHER(0, "OTHER"),
    TOP_OF_SHOE(1, "TOP_OF_SHOE"),
    IN_SHOE(2, "IN_SHOE"),
    HIP(3, "HIP"),
    FRONT_WHEEL(4, "FRONT_WHEEL"),
    LEFT_CRANK(5, "LEFT_CRANK"),
    RIGHT_CRANK(6, "RIGHT_CRANK"),
    LEFT_PEDAL(7, "LEFT_PEDAL"),
    RIGHT_PEDAL(8, "RIGHT_PEDAL"),
    FRONT_HUB(9, "SEARCHING"),
    READ_DROPOUT(10, "TRACKING"),
    CHAINSTAY(11, "UNRECOGNIZED"),
    READ_WHEEL(12, "READ_WHEEL"),
    REAR_HUB(13, "REAR_HUB"),
    CHEST(14, "CHEST"),
    SPIDER(15, "SPIDER"),
    CHAIN_RING(16, "CHAIN_RING");

    private final int value;
    private final String name;
    private static final SparseArray<BLESensorLocation> map = new SparseArray<>();
    static {
        for (BLESensorLocation typeEnum : BLESensorLocation.values()) {
            map.put(typeEnum.value, typeEnum);
        }
    }

    /**
     * Returns the enum constant of this type with the specified number.
     *
     * @param type integer equivalent of the enum constant to be returned.
     * @return the enum constant with the specified number
     */
    public static BLESensorLocation valueOf(int type) {
        //Log.w("DeviceStatus", "type: " + type);
        //Log.w("DeviceStatus", "valueOf: " + map.get(type, UNRECOGNIZED).toString());
        return map.get(type, OTHER);
    }
    BLESensorLocation(int value, String name) {
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
