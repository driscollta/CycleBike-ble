package com.cyclebikeapp.ble;

import android.util.SparseArray;

/**
 * Created by TommyD on 12/15/2016.
 *
 */

enum BLEDataType {

    DEVICE_NAME(0x2A00, "DEVICE_NAME"),
    /* utf8s*/
    APPEARANCE(0x2A01, "APPEARANCE"),
    /* 832: Heart Rate Sensor
    * 833: Heart Rate Belt
    * 1154: Cycling: Speed Sensor
    * 1155: Cycling: Cadence Sensor
    * 1156: Cycling: Power Sensor
    * 1157: Cycling: Speed and Cadence Sensor*/
    PREFERRED_CONNECTION_PARAMS(0x2A04, "PREFERRED_CONNECTION_PARAMS"),
    SERVICE_CHANGED(0x2A05, "SERVICE_CHANGED"),
    BATTERY_LEVEL(0x2A19, "BATTERY_LEVEL"),
    /* uint8, percentage 0-100*/
    MODEL_NUMBER(0x2A24, "MODEL_NUMBER"),
    /* utf8s*/
    SERIAL_NUMBER(0x2A25, "SERIAL_NUMBER"),
    /* utf8s*/
    FIRMWARE_REV(0x2A26, "FIRMWARE_REV"),
    /* utf8s*/
    HARDWARE_REV(0x2A27, "HARDWARE_REV"),
    /* utf8s*/
    SOFTWARE_REV(0x2A28, "SOFTWARE_REV"),
    /* utf8s*/
    MANUFACTURER_NAME(0x2A29, "MANUFACTURER"),
    /* utf8s*/
    HEARTRATE_MEAS(0x2A37, "HEARTRATE"),
    /* 8-bit: bit 0 = 0, HR value is uint8; bit 0 = 1, HR value is uint16; bit 1,2 Sensor contact
     * offset1: uint8 heartrate measurement bpm
      * or
      * offset2: uint16 heartrate measurement bpm*/
    BODY_SENSOR_LOC(0x2A38, "BODY_SENSOR_LOC"),
    /* 8bit
    Key 	Value
    0 	Other     1 	Chest    2 	Wrist
    3 	Finger    4 	Hand    5 	Ear Lobe    6 	Foot*/
    HR_CONTROL_PT(0x2A39, "HR_CONTROL_PT"),
    SC_CONTROL_PT(0x2A55, "SC_CONTROL_PT"),
    CSC_MEAS(0x2A5B, "CSC_MEASUREMENT"),
    /* 8 bit: bit 0 = 1, wheel revolution data present; bit 1 = 1, crank revolution data present
    * offset 1: uint32 (Wheel revs) Cumulative Wheel Revolutions, or uint16 (Cumulative Crank Revolutions)
    * offset 2: uint16 Last Wheel Event Time in seconds with a resolution of 1/1024*/
    BIKE_SPD_CAD_FEATURE(0x2A5C, "BIKE_SPD_CAD_FEATURE"),
    SENSOR_LOCATION(0x2A5D, "SENSOR_LOCATION"),
    /*0  Other   1 	Top of shoe  2 	In shoe
3 	Hip          4 	Front Wheel
5 	Left Crank   6 	Right Crank  7 	Left Pedal   8 	Right Pedal  9 	Front Hub   10 	Rear Dropout
11 	Chainstay   12 	Rear Wheel  13 	Rear Hub
14 	Chest       15 	Spider      16 	Chain Ring*/
    BIKE_POWER_MEAS(0x2A63, "BIKE_POWER"),
    /* 16-bit: bit 0 = 1, Pedal Power Balance Present; bit 1 = 1, Pedal Power Balance Reference; bit 2 = 1, Accumulated Torque Present,
    bit 3 = 1, Accumulated Torque Source; bit 4 = 1, Wheel Revolution Data Present; bit 5 = 1, Crank Revolution Data Present, etc
     offset 1: sint16:  Instantaneous Power (W);
     offset 2?: uint8: Pedal Power Balance
     offset 3?: uint16:  Accumulated Torque;
     offset 4?: uint32 (Wheel revs) Cumulative Wheel Revolutions, or uint16 (Cumulative Crank Revolutions)
     offset 5: uint16 Last Wheel Event Time in seconds with a resolution of 1/1024
     */
    BIKE_POWER_VECTOR(0x2A64, "BIKE_POWER_VECTOR"),
    /* 8-bit: bit 0 = 1, crank revolution data present; bit 1 = 1, 1st crank measurement angle present; bit 2 = 1, Instantaneous Force Magnitude Array present, etc,
     offset 1: uint16:  Cumulative Crank Revolutions;
     offset 2: uint16: Crank Revolution Data - Last Crank Event Time in seconds with a resolution of 1/1024*/
    BIKE_POWER_FEATURE(0x2A65, "BIKE_POWER_FEATURE"),
    /* 32-bit: bit 0 = 1, Pedal Power Balance; bit 1 = 1, accumulated Torque; bit 2 = 1, Wheel revolution;
     bit 3 = 1, Crank revolution; */
    BIKE_POWER_CONTROL_PT(0x2A66, "BIKE_POWER_CONTROL_PT"),
    /* 16 bit: bit 0 = 1, wheel revolution; bit 1 = 1, crank revolution */
    UNKNOWN(0x0000,"UNKNOWN");

    private final int value;
    private final String name;

    BLEDataType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int intValue() {
        return value;
    }

    private static final SparseArray<BLEDataType> map = new SparseArray<>();

    static {
        for (BLEDataType typeEnum : BLEDataType.values()) {
            map.put(typeEnum.value, typeEnum);
        }
    }

    public static BLEDataType valueOf(int type) {
        //Log.v("BLEDATATYPE", "DataType: " + type);
        //Log.v("BLEDATATYPE", "valueOf DataType: " + map.get(type, UNKNOWN).toString());
        return map.get(type, UNKNOWN);
    }

    public String toString() {
        return this.name;
    }

}
