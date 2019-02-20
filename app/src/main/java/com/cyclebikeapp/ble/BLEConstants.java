package com.cyclebikeapp.ble;

import java.util.UUID;

/**
 * Created by TommyD on 12/25/2016.
 *
 */

final class BLEConstants {

    static final String SCANLE_DEVICES = "scanForLeDevices() - ";

    private BLEConstants() {}
    // some Bluetooth-related static Strings
    private static final int ONE_SEC = 1000;
    static final long DEVICE_DISCOVERY_TIME = 11 * ONE_SEC;
    static final long SCAN_PERIOD = 5 * ONE_SEC;
    static final int SECONDS_PER_MINUTE = 60;
    static final double POWER_WHEEL_TIME_RESOLUTION = 2048.;
    static final double WHEEL_TIME_RESOLUTION = 1024.;
    static final double POWER_CRANK_TIME_RESOLUTION = 1024.;
    static final double CADENCE_TIME_RESOLUTION = 1024.;
    static final int RADIX16 = 16;
    static final int SHORT_UUID_BEGIN_INDEX = 4;
    static final int SHORT_UUID_END_INDEX = 8;
    final static String ACTION_HRM_GATT_CONNECTED =
            "com.cyclebikeapp.ble.ACTION_HRM_GATT_CONNECTED";
    final static String ACTION_HRM_GATT_DISCONNECTED =
            "com.cyclebikeapp.ble.ACTION_HRM_GATT_DISCONNECTED";
    final static String ACTION_HRM_GATT_SERVICES_DISCOVERED =
            "com.cyclebikeapp.ble.ACTION_HRM_GATT_SERVICES_DISCOVERED";
    final static String ACTION_HRM_DATA_AVAILABLE =
            "com.cyclebikeapp.ble.ACTION_HRM_DATA_AVAILABLE";
    final static String EXTRA_HRM_DATA = "com.cyclebikeapp.ble.EXTRA_HRM_DATA";
    final static String EXTRA_HRM_DATA_TYPE = "com.cyclebikeapp.ble.EXTRA_HRM_DATA_TYPE";
    static final String EXTRAS_DEVICE_HRM_ADDRESS = "com.cyclebikeapp.ble.EXTRAS_DEVICE_HRM_ADDRESS";
    final static UUID UUID_HEARTRATE_SERVICE = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
//    final static String UUID_HEARTRATE_SERVICE_SHORT = "0000180D";
    final static UUID UUID_CSC_SERVICE = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
//    final static String UUID_CSC_SERVICE_SHORT = "00001816";

