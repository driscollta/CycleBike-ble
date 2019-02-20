package com.cyclebikeapp.ble;

import android.util.SparseArray;

/**
 * Created by TommyD on 12/16/2016.
 * Enum representing the various BLE Device Types.
 */

enum BLEDeviceType {
    /*
    * 832: Heart Rate Sensor
    * 833: Heart Rate Belt
    * 1154: Cycling: Speed Sensor
    * 1155: Cycling: Cadence Sensor
    * 1156: Cycling: Power Sensor
    * 1157: Cycling: Speed and Cadence Sensor
    * ! these are not official Bluetooth SIG designations !
    * 1256: Cycling: left distributed Power Sensor
    * 1356: Cycling: right distributed Power Sensor
    * */

    BIKE_CADENCE_DEVICE(1155, "Bike Cadence"),
    BIKE_POWER_DEVICE(1156, "Bike Power"),
    BIKE_LEFT_POWER_DEVICE(1256, "Left Bike Power"),
    BIKE_RIGHT_POWER_DEVICE(1356, "Right Bike Power"),
    BIKE_SPD_DEVICE(1154, "Bike Speed"),
    BIKE_SPDCAD_DEVICE(1157, "Bike Speed-Cadence"),
    HEARTRATE_DEVICE(832, "Heart Rate"),
    HEARTRATE_DEVICE_BELT(833, "Heart Rate"),
    BIKE_SPDCAD_DEVICE_OTHER(4660, "Bike SpeedCAD - undefined"),
    NOT_BIKE_DEVICE(0, "Not bike"),
    UNKNOWN_DEVICE(5, "Unknown Device");

    private final int value;
    private static final SparseArray<BLEDeviceType> map = new SparseArray<>();

    static {
        for (BLEDeviceType typeEnum : BLEDeviceType.values()) {
            map.put(typeEnum.value, typeEnum);
        }
    }

    /**
     * Returns the enum constant of this type with the specified number.
     *
     * @param type integer equivalent of the enum constant to be returned.
     * @return the enum constant with the specified number
     */
    public static BLEDeviceType valueOf(int type) {
        //Log.v("BLEDeviceType", "DeviceType: " + type);
        //Log.v("BLEDeviceType", "valueOf DeviceType: " + map.get(type, UNKNOWN_DEVICE).toString());
        return map.get(type, UNKNOWN_DEVICE);
    }

    BLEDeviceType(int value, String name) {
        this.value = value;
        String name1 = name;
    }



    /**
     * Convert enum to equivalent int value
     * Returns: integer value equivalent
     *
     * @return integer value equivalent
     */
    public int intValue() {
        return value;
    }
}
