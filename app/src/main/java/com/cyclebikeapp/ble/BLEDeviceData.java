package com.cyclebikeapp.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.ContentValues;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static com.cyclebikeapp.ble.BLEUtilities.getMeasurementCharacteristicFromList;
import static com.cyclebikeapp.ble.BLEUtilities.getShortUUID;
import static com.cyclebikeapp.ble.BLEUtilities.isCharacterisiticNotifiable;
import static com.cyclebikeapp.ble.BLEUtilities.isCharacteristicIndicatable;
import static com.cyclebikeapp.ble.BLEUtilities.isCharacteristicReadable;
import static com.cyclebikeapp.ble.BLEUtilities.isCharacteristicWritable;
import static com.cyclebikeapp.ble.Constants.DB_KEY_ACTIVE;
import static com.cyclebikeapp.ble.Constants.DB_KEY_BATT_STATUS;
import static com.cyclebikeapp.ble.Constants.DB_KEY_DEV_NAME;
import static com.cyclebikeapp.ble.Constants.DB_KEY_DEV_TYPE;
import static com.cyclebikeapp.ble.Constants.DB_KEY_MANUFACTURER;
import static com.cyclebikeapp.ble.Constants.DB_KEY_MODEL_NUM;
import static com.cyclebikeapp.ble.Constants.DB_KEY_POWER_CAL;
import static com.cyclebikeapp.ble.Constants.DB_KEY_SEARCH_PRIORITY;
import static com.cyclebikeapp.ble.Constants.DB_KEY_SERIAL_NUM;
import static com.cyclebikeapp.ble.Constants.DB_KEY_SOFTWARE_REV;

/**
 * Created by TommyD on 12/16/2016.
 * Container for important data about Bluetooth LE devices we discover.
 * mDeviceType from DeviceType enum
 * address is a string version of the MAC address obtained from BluetoothDevice.getAddress()
 * mUUIDList is an ArrayList<String> which is all the characteristic uuids we learn about each device
 * mDeviceName is readable Manufacturer's name
 */

class BLEDeviceData {

    private static String LOGTAG = "DeviceData";
    private final ContentValues data;
    private BLEDeviceType mBLEDeviceType;
    private String address;
    BLEDeviceStatus status;
    // keeps track of which characteristic we're reading from
    private int currentEnabledCharacteristic;
    boolean isDistributedPower;
    // power sensor crank length parameter is different than user spec
    boolean shouldWriteCrankLength;
    // need to check the power sensor crank parameter
    boolean shouldReadCrankLength;
    // power sensor is receptive to POWER_CONTROL_POINT commands
    boolean canReadCrankLength;
    private BLESensorLocation sensorLocation;
    private final ArrayList<BluetoothGattCharacteristic> characteristicList;
    // this is the previous characteristic that we should disable before enabling another one
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic powerControlPtCharacteristic;
    private BluetoothGattCharacteristic measurementCharacteristic;

