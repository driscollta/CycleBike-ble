/*
 * Copyright 2010 Dynastream Innovations Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.cyclebikeapp.ble;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;

import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_CADENCE_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_LEFT_POWER_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_POWER_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_RIGHT_POWER_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_SPDCAD_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_SPD_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.UNKNOWN_DEVICE;
import static com.cyclebikeapp.ble.Constants.AVG_HR;
import static com.cyclebikeapp.ble.Constants.CUM_ENERGY;
import static com.cyclebikeapp.ble.Constants.CUM_POWER_TIME;
import static com.cyclebikeapp.ble.Constants.DB_KEY_ACTIVE;
import static com.cyclebikeapp.ble.Constants.DB_KEY_DEV_NAME;
import static com.cyclebikeapp.ble.Constants.DB_KEY_DEV_TYPE;
import static com.cyclebikeapp.ble.Constants.DB_KEY_SEARCH_PRIORITY;
import static com.cyclebikeapp.ble.Constants.KEY_AUTO_CONNECT_ALL;
import static com.cyclebikeapp.ble.Constants.MAX_HR;
import static com.cyclebikeapp.ble.Constants.NUM_CALC_CAD;
import static com.cyclebikeapp.ble.Constants.NUM_HR_EVENTS;
import static com.cyclebikeapp.ble.Constants.NUM_PEDAL_CAD;
import static com.cyclebikeapp.ble.Constants.PED_CNTS_INIT;
import static com.cyclebikeapp.ble.Constants.POWER_CNTS_INIT;
import static com.cyclebikeapp.ble.Constants.POWER_WHEEL_IS_CAL;
import static com.cyclebikeapp.ble.Constants.PREFS_NAME;
import static com.cyclebikeapp.ble.Constants.TOTAL_CALC_CAD;
import static com.cyclebikeapp.ble.Constants.TOTAL_HR_COUNTS;
import static com.cyclebikeapp.ble.Constants.TOTAL_PEDAL_CAD;
import static com.cyclebikeapp.ble.Constants.WHEEL_IS_CAL;


/**
 * This class handles setting up the channels,
 * and processing Ant events.
 */
class BLEDeviceManager {

	/** structure for combining all wheel sensor data */
    final SpeedCadenceCounts wheelCnts = new SpeedCadenceCounts();
    /** structure for combining all crank cadence sensor data */
    final SpeedCadenceCounts crankCadenceCnts = new SpeedCadenceCounts();
    /** structure for combining all crank cadence sensor data in distributed power sensor*/
    final SpeedCadenceCounts oppositeCrankCadenceCnts = new SpeedCadenceCounts();
    /** structure for combining all cadence sensor data */
    final SpeedCadenceCounts pedalCadenceCnts = new SpeedCadenceCounts();
    /** structure for combining all power meter calculated power sensor data in distributed power sensor */
    final SpeedCadenceCounts oppositeCalcPowerData = new SpeedCadenceCounts();
    /** structure for combining all power meter calculated power sensor data */
    final SpeedCadenceCounts calcPowerData = new SpeedCadenceCounts();
    /** structure for combining all PowerTap wheel sensor data */
    final SpeedCadenceCounts powerWheelCnts = new SpeedCadenceCounts();
	final SpeedCadenceCounts hrData= new SpeedCadenceCounts();

    private double powerTime = .1;
	private double totalEnergy = 0;
	private long totalCalcCrankCad = 0;
	private long numCalcCrankCad = 0;
	private long totalPedalCad = 0;
	private long numPedalCad = 0;
	private long totalHRCounts = 0;
	private long numHREvents = 0;
    private BLEScannerStatus mBLEScannerStatus;
    // list of bluetooth Bike Devices we've heard in LE Scan
    final ArrayList<BLEDeviceData> bleActiveBikeDeviceList;
    // List of other bluetooth devices we've heard in LE Scan
	final ArrayList<BLEDeviceData> bleOtherDeviceList;
    private final SharedPreferences.Editor editor;
    private final SharedPreferences settings;
    private static String logtag = null;

    /**
     * Default Constructor
	 * @param context the app context
     */
    @SuppressLint("CommitPrefEdits")
    BLEDeviceManager(Context context) {
        mBLEScannerStatus = BLEScannerStatus.DEAD;
   	 	bleActiveBikeDeviceList = new ArrayList<>();
		bleOtherDeviceList = new ArrayList<>();
        settings = context.getSharedPreferences(PREFS_NAME, 0);
        editor = settings.edit();
        logtag = this.getClass().getSimpleName();
    }

