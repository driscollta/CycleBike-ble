package com.cyclebikeapp.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_CAD_DATA_AVAILABLE;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_CAD_GATT_CONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_CAD_GATT_DISCONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_CAD_GATT_SERVICES_DISCOVERED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_GATT_CONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_GATT_DISCONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_GATT_SERVICES_DISCOVERED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_HRM_DATA_AVAILABLE;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_HRM_GATT_CONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_HRM_GATT_DISCONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_HRM_GATT_SERVICES_DISCOVERED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_OPPOSITE_POWER_DATA_AVAILABLE;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_OPPOSITE_POWER_GATT_CONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_OPPOSITE_POWER_GATT_DISCONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_OPPOSITE_POWER_GATT_SERVICES_DISCOVERED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_POWER_DATA_AVAILABLE;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_POWER_GATT_CONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_POWER_GATT_DISCONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_POWER_GATT_SERVICES_DISCOVERED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_SPEED_DATA_AVAILABLE;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_SPEED_GATT_CONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_SPEED_GATT_DISCONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_SPEED_GATT_SERVICES_DISCOVERED;
import static com.cyclebikeapp.ble.BLEConstants.CLIENT_CHARACTERISTIC_CONFIG;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_CAN_CHANGE_CRANK_LENGTH;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_IS_DISTRIBUTED;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_SENSOR_LOCATION;
import static com.cyclebikeapp.ble.BLEConstants.RADIX16;
import static com.cyclebikeapp.ble.BLEConstants.SHORT_UUID_BEGIN_INDEX;
import static com.cyclebikeapp.ble.BLEConstants.SHORT_UUID_END_INDEX;
import static com.cyclebikeapp.ble.BLEConstants.UUID_CSC_SERVICE;
import static com.cyclebikeapp.ble.BLEConstants.UUID_HEARTRATE_SERVICE;
import static com.cyclebikeapp.ble.BLEConstants.UUID_POWER_SERVICE;
import static com.cyclebikeapp.ble.BLEDataType.APPEARANCE;
import static com.cyclebikeapp.ble.BLEDataType.BATTERY_LEVEL;
import static com.cyclebikeapp.ble.BLEDataType.BIKE_POWER_CONTROL_PT;
import static com.cyclebikeapp.ble.BLEDataType.BIKE_POWER_MEAS;
import static com.cyclebikeapp.ble.BLEDataType.BODY_SENSOR_LOC;
import static com.cyclebikeapp.ble.BLEDataType.CSC_MEAS;
import static com.cyclebikeapp.ble.BLEDataType.HEARTRATE_MEAS;
import static com.cyclebikeapp.ble.BLEDataType.HR_CONTROL_PT;
import static com.cyclebikeapp.ble.BLEDataType.PREFERRED_CONNECTION_PARAMS;
import static com.cyclebikeapp.ble.BLEDataType.SC_CONTROL_PT;
import static com.cyclebikeapp.ble.BLEDataType.SERVICE_CHANGED;
import static com.cyclebikeapp.ble.BLEDataType.UNKNOWN;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_LEFT_POWER_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_POWER_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_RIGHT_POWER_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_SPDCAD_DEVICE_OTHER;
import static com.cyclebikeapp.ble.BLEDeviceType.UNKNOWN_DEVICE;
import static com.cyclebikeapp.ble.BLESensorLocation.LEFT_CRANK;
import static com.cyclebikeapp.ble.BLESensorLocation.LEFT_PEDAL;
import static com.cyclebikeapp.ble.Constants.DB_KEY_ACTIVE;
import static com.cyclebikeapp.ble.Constants.DB_KEY_BATT_STATUS;
import static com.cyclebikeapp.ble.Constants.DB_KEY_DEV_NAME;
import static com.cyclebikeapp.ble.Constants.DB_KEY_DEV_TYPE;
import static com.cyclebikeapp.ble.Constants.DB_KEY_MANUFACTURER;
import static com.cyclebikeapp.ble.Constants.DB_KEY_MODEL_NUM;
import static com.cyclebikeapp.ble.Constants.DB_KEY_SEARCH_PRIORITY;
import static com.cyclebikeapp.ble.Constants.DB_KEY_SERIAL_NUM;
import static com.cyclebikeapp.ble.Constants.DB_KEY_SOFTWARE_REV;
import static com.cyclebikeapp.ble.Constants.KEY_SP_DEVICE_NAME;