    final static UUID UUID_POWER_SERVICE = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb");
//    final static String UUID_POWER_SERVICE_SHORT = "00001818";
    final static UUID UUID_BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    final static UUID UUID_POWER_MEASUREMENT = UUID.fromString("00002a63-0000-1000-8000-00805f9b34fb");
    final static UUID UUID_POWER_CONTROL_PT = UUID.fromString("00002a66-0000-1000-8000-00805f9b34fb");
    final static UUID UUID_GENERIC_ATTRIBUTE_SERVICE = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
    final static UUID UUID_GENERIC_ACCESS_SERVICE = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    final static UUID UUID_DEVICE_INFO_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb");
    final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    final static UUID UUID_CSC_MEASUREMENT = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb");
    static final UUID BATTERY_LEVEL_CHARACTER_UUID =
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    static final UUID CHARACTERISTIC_PRESENTATION_FORMAT_DESCRIPTOR_UUID =
            UUID.fromString("00002904-0000-1000-8000-00805f9b34fb");
    static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    static final UUID REPORT_REFERENCE_DESCRIPTOR_UUID =
            UUID.fromString("00002908-0000-1000-8000-00805f9b34fb");
     static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    final static String ACTION_SPEED_GATT_CONNECTED =
            "com.cyclebikeapp.ble.ACTION_SPEED_GATT_CONNECTED";
    final static String ACTION_SPEED_GATT_DISCONNECTED =
            "com.cyclebikeapp.ble.ACTION_SPEED_GATT_DISCONNECTED";
    final static String ACTION_SPEED_GATT_SERVICES_DISCOVERED =
            "com.cyclebikeapp.ble.ACTION_SPEED_GATT_SERVICES_DISCOVERED";
    final static String ACTION_SPEED_DATA_AVAILABLE =
            "com.cyclebikeapp.ble.ACTION_SPEED_DATA_AVAILABLE";
    final static String EXTRA_REV_DATA = "com.cyclebikeapp.ble.EXTRA_REV_DATA";
    final static String EXTRA_REV_TIME_DATA = "com.cyclebikeapp.ble.EXTRA_REV_TIME_DATA";
    final static String EXTRA_SPEED_DATA = "com.cyclebikeapp.ble.EXTRA_SPEED_DATA";
    final static String EXTRA_CSC_DATA_TYPE = "com.cyclebikeapp.ble.EXTRA_CSC_DATA_TYPE";
    static final String EXTRAS_DEVICE_SPEED_ADDRESS = "com.cyclebikeapp.ble.EXTRAS_DEVICE_SPEED_ADDRESS";
    final static String EXTRA_CAD_DATA = "com.cyclebikeapp.ble.EXTRA_CAD_DATA";
    static final String EXTRAS_DEVICE_CAD_ADDRESS = "com.cyclebikeapp.ble.EXTRAS_DEVICE_CAD_ADDRESS";
    final static String ACTION_CAD_GATT_CONNECTED = "com.cyclebikeapp.ble.ACTION_CAD_GATT_CONNECTED";
    final static String ACTION_CAD_GATT_DISCONNECTED = "com.cyclebikeapp.ble.ACTION_CAD_GATT_DISCONNECTED";
    final static String ACTION_CAD_GATT_SERVICES_DISCOVERED = "com.cyclebikeapp.ble.ACTION_CAD_GATT_SERVICES_DISCOVERED";
    final static String ACTION_CAD_DATA_AVAILABLE = "com.cyclebikeapp.ble.ACTION_CAD_DATA_AVAILABLE";
    final static String EXTRA_CSC_DEVICE_TYPE = "com.cyclebikeapp.ble.EXTRA_CSC_DEVICE_TYPE";
    final static String EXTRA_POWER_DATA_TYPE = "com.cyclebikeapp.ble.EXTRA_POWER_DATA_TYPE";
    final static String EXTRA_POWER_DATA = "com.cyclebikeapp.ble.EXTRA_POWER_DATA";
    final static String EXTRA_POWER_IPOWER = "com.cyclebikeapp.ble.EXTRA_POWER_IPOWER";
    final static String EXTRA_POWER_IS_DISTRIBUTED = "com.cyclebikeapp.ble.EXTRA_POWER_IS_DISTRIBUTED";
    final static String EXTRA_CAN_CHANGE_CRANK_LENGTH = "com.cyclebikeapp.ble.EXTRA_CAN_CHANGE_CRANK_LENGTH";
    final static String EXTRA_POWER_HAS_SPEED = "com.cyclebikeapp.ble.EXTRA_POWER_HAS_SPEED";
    final static String EXTRA_POWER_HAS_CAD = "com.cyclebikeapp.ble.EXTRA_POWER_HAS_CAD";
    final static String EXTRA_POWER_WHEEL_REVS = "com.cyclebikeapp.ble.EXTRA_POWER_WHEEL_REVS";
    final static String EXTRA_POWER_WHEEL_REV_TIME = "com.cyclebikeapp.ble.EXTRA_POWER_WHEEL_REV_TIME";
    final static String EXTRA_POWER_CRANK_REVS = "com.cyclebikeapp.ble.EXTRA_POWER_CRANK_REVS";
    final static String EXTRA_POWER_CRANK_REV_TIME = "com.cyclebikeapp.ble.EXTRA_POWER_CRANK_REV_TIME";
    final static String EXTRA_POWER_CAL_VALUE = "com.cyclebikeapp.ble.EXTRA_POWER_CAL_VALUE";
    final static String EXTRA_RESPONSE_VALUE = "com.cyclebikeapp.ble.EXTRA_RESPONSE_VALUE";
    final static String EXTRA_POWER_CRANK_LENGTH = "com.cyclebikeapp.ble.EXTRA_POWER_CRANK_LENGTH";
    final static String EXTRA_POWER_SENSOR_LOCATION = "com.cyclebikeapp.ble.EXTRA_POWER_SENSOR_LOCATION";
    static final String EXTRAS_DEVICE_POWER_ADDRESS = "com.cyclebikeapp.ble.EXTRAS_DEVICE_POWER_ADDRESS";
    final static String EXTRA_OPPOSITE_POWER_DATA_TYPE = "com.cyclebikeapp.ble.EXTRA_OPPOSITE_POWER_DATA_TYPE";
    static final String EXTRAS_DEVICE_OPPOSITE_POWER_ADDRESS = "com.cyclebikeapp.ble.EXTRAS_DEVICE_OPPOSITE_POWER_ADDRESS";