    BLEDeviceData(String address, BLEDeviceType deviceType) {
        mBLEDeviceType = deviceType;
        this.address = address;
        sensorLocation = BLESensorLocation.OTHER;
        data = new ContentValues();
        characteristicList = new ArrayList<>();
        status = BLEDeviceStatus.UNRECOGNIZED;
        currentEnabledCharacteristic = 0;
        // set to true so Sensor Location information is preserved until we know if this is a distributed power
        // When we find out that it is not, we will eliminate sensor location info
        isDistributedPower = true;
        canReadCrankLength = false;
        shouldWriteCrankLength = false;
        // need to read crank length every time power meter is started
        shouldReadCrankLength = true;
    }
    BLEDeviceData(String address, BLEDeviceType deviceType, String name) {
        mBLEDeviceType = deviceType;
        this.address = address;
        sensorLocation = BLESensorLocation.OTHER;
        data = new ContentValues();
        data.put(DB_KEY_DEV_NAME, name);
        characteristicList = new ArrayList<>();
        status = BLEDeviceStatus.UNRECOGNIZED;
        currentEnabledCharacteristic = 0;
        // set to true so Sensor Location information is preserved until we know if this is a distributed power
        // When we find out that it is not, we will eliminate sensor location info
        isDistributedPower = true;
        canReadCrankLength = false;
        shouldWriteCrankLength = false;
        // need to read crank length every time power meter is started
        shouldReadCrankLength = true;
        LOGTAG = this.getClass().getSimpleName();
    }
    /**
     * Take the discovered UUIDs and deliver them to DeviceData.
     * Find out what measurement we want running continuously and add that to the end of the list
     * @param gattServices the characteristics we've discovered
     */
    BluetoothGattCharacteristic hasSpdCadFeature(List<BluetoothGattService> gattServices) {
        Log.i(LOGTAG, "hasSpdCadFeature()");
        if (gattServices == null){
            Log.e(LOGTAG, "No GattServices discovered");
            return null;
        }
        for (BluetoothGattService gattService : gattServices) {
            if (MainActivity.debugBLEData)Log.i(LOGTAG, "Looking at gattService " + gattService.getUuid().toString());
            for (BluetoothGattCharacteristic aCharacteristic: gattService.getCharacteristics()){
                if (getShortUUID(aCharacteristic) == BLEDataType.BIKE_SPD_CAD_FEATURE.intValue()){
                    if (MainActivity.debugBLEData)Log.i(LOGTAG, "Discovered BIKE_SPD_CAD_FEATURE characteristic");
                    return aCharacteristic;
                }
            }
        }//loop over Services
        return null;
    }
    /**
     * Take the discovered UUIDs and deliver them to DeviceData.
     * Find out what measurement we want running continuously and add that to the end of the list
     * @param gattServices the characteristics we've discovered
     */
    void updateDeviceDataGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        //first clear the measurement list in case we've re-acquired
        clearCharacteristicList();
        //also reset characteristic #
        currentEnabledCharacteristic = 0;
        // clear the previous NotifySpeedDescriptor
        mNotifyCharacteristic = null;
        if (MainActivity.debugBLEData)Log.i(LOGTAG, "updateDeviceDataGattServices() Device: " + mBLEDeviceType.name());
        ArrayList<BluetoothGattCharacteristic> theGattCharList = new ArrayList<>();
        for (BluetoothGattService gattService : gattServices) {
            // Loops through available Characteristics
            for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                theGattCharList.add(gattCharacteristic);
                //extract PowerControlPt characteristic from the list and save it
                // we use this for calibration, crank length read & write, etc.
                if (BLEDataType.BIKE_POWER_CONTROL_PT == BLEDataType.valueOf(getShortUUID(gattCharacteristic))){
                    powerControlPtCharacteristic = gattCharacteristic;
                }
            }// loop over all characteristics in each Service
        }//loop over Services
        logCharacteristicList(theGattCharList, "before removing unwanted");
        // save the desired measurementCharacteristic to later add to the end of the List
        measurementCharacteristic = getMeasurementCharacteristicFromList(theGattCharList);
        // this operates on the characteristicList in theDevice structure
        //go through the List, remove any Measurement, ServiceConnected, etc chars we don't want
        addAllCharacteristicToList(BLEUtilities.removeUnwantedGattCharacteristics(theGattCharList));
        addMeasurementCharacteristicToList(measurementCharacteristic);
        logCharacteristicList(characteristicList, "after removing unwanted");
    }

    /**
     * We'll use this function to disable the current characteristic before enabling the next one.
     * Only one characteristic can be enabled at a time, so we have to step thru the list when GATT is connected.
     * If we haven't yet enabled the first characteristic there is nothing to disable, so return "".
     * Likewise, if the last characteristic, the continuous measurement one, is enabled, we don't want to diable that
     * so return "";
     *
     * @return String short uuid of the currently enabled characteristic
     */
    BluetoothGattCharacteristic getNextCharacteristic() {
        if (MainActivity.debugBLEData)Log.i(LOGTAG, "getNextCharacteristic() characteristic #: " + currentEnabledCharacteristic);
        // if this is the last characteristic, return null so we don't disable the measurement one
        BluetoothGattCharacteristic nextCharacteristic = null;
        if (currentEnabledCharacteristic >= 0 && currentEnabledCharacteristic < characteristicList.size()) {
            nextCharacteristic = characteristicList.get(currentEnabledCharacteristic);
        }
        // now increment currentEnabled, so we return next one on the next call
        currentEnabledCharacteristic++;
        String gattCharString = "";
        if (nextCharacteristic != null) {
            gattCharString = nextCharacteristic.getUuid().toString();
        }
        if (MainActivity.debugBLEData)Log.i(LOGTAG, "BluetoothGattCharacteristic: " + gattCharString);
        return nextCharacteristic;
    }

    /**
     * We'll add this list here so we know which characteristics this DeviceData understands.
     * I think it would be bad to enable a characteristic a GATT doesn't know.
     *
     * @param gattCharacteristics is a list of UUID Strings we've obtained when onServicesDiscovered() is called.
     */
    private void addAllCharacteristicToList(ArrayList<BluetoothGattCharacteristic> gattCharacteristics) {
        if (MainActivity.debugBLEData)Log.i(LOGTAG, "addAllCharacteristicToList() Device: " + mBLEDeviceType.name());
        characteristicList.addAll(gattCharacteristics);
        // for debugging, list all characteristics
        if (characteristicList.size() > 0) {
            for (BluetoothGattCharacteristic aCharacteristic : characteristicList) {
                if (MainActivity.debugBLEData)Log.i(LOGTAG, "BluetoothGattCharacteristic: " + aCharacteristic.getUuid());
            }
        }
    }

    /**
     * After adding all known characteristics found in onServicesDiscovered(),
     * we'll want to add the measurement UUID to the end of the list, so the measurement will run continuously.
     * The calling function will have to figure out what that characteristic is, based on the DeviceType, etc.
     *
     * @param gattCharacteristic the UUID of a characteristic we want to add to the known List
     */
    private void addMeasurementCharacteristicToList(BluetoothGattCharacteristic gattCharacteristic) {
        if (MainActivity.debugBLEData)Log.i(LOGTAG, "addMeasurementCharacteristicToList() Device: " + mBLEDeviceType.name());
        characteristicList.add(gattCharacteristic);
        // for debugging, list all characteristics
        if (characteristicList.size() > 0) {
            for (BluetoothGattCharacteristic aCharacteristic : characteristicList) {
                if (MainActivity.debugBLEData)Log.i(LOGTAG, "BluetoothGattCharacteristic: " + aCharacteristic.getUuid());
            }
        }
    }

    BLEDeviceType getDeviceType() {
        return mBLEDeviceType;
    }
    private void clearCharacteristicList() {
        characteristicList.clear();
    }
    void setDeviceType(BLEDeviceType mBLEDeviceType) {
        //Log.i(LOGTAG, "setDeviceType: " + mBLEDeviceType.name());
        this.mBLEDeviceType = mBLEDeviceType;
        data.put(DB_KEY_DEV_TYPE, mBLEDeviceType.intValue());
    }

    String getAddress() {
        return address;
    }

    void setAddress(String address) {
        this.address = address;
    }

    public ContentValues getData() {
        return data;
    }

    public void setData(ContentValues data) {
        this.data.putAll(data);
    }

    private void logCharacteristicList(ArrayList<BluetoothGattCharacteristic> theList, String source) {
        StringBuilder outputString = new StringBuilder();
        for (BluetoothGattCharacteristic gattCharacteristic : theList) {
            // now see if we have heartrate measurement, bike_power_measurement or CSC_measurement uuids
            // we've used the "short-UUIDs" as keys to the DeviceType's
            int shortIntUUID = getShortUUID(gattCharacteristic);
            outputString.append(BLEDataType.valueOf(shortIntUUID).name()).append(" Descriptors {");
            // also list all descriptors for each characteristic
            List<BluetoothGattDescriptor> descriptors = gattCharacteristic.getDescriptors();
            for (BluetoothGattDescriptor aDescriptor:descriptors){
                outputString.append(aDescriptor.getUuid().toString()).append(", ");
            }
            outputString.append("} ");
            if (isCharacterisiticNotifiable(gattCharacteristic)){
                outputString.append("(Notifiable)");
            }
            if (isCharacteristicReadable(gattCharacteristic)){
                outputString.append("(Readable)");
            }
            if (isCharacteristicWritable(gattCharacteristic)){
                outputString.append("(Writeable)");
            }
            if (isCharacteristicIndicatable(gattCharacteristic)){
                outputString.append("(Indicatable)");
            }
            outputString.append("\n");
        }
        if (MainActivity.debugBLEData){
            Log.i(this.getClass().getSimpleName(), "known characteristics for "
                    + mBLEDeviceType.name() + " " + outputString);
        }
    }
    void logData(boolean brief) {
        int deviceActive = 0;
        int searchPriority = 0;
        int devType = 5;
        String upTime = "";
        String battVolts = "";
        String battStatus = "";
        //+ DB_KEY_DEV_NAME + TEXT_NOT_NULL + ", "
        String devName = data.getAsString(DB_KEY_DEV_NAME);
        String serialNum = "";
        String manufacturer = "";
        String softwareRev = "";
        String modelNum = "";
        String powerCal = "";
        //+ DB_KEY_DEV_TYPE + INTEGER_NOT_NULL + ", "
        if (!brief) {
            devType = data.getAsInteger(DB_KEY_DEV_TYPE);
            //+ DB_KEY_BATT_STATUS + TEXT_NOT_NULL + ", "
            battStatus = data.getAsString(DB_KEY_BATT_STATUS);
            //+ DB_KEY_SERIAL_NUM + TEXT_NOT_NULL + ", "
            serialNum = data.getAsString(DB_KEY_SERIAL_NUM);
            //+ DB_KEY_MANUFACTURER + TEXT_NOT_NULL + ", "
            manufacturer = data.getAsString(DB_KEY_MANUFACTURER);
            //+ DB_KEY_SOFTWARE_REV + TEXT_NOT_NULL + ", "
            softwareRev = data.getAsString(DB_KEY_SOFTWARE_REV);
            //+ DB_KEY_MODEL_NUM + TEXT_NOT_NULL + ", "
            modelNum = data.getAsString(DB_KEY_MODEL_NUM);
            //+ DB_KEY_POWER_CAL + TEXT_NOT_NULL + ", "
            powerCal = data.getAsString(DB_KEY_POWER_CAL);
            //+ DB_KEY_SEARCH_PRIORITY + INTEGER_NOT_NULL + ", "
            searchPriority = data.getAsInteger(DB_KEY_SEARCH_PRIORITY);
            //+ DB_KEY_ACTIVE + INTEGER_NOT_NULL + ");";
            deviceActive = data.getAsInteger(DB_KEY_ACTIVE);
        }
        if (MainActivity.debugBLEData){
            Log.i(this.getClass().getSimpleName(), "logData()" + address + ", " + devName + ", " + BLEDeviceType.valueOf(devType).name()
                    + ", " + battVolts + ", " + battStatus + ", " +
                    serialNum + ", " + manufacturer + ", " + softwareRev + ", " + modelNum + ", " +
                    powerCal + ", " + searchPriority + ", " + upTime + ", " + deviceActive);
        }
    }

    BluetoothGattCharacteristic getNotifyCharacteristic() {
        return mNotifyCharacteristic;
    }

    void setNotifyCharacteristic(BluetoothGattCharacteristic notifyCharacteristic) {
        this.mNotifyCharacteristic = notifyCharacteristic;
    }
    BluetoothGattCharacteristic getMeasurementCharacteristic() {
        return measurementCharacteristic;
    }
    BluetoothGattCharacteristic getPowerControlPtCharacteristic() {
        return powerControlPtCharacteristic;
    }

    BLESensorLocation getSensorLocation() {
        return sensorLocation;
    }

    void setSensorLocation(BLESensorLocation sensorLocation) {
        this.sensorLocation = sensorLocation;
    }
}