/**
 * Created by TommyD on 12/25/2016.
 * A collection of utility functions used in the Bluetooth communications
 */

class BLEUtilities {
    /**
     *
     * handle data from GATTReceiver sent from a BLEService. This data is common to all BIKE BLE Devices.
     * We'll just save the data in ContentValues and deliver back to the Receiver that called.
     * @param intent the Intent that delivered the data
     * @param newContent store the received data in ContentValues structure
     * @param type switch on DataType
     * @param dataSource key to extract data from the Intent
     */
    static void handleBLEDeviceInformation(Intent intent, ContentValues newContent, BLEDataType type, String dataSource) {
        switch (type) {
            case DEVICE_NAME:
                String deviceName = intent.getStringExtra(dataSource);
                newContent.put(DB_KEY_DEV_NAME, deviceName);
                //Log.i("BLEUtilities", "received device name: " + deviceName);
                break;
            case APPEARANCE:
                // can't use this since Wahoo doesn't implement it
            case SERVICE_CHANGED:
                break;
            case BATTERY_LEVEL:
                String batteryLevel = intent.getStringExtra(dataSource);
                newContent.put(DB_KEY_BATT_STATUS, batteryLevel);
                Log.i("BLEUtilities", "received Battery level: " + batteryLevel);
                break;
            case MODEL_NUMBER:
                String modelNumber = intent.getStringExtra(dataSource);
                newContent.put(DB_KEY_MODEL_NUM, modelNumber);
                //Log.i("BLEUtilities", "received Model #: " + modelNumber);
                break;
            case SERIAL_NUMBER:
                String serialNumber = intent.getStringExtra(dataSource);
                newContent.put(DB_KEY_SERIAL_NUM, serialNumber);
                //Log.i("BLEUtilities", "received Serial #: " + serialNumber);
                break;
            case FIRMWARE_REV:
                String firmwareRev = intent.getStringExtra(dataSource);
                newContent.put(DB_KEY_SOFTWARE_REV, firmwareRev);
                Log.i("BLEUtilities", "received Firmware rev: " + firmwareRev);
                break;
            case HARDWARE_REV:
                String hardwareRev = intent.getStringExtra(dataSource);
                newContent.put(DB_KEY_MODEL_NUM, hardwareRev);
                //Log.i("BLEUtilities", "received Hardware rev: " + hardwareRev);
                break;
            case SOFTWARE_REV:
                String softwareRev = intent.getStringExtra(dataSource);
                //newContent.put(DB_KEY_SOFTWARE_REV, softwareRev);
                //Log.i("BLEUtilities", "received Software rev: " + softwareRev);
                break;
            case MANUFACTURER_NAME:
                String manufacturer = intent.getStringExtra(dataSource);
                newContent.put(DB_KEY_MANUFACTURER, manufacturer);
                //Log.i("BLEUtilities", "received Manufacturer: " + manufacturer);
                break;

            default:
                //Log.i("BLEUtilities", "received other data: " + intent.getStringExtra(dataSource));
        }// switch
    }

    static void logCharacteristicDescriptors(BluetoothGattCharacteristic gattCharacteristic) {
        String logtag = "BLEUtilities";
        List<BluetoothGattDescriptor> theDescriptorList = gattCharacteristic.getDescriptors();
        Log.i(logtag, "descriptors for gattCharacteristic: " + gattCharacteristic.getUuid().toString());
        for(BluetoothGattDescriptor aDescriptor:theDescriptorList){
            Log.i(logtag, aDescriptor.getUuid().toString() + " permission: " + aDescriptor.getPermissions());
        }
    }