	/**
	 * Test if a device is already in the bleActiveBikeDeviceList ArrayList
	 * 
	 * @param bleDeviceAddress
	 *            the String device address to look for
	 * @return true if the device address is already in the list
	 */
	boolean isDeviceInActiveBikeList(String bleDeviceAddress) {
        //Log.i(logtag, "isDeviceInActiveBikeList; bleDevice address: " + bleDeviceAddress);
        //logActiveBikeDeviceListData("isDeviceInActiveBikeList");
		for (BLEDeviceData activeDevice: bleActiveBikeDeviceList) {
			if ((bleDeviceAddress).equals(activeDevice.getAddress())) {
                //Log.e(logtag, "Device is already in DBActiveList");
				return true;
			}
		}
		return false;
	}

	/**
	 * Test if a device is already in the bleActiveBikeDeviceList ArrayList
	 *
	 * @param bleDeviceAddress
	 *            the String device address to look for
	 * @return true if the device number is already in the list
	 */
	boolean isDeviceInOtherActiveList(String bleDeviceAddress) {
        //Log.i(logtag, "isDeviceInOtherActiveList; bleDevice address: " + bleDeviceAddress);
        //logActiveOtherDeviceListData("isDeviceInOtherActiveList");
		for (BLEDeviceData otherDevice: bleOtherDeviceList) {
            if ((bleDeviceAddress).equals(otherDevice.getAddress())) {
                return true;
            }
		}
		return false;
	}

	/** Get requested content by specifying index to ArrayList
	 * @param index is the entry number in the ArrayList
	 * @return BLEDeviceData containing the device data or null if index is greater than ArrayList size*/
	BLEDeviceData getActiveBikeDeviceData(int index) {
		// find index to ArrayList using deviceNum
		if (index < bleActiveBikeDeviceList.size()) {
			return bleActiveBikeDeviceList.get(index);
		} else {
			return null;
		}
	}

	/** Get a list of *active* devices by DeviceType. The DB list is populated by reading the database
	 * Must check to make sure the device is active.
	 * @param bleDeviceType DeviceType of the BLE device
	 * @return BLEDeviceData List containing the device data*/
	ArrayList<BLEDeviceData> getActiveBLEDeviceDBDataByDeviceType(BLEDeviceType bleDeviceType) {
		ArrayList<BLEDeviceData> activeDeviceListByType = new ArrayList<>();
		for (BLEDeviceData deviceData: bleActiveBikeDeviceList) {
            //Log.i(logtag, "getActiveBLEDeviceDBDataByDeviceType() -deviceData type: " + deviceData.getDeviceType().name() + " desiredType: " + bleDeviceType.name());
            //Log.i(logtag,"getActiveBLEDeviceDBDataByDeviceType() -deviceActive? " + (deviceData.getData().getAsInteger(DB_KEY_ACTIVE) == 1?"yes":"no"));
            //deviceData.logData(false);
            if (deviceData.getData().getAsInteger(DB_KEY_ACTIVE) == 1) {
                if (deviceData.getDeviceType() == bleDeviceType) {
                    //deviceData.logData(false);
                    activeDeviceListByType.add(deviceData);
                }
                // if we're looking for SPD or CAD device and this device in the active list is a SPD_CAD device, declare it as a match
                if ((bleDeviceType == BIKE_SPD_DEVICE || bleDeviceType == BIKE_CADENCE_DEVICE)
                        && deviceData.getDeviceType() == BIKE_SPDCAD_DEVICE){
                    //Log.i(logtag, "searching for SPD, found SPD_CAD");
                    //deviceData.logData(false);
                    activeDeviceListByType.add(deviceData);
                }
                //if looking for BIKE_POWER_DEVICE, also declare BIKE_LEFT_POWER_DEVICE & BIKE_RIGHT_POWER_DEVICE as a match
                if (bleDeviceType == BIKE_POWER_DEVICE
                        && (deviceData.getDeviceType() == BIKE_LEFT_POWER_DEVICE
                            || deviceData.getDeviceType() == BIKE_RIGHT_POWER_DEVICE)){
                    activeDeviceListByType.add(deviceData);
                }
            }
		}
		return activeDeviceListByType;
	}