    final static String EXTRA_WHEEL_REVS = "com.cyclebikeapp.ble.EXTRA_WHEEL_REVS";
    final static String EXTRA_WHEEL_REV_TIME = "com.cyclebikeapp.ble.EXTRA_WHEEL_REV_TIME";
    final static String ACTION_POWER_GATT_CONNECTED =
            "com.cyclebikeapp.ble.ACTION_POWER_GATT_CONNECTED";
    final static String ACTION_POWER_GATT_DISCONNECTED =
            "com.cyclebikeapp.ble.ACTION_POWER_GATT_DISCONNECTED";
    final static String ACTION_POWER_GATT_SERVICES_DISCOVERED =
            "com.cyclebikeapp.ble.ACTION_POWER_GATT_SERVICES_DISCOVERED";
    final static String ACTION_POWER_DATA_AVAILABLE =
            "com.cyclebikeapp.ble.ACTION_POWER_DATA_AVAILABLE";
    final static String ACTION_GATT_CONNECTED =
            "com.cyclebikeapp.ble.ACTION_GATT_CONNECTED";
    final static String ACTION_GATT_DISCONNECTED =
            "com.cyclebikeapp.ble.ACTION_GATT_DISCONNECTED";
    final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.cyclebikeapp.ble.ACTION_GATT_SERVICES_DISCOVERED";
    final static String ACTION_OPPOSITE_POWER_GATT_CONNECTED =
            "com.cyclebikeapp.ble.ACTION_OPPOSITE_POWER_GATT_CONNECTED";
    final static String ACTION_OPPOSITE_POWER_GATT_DISCONNECTED =
            "com.cyclebikeapp.ble.ACTION_OPPOSITE_POWER_GATT_DISCONNECTED";
    final static String ACTION_OPPOSITE_POWER_GATT_SERVICES_DISCOVERED =
            "com.cyclebikeapp.ble.ACTION_OPPOSITE_POWER_GATT_SERVICES_DISCOVERED";
    final static String ACTION_OPPOSITE_POWER_DATA_AVAILABLE =
            "com.cyclebikeapp.ble.ACTION_OPPOSITE_POWER_DATA_AVAILABLE";

    final static String ACTION_DATA_AVAILABLE = "com.cyclebikeapp.ble.ACTION_DATA_AVAILABLE";
    final static String EXTRA_DATA = "com.cyclebikeapp.ble.EXTRA_DATA";
    static final String EXTRAS_DEVICE_ADDRESS = "com.cyclebikeapp.ble.EXTRA_DEVNUM_DATA";
    final static String EXTRA_PCP_OPCODE = "com.cyclebikeapp.ble.EXTRA_PCP_OPCODE";
    final static String EXTRA_OPPOSITE_PCP_OPCODE = "com.cyclebikeapp.ble.EXTRA_OPPOSITE_PCP_OPCODE";

    static final int CALIBRATE_POWER_OPCODE = 12;
    static final int REQUEST_CRANK_LENGTH_OPCODE = 5;
    static final int WRITE_CRANK_LENGTH_OPCODE = 4;

    static final int RESPONSE_VALUE_SUCCESS = 1;
    static final int RESPONSE_VALUE_FAILURE = 4;
    static final int RESPONSE_VALUE_NOT_SUPPORTED = 2;

}