    /**
     * Test if a device is already in the bleActiveBikeDeviceList ArrayList
     *
     * @param device
     *            the String device address to look for
     * @return true if the device address is already in the list
     */
    static boolean isDeviceInSPList(HashMap<String, String> device, ArrayList<HashMap<String, String>> deviceSPList) {
        for (HashMap<String, String> map: deviceSPList) {
            if ((map.get(KEY_SP_DEVICE_NAME)).equals(device.get(KEY_SP_DEVICE_NAME))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBluetoothAdapterOn(BluetoothAdapter mBTAdapter) {
        return mBTAdapter.getState() == BluetoothAdapter.STATE_ON;
    }

    private static boolean isBluetoothAdapterEnabled(BluetoothAdapter mBTAdapter) {
        return mBTAdapter.isEnabled();
    }

    private static boolean isBluetoothAdapterDiscovering(BluetoothAdapter mBTAdapter) {
        return mBTAdapter.isDiscovering();
    }
    @NonNull
    static String getBTAdapterStatusString(BluetoothAdapter mBluetoothAdapter) {
        return " BluetoothAdapter on? "
                + (BLEUtilities.isBluetoothAdapterOn(mBluetoothAdapter) ? "yes" : "no")
                + " BluetoothAdapter enabled? "
                + (BLEUtilities.isBluetoothAdapterEnabled(mBluetoothAdapter) ? "yes" : "no")
                + " BluetoothAdapter discovering? "
                + (BLEUtilities.isBluetoothAdapterDiscovering(mBluetoothAdapter) ? "yes" : "no");
    }

    static void logUUIDS(List<UUID> theUUIDs, String type) {
        String logtag = "BLEUtilities " + type;
        for(UUID aUUID:theUUIDs){
            Log.i(logtag, "LeScanCallback advertised UUIDs: " + aUUID.toString());
        }
    }
    // this just parses response from LEScan to extract the Device name. Other info is available
    static BLEDeviceData parseAdvertisedData(byte[] advertisedData, String address) {
        List<UUID> uuids = new ArrayList<>();
        String name = "";
        BLEDeviceType deviceType = UNKNOWN_DEVICE;
        if (advertisedData == null) {
            return new BLEDeviceData(address, deviceType, name);
        }

        ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) {
                break;
            }

            byte type = buffer.get();
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    //logUUIDS(uuids, "case 0x03");
                    if (isUUIDinList(uuids, UUID_HEARTRATE_SERVICE.toString())){
                        deviceType = BLEDeviceType.HEARTRATE_DEVICE;
                    } else if (isUUIDinList(uuids, UUID_CSC_SERVICE.toString())){
                        deviceType = BIKE_SPDCAD_DEVICE_OTHER;
                    } else if (isUUIDinList(uuids, UUID_POWER_SERVICE.toString())){
                        deviceType = BLEDeviceType.BIKE_POWER_DEVICE;
                    }
                    break;
                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }

                    if (isUUIDinList(uuids, UUID_HEARTRATE_SERVICE.toString())){
                        deviceType = BLEDeviceType.HEARTRATE_DEVICE;
                    }  else if (isUUIDinList(uuids, UUID_CSC_SERVICE.toString())){
                        deviceType = BIKE_SPDCAD_DEVICE_OTHER;
                    } else if (isUUIDinList(uuids, UUID_POWER_SERVICE.toString())){
                        deviceType = BLEDeviceType.BIKE_POWER_DEVICE;
                    }
                    //logUUIDS(uuids, "case 0x07");
                    break;
                case 0x09:
                    byte[] nameBytes = new byte[length - 1];
                    buffer.get(nameBytes);
                    try {
                        name = new String(nameBytes, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    buffer.position(buffer.position() + length - 1);
                    break;
            }
        }
        return  new BLEDeviceData(address, deviceType, name);
    }


    private static boolean isUUIDinList(List<UUID> uuids, String s) {
        for(UUID aUUID:uuids){
            String logtag = "BLEUtilities - isUUIDinList";
            if (aUUID.toString().contains(s)){
                //Log.i(logtag, "Service String: " + s + " found UUID: " + aUUID.toString());
                return true;
            }
        }
        return false;
    }
    static void handleBikeFeature(Intent intent, ContentValues newContent, BLEDeviceData theDeviceData) {
        theDeviceData.canReadCrankLength = intent.getBooleanExtra(EXTRA_CAN_CHANGE_CRANK_LENGTH, false);
        theDeviceData.isDistributedPower = intent.getBooleanExtra(EXTRA_POWER_IS_DISTRIBUTED, false);
        BLEDeviceType theType = determinePowerDeviceType(theDeviceData);
        newContent.put(DB_KEY_DEV_TYPE, theType.intValue());
        theDeviceData.setDeviceType(theType);
    }

    static void handleSensorLocation(Intent intent, ContentValues newContent, BLEDeviceData theDeviceData) {
        theDeviceData.setSensorLocation(BLESensorLocation.valueOf(intent.getIntExtra(EXTRA_POWER_SENSOR_LOCATION, 0)));
        BLEDeviceType theType = determinePowerDeviceType(theDeviceData);
        theDeviceData.setDeviceType(theType);
        newContent.put(DB_KEY_DEV_TYPE, theType.intValue());
    }

    @NonNull
    private static BLEDeviceType determinePowerDeviceType(BLEDeviceData theDeviceData) {
        BLESensorLocation theSensorLocation = theDeviceData.getSensorLocation();
        BLEDeviceType powerDeviceType = BIKE_POWER_DEVICE;
        if (theDeviceData.isDistributedPower &&
                (theSensorLocation == LEFT_CRANK || theSensorLocation == LEFT_PEDAL)) {
            powerDeviceType = BIKE_LEFT_POWER_DEVICE;
        } else if (theDeviceData.isDistributedPower) {
            powerDeviceType = BIKE_RIGHT_POWER_DEVICE;
        }
        return powerDeviceType;
    }
    /**
     * Want to add a measurement characteristic to the end of our GattCharList depending on what DeviceType this is.
     * doesn't really matter what DeviceType this is, just return one of HEARTRATE_MEAS, BIKE_POWER_MEAS, or BIKESPD_CAD_MEAS
     * whichever we find in the list of characteristics this Device "knows"
     * @param theGattCharList a List of known Characteristics this device supports
     * @return the Measurement Characteristic, in the List, unique to this DeviceType
     *
    // 0x2A5B for SPD_CAD_MEASUREMENT
    // 0x2A64 for CYCLING_POWER_VECTOR to get crank cadence and wheel speed, if present, may not need this
    // 0x2A63 for CYCLING_POWER_MEASUREMENT, seems like it has Instantaneous power, speed and/or cadence if Present
    // 0x2A37 for HEARTRATE_MEASUREMENT
     */
    static BluetoothGattCharacteristic getMeasurementCharacteristicFromList(ArrayList<BluetoothGattCharacteristic> theGattCharList) {

        // Loops through available Characteristics
        for (BluetoothGattCharacteristic gattCharacteristic : theGattCharList) {
            // now see if we have heartrate measurement, bike_power_measurement or CSC_measurement uuids
            // we've used the "short-UUIDs" as keys to the DeviceType's
            int shortIntUUID = getShortUUID(gattCharacteristic);
            if (shortIntUUID == HEARTRATE_MEAS.intValue()
                    ||(shortIntUUID == CSC_MEAS.intValue())
                    || (shortIntUUID == BIKE_POWER_MEAS.intValue())){
                return gattCharacteristic;
            }
        }// loop over all characteristics in each Service
        return null;
    }

    static String composeDeviceDialogMessage(Context mContext, String deviceAddress, BLEDBAdapter dataBaseAdapter) {
        String otherMessage = "";
        String modelMessage = "";
        boolean deviceActive;
        String deviceName = mContext.getString(R.string.device_name_);
        String priorityString = mContext.getString(R.string.search_priority);
        String deviceActiveString = mContext.getString(R.string.device_active);
        String deviceNumString = mContext.getString(R.string.device_number_);
        Cursor mCursor = null;
        try {
            mCursor = dataBaseAdapter.fetchDeviceData(deviceAddress);
            if (mCursor != null) {
                String name = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_DEV_NAME));
                if (name == null || name.equals("")) {
                    name = "<" + deviceAddress + ">";
                }
                deviceName = mContext.getString(R.string.device_name_) + name;
                deviceNumString = mContext.getString(R.string.device_number_) + deviceAddress;
                otherMessage = dataBaseAdapter.getOtherMessage(mCursor);
                modelMessage = dataBaseAdapter.getModelMessage(mCursor);
                priorityString = mContext.getString(R.string.search_priority)
                        + mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SEARCH_PRIORITY));
                deviceActive = mCursor.getInt(mCursor.getColumnIndexOrThrow(DB_KEY_ACTIVE)) == 1;
                deviceActiveString = mContext.getString(R.string.device_active)
                        + (deviceActive ? mContext.getString(R.string.yes) : mContext.getString(R.string.no));
            }// if cursor not null
        } catch (IllegalArgumentException e) {
            Log.e(mContext.getClass().getName(), "IllegalArgumentException - " + e.toString());
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return priorityString + "\n" + deviceActiveString + "\n" + deviceName + "\n"
                + deviceNumString + "\n\n" + otherMessage + modelMessage;
    }

    static String composeDeviceDialogTitle(Context mContext, BLEDeviceType deviceType) {
        String returnString = "";
        switch (deviceType) {
            case BIKE_CADENCE_DEVICE:
                returnString = mContext.getString(R.string._cadence_sensor);
                break;
            case BIKE_POWER_DEVICE:
                returnString = mContext.getString(R.string._power_sensor);
                break;
            case BIKE_SPD_DEVICE:
                returnString = mContext.getString(R.string._speed_sensor);
                break;
            case BIKE_SPDCAD_DEVICE:
                returnString = mContext.getString(R.string._speed_cadence_sensor);
                break;
            case HEARTRATE_DEVICE:
                returnString = mContext.getString(R.string._heart_rate_sensor);
                break;
            default:
                break;
        }
        return returnString;
    }

    static String composeGPSDialogTitle(Context mContext) {
        return mContext.getString(R.string.location_status_title);
    }

    static String getStringFromByte(byte[] sn) {
        if (sn != null && sn.length > 0) {
            StringBuilder returnString = new StringBuilder(sn.length);
            for (byte byteChar : sn) {
                if (byteChar < 0x7e && byteChar > 0x20) {
                    returnString.append((char) byteChar);
                }
            }
            return returnString.toString();
        } else {
            return "";
        }

    }
    static int shiftBits(int flag, int shiftNum) {
        return flag>>>shiftNum;
    }

    static String byteToHexString(byte[] powerData) {
        StringBuilder stringBuilder = new StringBuilder();
        if (powerData != null && powerData.length > 0) {
            stringBuilder = new StringBuilder(powerData.length);
            for (byte byteChar : powerData) {
                stringBuilder.append(String.format("%02X ", byteChar));
            }
        }
        return stringBuilder.toString();
    }
    static UUID[] getServiceUUIDsByDeviceType(BLEDeviceType deviceType) {
        UUID[] mUUIDArray = null;
        switch (deviceType){
            case HEARTRATE_DEVICE:
                mUUIDArray = new UUID[]{UUID_HEARTRATE_SERVICE};
                break;
            case BIKE_POWER_DEVICE:
                mUUIDArray = new UUID[]{UUID_POWER_SERVICE};
                break;
            case BIKE_CADENCE_DEVICE:
                mUUIDArray = new UUID[]{UUID_CSC_SERVICE};
                break;
            case BIKE_SPD_DEVICE:
                mUUIDArray = new UUID[]{UUID_CSC_SERVICE};
                break;
            case BIKE_SPDCAD_DEVICE:
                mUUIDArray = new UUID[]{UUID_CSC_SERVICE};
                break;
        }
        return mUUIDArray;
    }
    static ArrayList<BluetoothGattCharacteristic> removeUnwantedGattCharacteristics(ArrayList<BluetoothGattCharacteristic> theGattCharList) {
        //step through the input list, removing unwanted Characteristics. (We'll add the measurement characteristics later)
        for (int index = theGattCharList.size() - 1; index >= 0; index--) {
            BluetoothGattCharacteristic aChar = theGattCharList.get(index);
            int shortUUID = getShortUUID(aChar);
            //Log.v("BLEUtilities", "looking at " + aChar.getUuid().toString() + " (int: " + shortUUID + ") in the list");
            BLEDataType type = BLEDataType.valueOf(shortUUID);
            if (SERVICE_CHANGED == type
                    || BODY_SENSOR_LOC == type
                    || APPEARANCE == type
                    || PREFERRED_CONNECTION_PARAMS == type
                    || SC_CONTROL_PT == type
                    || HR_CONTROL_PT == type
                    || UNKNOWN == type
                    || CSC_MEAS == type
                    || HEARTRATE_MEAS == type
                    || (BATTERY_LEVEL == type && !isCharacteristicReadable(aChar))
                    || BIKE_POWER_CONTROL_PT == type
                    || BIKE_POWER_MEAS == type) {
                theGattCharList.remove(index);
                //Log.v("BLEUtilities", "Removing " + type.name() + " from the list");
            }
        }
        return theGattCharList;
    }

    static int getShortUUID(BluetoothGattCharacteristic aChar) {
        return Integer.parseInt(aChar.getUuid().toString().substring(SHORT_UUID_BEGIN_INDEX, SHORT_UUID_END_INDEX), RADIX16);
    }

    /**
     * @return Returns <b>true</b> if property is writable
     */
    static boolean isCharacteristicWritable(BluetoothGattCharacteristic pChar) {
        return  pChar != null && (pChar.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }

    /**
     * @return Returns <b>true</b> if property is writable
     */
    static boolean isCharacteristicIndicatable(BluetoothGattCharacteristic pChar) {
        return pChar != null && (pChar.getProperties() & (BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0;
    }
    /**
     * @return Returns <b>true</b> if property is writable
     */
    public static boolean isDescriptorIndicationEnabled(BluetoothGattCharacteristic pChar) {
       byte[] mValue = pChar.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG)).getValue();
        return mValue != null && mValue[0] == ENABLE_INDICATION_VALUE[0];
    }

    /**
     * @return Returns <b>true</b> if property is Readable
     */
    static boolean isCharacteristicReadable(BluetoothGattCharacteristic pChar) {
        return ((pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }

    /**
     * @return Returns <b>true</b> if property is supports notification
     */
    static boolean isCharacterisiticNotifiable(BluetoothGattCharacteristic pChar) {
        return (pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }
    static IntentFilter makeGattDiscoveryIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEConstants.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    static IntentFilter makeGattHRMIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_HRM_GATT_CONNECTED);
        intentFilter.addAction(ACTION_HRM_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_HRM_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_HRM_DATA_AVAILABLE);
        return intentFilter;
    }

    static IntentFilter makeGattSpeedIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SPEED_GATT_CONNECTED);
        intentFilter.addAction(ACTION_SPEED_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_SPEED_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_SPEED_DATA_AVAILABLE);
        return intentFilter;
    }

    static IntentFilter makeGattCadenceIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_CAD_GATT_CONNECTED);
        intentFilter.addAction(ACTION_CAD_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_CAD_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_CAD_DATA_AVAILABLE);
        return intentFilter;
    }
    static IntentFilter makeGattPowerIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_POWER_GATT_CONNECTED);
        intentFilter.addAction(ACTION_POWER_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_POWER_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_POWER_DATA_AVAILABLE);
        return intentFilter;
    }
    static IntentFilter makeGattOppositePowerIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_OPPOSITE_POWER_GATT_CONNECTED);
        intentFilter.addAction(ACTION_OPPOSITE_POWER_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_OPPOSITE_POWER_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_OPPOSITE_POWER_DATA_AVAILABLE);
        return intentFilter;
    }


}