	/** When new data is received about one of the active database BLE devices, update the content
	 * @param bleDeviceAddress device String address of the BLE device
	 * @param data ContentValues containing the new data
	 */
	void updateActiveBikeDeviceData(String bleDeviceAddress, ContentValues data) {
		// find index to ArrayList using deviceNum
        if (data == null || bleDeviceAddress == null){
            return;
        }
        if (bleActiveBikeDeviceList == null || bleActiveBikeDeviceList.size() == 0){
            return;
        }
		int i = 0;
		for (BLEDeviceData deviceData: bleActiveBikeDeviceList) {
			if ((bleDeviceAddress).equals(deviceData.getAddress())) {
				deviceData.setData(data);
				bleActiveBikeDeviceList.set(i, deviceData);
			}
			i++;
		}
	}

	/**
	 * called from addDeviceToActiveList from Discovery; sets the active status of device bleDeviceAddress
	 * @param bleDeviceAddress device String address of the BLE device to set status of
	 * @param activeStatus the status
	 */
	void setDevAddressActiveStatus(String bleDeviceAddress, int activeStatus) {
		ContentValues data = new ContentValues();
		data.put(DB_KEY_ACTIVE, activeStatus);
        updateActiveBikeDeviceData(bleDeviceAddress, data);
        if (MainActivity.debugLEScan){logActiveBikeDeviceListData("updating active status");}
	}

    /**
     * called from addDeviceToActiveList from Discovery; sets the active status of device bleDeviceAddress
     * @param bleDeviceAddress device String address of the BLE device to set status of
     * @return  activeStatus the status 0 = inactive, 1 = active
     */
    int getDevAddressActiveStatus(String bleDeviceAddress) {
        // find index to ArrayList using bleDeviceAddress
        int activeStatus = 0;
        for (BLEDeviceData deviceData: bleActiveBikeDeviceList) {
            if ((bleDeviceAddress).equals(deviceData.getAddress())) {
                activeStatus = deviceData.getData().getAsInteger(DB_KEY_ACTIVE);
            }
        }
        return activeStatus;
        //logActiveBikeDeviceListData("updating active status");
    }

    String getDevAddressName(String bleDeviceAddress) {
        String deviceName = "";
        for (BLEDeviceData deviceData: bleActiveBikeDeviceList) {
            if ((bleDeviceAddress).equals(deviceData.getAddress())) {
                deviceName = deviceData.getData().getAsString(DB_KEY_DEV_NAME);
            }
        }
        return deviceName;
    }

    BLEDeviceType getDevAddressType(String bleDeviceAddress) {
        BLEDeviceType theDeviceType = UNKNOWN_DEVICE;
        for (BLEDeviceData deviceData: bleActiveBikeDeviceList) {
            if ((bleDeviceAddress).equals(deviceData.getAddress())) {
                theDeviceType =  BLEDeviceType.valueOf(deviceData.getData().getAsInteger(DB_KEY_DEV_TYPE));
            }
        }
        return theDeviceType;
    }

    void setDevAddressType(String bleDeviceAddress, BLEDeviceType deviceType) {
        for (BLEDeviceData deviceData: bleActiveBikeDeviceList) {
            if ((bleDeviceAddress).equals(deviceData.getAddress())) {
                deviceData.setDeviceType(deviceType);
            }
        }
    }
	/**
	 * Reset active status for all DeviceType when starting a new LE scan
	 * @param bleDeviceType DeviceType of the BLE device
	*/
	void resetActiveStatusByType(BLEDeviceType bleDeviceType) {
		int i = 0;
		ContentValues data = new ContentValues();
		data.put(DB_KEY_ACTIVE, 0);
		for (BLEDeviceData deviceData: bleActiveBikeDeviceList) {
            BLEDeviceType activeDeviceType = deviceData.getDeviceType();
            // include LEFT_ and RIGHT_ power devices in resetting active status by temporarily declaring them to be BIKE_POWER_DEVICES
            if (bleDeviceType == BIKE_POWER_DEVICE
                    && (activeDeviceType == BIKE_LEFT_POWER_DEVICE || activeDeviceType == BIKE_RIGHT_POWER_DEVICE)){
                activeDeviceType = BIKE_POWER_DEVICE;
            }
			if (activeDeviceType == bleDeviceType) {
				deviceData.setData(data);
				bleActiveBikeDeviceList.set(i, deviceData);
			}
			i++;
		}
	}
    /**
     * Reset active status for all devices when starting a new LE scan
     */
    void resetActiveStatusAll() {
        int i = 0;
        ContentValues data = new ContentValues();
        data.put(DB_KEY_ACTIVE, 0);
        for (BLEDeviceData deviceData: bleActiveBikeDeviceList) {
            deviceData.setData(data);
            bleActiveBikeDeviceList.set(i, deviceData);
            i++;
        }
        //logActiveBikeDeviceListData("reset all active status");
    }
	/** For debugging, log all device data in the activeDeviceList 
	 * @param string an indication of where we called this from for debugging*/
    void logActiveBikeDeviceListData(String string) {
		Log.i(logtag, "logActiveBikeDeviceListData - " + string);
		for (BLEDeviceData deviceData: bleActiveBikeDeviceList) {
			deviceData.logData(false);
		}
	}
	/** For debugging, log all device data in the activeDeviceList
	 * @param string an indication of where we called this from for debugging*/
    void logActiveOtherDeviceListData(String string) {
		Log.i(logtag, string);
		for (BLEDeviceData deviceData: bleOtherDeviceList) {
			deviceData.logData(true);
		}
	}

    /**
     * We've just acquired communication with a device. It has been added to the
     * bleActiveBikeDeviceList with search priority 1.
     * Down-grade all other active database devices of the same type by increasing the search priority.
     * Devices in the bleActiveBikeDeviceList may not be in the database, if search priority is < 0
     * @param bleDeviceAddress the device address of the newly acquired device
     * @param devType the DeviceType of the newly acquired device
     */
    void resetSearchPriority(String bleDeviceAddress, BLEDeviceType devType) {
        int i = 0;
        if (MainActivity.debugLEScan)logActiveBikeDeviceListData("before resetSearchPriority");
        for (BLEDeviceData activeDevice: bleActiveBikeDeviceList) {
            String activeDeviceAddress = activeDevice.getAddress();
            BLEDeviceType activeDeviceType = activeDevice.getDeviceType();
            // include LEFT_ and RIGHT_ power devices in downgrading search priority by temporarilly declaring them to be BIKE_POWER_DEVICES
            if (devType == BIKE_POWER_DEVICE
                    && (activeDeviceType == BIKE_LEFT_POWER_DEVICE || activeDeviceType == BIKE_RIGHT_POWER_DEVICE)){
                activeDeviceType = BIKE_POWER_DEVICE;
            }
            if (activeDeviceType == devType && !activeDeviceAddress.equals(bleDeviceAddress)) {
                int searchPriority = activeDevice.getData().getAsInteger(DB_KEY_SEARCH_PRIORITY);
                ContentValues content = new ContentValues();
                if (searchPriority > 0) {
                    if (MainActivity.debugLEScan){
                        Log.i(logtag, "devAddress = " + activeDeviceAddress + " increasing searchPriority " + searchPriority);
                    }
                    content.put(DB_KEY_SEARCH_PRIORITY, searchPriority + 1);
                    activeDevice.setData(content);
                    bleActiveBikeDeviceList.set(i, activeDevice);
                }
            }
            i++;
        }
        if (MainActivity.debugLEScan)logActiveBikeDeviceListData("after resetSearchPriority");
    }

    void restartWheelCal(Double wheelTripDistance) {
        editor.putBoolean(WHEEL_IS_CAL, false).apply();
        wheelCnts.isCalibrated = false;
        wheelCnts.calTotalCount = 0;
        // if we're restarting because GPS dropped out, change GPS StartDistance
        wheelCnts.calGPSStartDist = wheelTripDistance;
        wheelCnts.cumulativeRevsAtCalStart = wheelCnts.cumulativeRevolutions;
    }

    void restartPowerWheelCal(Double wheelTripDistance) {
        editor.putBoolean(POWER_WHEEL_IS_CAL, false).apply();
        powerWheelCnts.isCalibrated = false;
        powerWheelCnts.calTotalCount = 0;
        // if we're restarting because GPS dropped out, change GPS StartDistance
        powerWheelCnts.calGPSStartDist = wheelTripDistance;
        powerWheelCnts.cumulativeRevsAtCalStart = powerWheelCnts.cumulativeRevolutions;
    }

    void restartHR(int avgHeartRate, int maxHeartRate) {
        setNumHREvents(0);
        setTotalHRCounts(0);

        editor.putInt(TOTAL_HR_COUNTS, (int) getTotalHRCounts());
        editor.putInt(NUM_HR_EVENTS, (int) getNumHREvents());
        editor.putInt(AVG_HR, avgHeartRate);
        editor.putInt(MAX_HR, maxHeartRate).apply();
    }

    void restartPower() {
        calcPowerData.initialized = false;
        oppositeCalcPowerData.initialized = false;
        setCumEnergy(0);
        setCumPowerTime(0);

        editor.putBoolean(POWER_CNTS_INIT, false);
        editor.putInt(CUM_ENERGY, 0);
        editor.putString(CUM_POWER_TIME, Double.toString(0.0)).apply();
    }

    void restartCadence() {
        oppositeCrankCadenceCnts.initialized = false;
        crankCadenceCnts.initialized = false;
        pedalCadenceCnts.initialized = false;
        setNumPedalCad(0);
        setTotalPedalCad(0);
        setNumCalcCrankCad(0);
        setTotalCalcCrankCad(0);

        editor.putBoolean(PED_CNTS_INIT, false);
        editor.putInt(NUM_PEDAL_CAD, 0);
        editor.putInt(NUM_CALC_CAD, 0);
        editor.putInt(TOTAL_PEDAL_CAD, 0);
        editor.putInt(TOTAL_CALC_CAD, 0).apply();
    }


	void addCumEnergy(double d) {
		this.totalEnergy += d;		
	}

	void setCumEnergy(long energy) {
		this.totalEnergy = energy;		
	}

	double getCumEnergy() {
		return this.totalEnergy;		
	}

	void addCumPowerTime(double deltaT) {
		this.powerTime += deltaT;
	}

	void setCumPowerTime(double time) {
		this.powerTime = time;
	}

	double getCumPowerTime() {
		return powerTime;
	}

	long getTotalCalcCrankCad() {
		return totalCalcCrankCad;
	}

	void addTotalCalcCrankCad(long newCalcCrankCad) {
		this.totalCalcCrankCad += newCalcCrankCad;
	}

	long getNumCalcCrankCad() {
		return numCalcCrankCad;
	}

	void addNumCalcCrankCad() {
		this.numCalcCrankCad++;
	}

	void setNumCalcCrankCad(int int1) {
		this.numCalcCrankCad = int1;		
	}

	void setTotalCalcCrankCad(int int1) {
		this.totalCalcCrankCad = int1;
	}

	long getTotalPedalCad() {
		return totalPedalCad;
	}

	void setTotalPedalCad(long totalPedalCad) {
		this.totalPedalCad = totalPedalCad;
	}

	void addTotalPedalCad(long pedalCad) {
		this.totalPedalCad += pedalCad;
	}

	long getNumPedalCad() {
		return numPedalCad;
	}
	long addNumPedalCad() {
		return numPedalCad++;
	}

	void setNumPedalCad(long numPedalCad) {
		this.numPedalCad = numPedalCad;
	}

	long getTotalHRCounts() {
		return totalHRCounts;
	}

	long addTotalHRCounts(long hrCount) {
		return totalHRCounts += hrCount;
	}

	void setTotalHRCounts(long totalHRCounts) {
		this.totalHRCounts = totalHRCounts;
	}

	long getNumHREvents() {
		return numHREvents;
	}

	long addNumHREvents() {
		return numHREvents++;
	}

	void setNumHREvents(long num) {
		this.numHREvents = num;
	}

    /**
     * Returns the device number of a particular device type in the activeDeviceList
     * whether it is active or not. Used to see if a distributed power sensor is in the database. And if it is active
     *
     * @param devType device type we're looking for
     * @return device address to connect
     */
    String getAllDevInfoByType(BLEDeviceType devType) {
        int devTypeNum = devType.intValue();
        Log.i(logtag, "getting highest priority " + devType.name());
        int priority = 1000;
        String devAddress = null;
        // find minimum priority device in activeList
        // we store DeviceType as an integer
        for (BLEDeviceData anyDevice : bleActiveBikeDeviceList) {
            Log.i(logtag, "looking at device " + anyDevice.getData().get(DB_KEY_DEV_NAME) + " type: " + BLEDeviceType.valueOf(anyDevice.getData().getAsInteger(DB_KEY_DEV_TYPE)));
            if (anyDevice.getData().getAsInteger(DB_KEY_DEV_TYPE) == devTypeNum) {
                int devicePriority = anyDevice.getData().getAsInteger(DB_KEY_SEARCH_PRIORITY);
                if (devicePriority < priority && devicePriority > 0) {
                    priority = devicePriority;
                    devAddress = anyDevice.getAddress();
                }
            }
        }
        Log.i(logtag, "devAddress = " + (devAddress != null?devAddress:"null"));
        return devAddress;
    }
    /**
     * Called from autoConnect_ to get the address of the highest priority sensor of the desired type
     * Returns the device number of a particular device type in the activeDeviceList
     * having highest search priority. If autoConnectAll == false, only return device number
     * if search priority is 1, which indicates we have last connected to this device.
     *
     * @param devType device type we're looking for
     * @return device address to connect
     */
    String getActiveDevInfoByType(BLEDeviceType devType) {
        int devTypeNum = devType.intValue();
        //Log.i(logtag, "getting highest priority: " + devType.name());
        int priority = 1000;
        String devAddress = null;
        // find minimum priority device in activeList
        for (BLEDeviceData activeDevice : bleActiveBikeDeviceList) {
            int intActiveDeviceType = activeDevice.getData().getAsInteger(DB_KEY_DEV_TYPE);
            // looking to autoConnect BIKE_POWER sensor, also autoConnect to LEFT_ sensors
            if (devType == BIKE_POWER_DEVICE
                    && (intActiveDeviceType == BIKE_LEFT_POWER_DEVICE.intValue())) {
                intActiveDeviceType = BIKE_POWER_DEVICE.intValue();
            }
            if (intActiveDeviceType == devTypeNum
                    && activeDevice.getData().getAsInteger(DB_KEY_ACTIVE) == 1) {
                int activeDevicePriority = activeDevice.getData().getAsInteger(DB_KEY_SEARCH_PRIORITY);
                if (activeDevicePriority < priority && activeDevicePriority > 0) {
                    priority = activeDevicePriority;
                    devAddress = activeDevice.getAddress();
                }
            }
        }
        // If we're only connecting to the last device, priority will be 1.
        // If highest priority device doesn't have priority 1 return null
        // Get SharedPrefs for autoConnectAll, if it's false and priority>1 set devInfo null
        // demand priority be >= 1 since BIKEDEVICEs that are not in DB have priority = -1
		boolean autoConnectAll = settings.getBoolean(KEY_AUTO_CONNECT_ALL, false);
        if (!autoConnectAll && (priority > 1)){
            devAddress = null;
		}
        return devAddress;
    }

    /**
     * On Discovery of a new BIKE_xxx_DEVICE, either add it to the list if it was not in the database;
     * or update the status if it was. We could just update the active status, or, if its a SPDCAD Devicee
     * we're updating the DeviceType on further Discovery
     * @param newDevice data describing the new BLE device we've discovered:
     *                  Content values with DeviceType, active status, search priority, address
     *
     */
    void addToBLEBikeDeviceList(BLEDeviceData newDevice) {
        if (isDeviceInActiveBikeList(newDevice.getAddress())) {
            updateActiveBikeDeviceData(newDevice.getAddress(), newDevice.getData());
        } else {
            bleActiveBikeDeviceList.add(newDevice);
        }
        if (MainActivity.debugLEScan) logActiveBikeDeviceListData("addToBLEBikeDeviceList()");
	}
    /**
     * On Discovery of a new UNKNOWN Device or NOT_BIKE_DEVICE, just add the address and DeviceType so we know what it is.
     * We won't use any other data, just identify the Device by its address
     * @param newDevice data describing the new BLE device we've discovered
     *
     */
    void addToBLEOtherDeviceList(BLEDeviceData newDevice) {
        if (!isDeviceInOtherActiveList(newDevice.getAddress())) {
            bleOtherDeviceList.add(newDevice);
        }
        //logActiveOtherDeviceListData("addToBLEOtherDeviceList()");
	}


    /**
     * User clicked forget in device dialog. set search priority to -1.
     * We'll still temporarily remember the device type and active status, but won't try to connect to it.
     * @param bleDeviceAddress address of device to forget
     */
    void forgetDevice(String bleDeviceAddress) {
        Log.i(logtag, " forget device - devAddress = " + (bleDeviceAddress));
        ContentValues data = new ContentValues();
        data.put(DB_KEY_SEARCH_PRIORITY, -1);
        updateActiveBikeDeviceData(bleDeviceAddress, data);
    }

    public boolean isAnyBikeDeviceActive() {
         for (BLEDeviceData theData:bleActiveBikeDeviceList){
             if (getDevAddressActiveStatus(theData.getAddress()) == 1){
                 return true;
             }
         }
        return false;
    }

    public BLEScannerStatus getBLEScannerStatus() {
        return mBLEScannerStatus;
    }

    public void setBLEScannerStatus(BLEScannerStatus status) {
        this.mBLEScannerStatus = status;
    }
}
