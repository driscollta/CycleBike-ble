package com.cyclebikeapp.ble;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_LEFT_POWER_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_POWER_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_RIGHT_POWER_DEVICE;
import static com.cyclebikeapp.ble.Constants.DB_KEY_ACTIVE;
import static com.cyclebikeapp.ble.Constants.DB_KEY_BATT_STATUS;
import static com.cyclebikeapp.ble.Constants.DB_KEY_DEV_ADDRESS;
import static com.cyclebikeapp.ble.Constants.DB_KEY_DEV_NAME;
import static com.cyclebikeapp.ble.Constants.DB_KEY_DEV_TYPE;
import static com.cyclebikeapp.ble.Constants.DB_KEY_MANUFACTURER;
import static com.cyclebikeapp.ble.Constants.DB_KEY_MODEL_NUM;
import static com.cyclebikeapp.ble.Constants.DB_KEY_POWER_CAL;
import static com.cyclebikeapp.ble.Constants.DB_KEY_SEARCH_PRIORITY;
import static com.cyclebikeapp.ble.Constants.DB_KEY_SERIAL_NUM;
import static com.cyclebikeapp.ble.Constants.DB_KEY_SOFTWARE_REV;
import static com.cyclebikeapp.ble.Constants.DEFAULT_WHEEL_CIRCUM;
import static com.cyclebikeapp.ble.Constants.FORMAT_4_3F;
import static com.cyclebikeapp.ble.Constants.PREFS_NAME;
import static com.cyclebikeapp.ble.Constants.WHEEL_CIRCUM;

class BLEDBAdapter {
	private static final String BLE_DEVICE_TABLE = "bleDeviceTable";
	private static final String ANT_DEVICE_DB = "bleDeviceDataBase";
    private static final String CLOSE_DB = "closeDB()";
    private static final String OPEN_DB = "openDB()";
    private static final String ON_CREATE_SQLTABLE = "onCreateSQLTable()";
    private static final String ON_UPGRADE = "onUpgrade()";
    private static final String CREATE_DBHELPER = "Create DBHelper()";
    private static final String COULDN_T_UPDATE_DB_FOR_DEVICE_NUM = "couldn't update DB for deviceNum: ";
    private static final String UPDATE_DEVICE_RECORD_DEVICE_NUM = "updateDeviceRecord(deviceNum)";
    private static final String ADD_DEVICE_TO_DB_DEVICE_NAME = "addDeviceToDB(deviceName)";
    private static final String GET_ALL_DEVICE_NAMES_DEVICE_TYPE = "getAllDeviceNames(deviceType) ";
    private static final String GET_ALL_DEVICE_DATA = "getAllDeviceData()";
    private static final String IS_DEVICE_IN_DATA_BASE_BLE_DEVICE_ADDRESS = "isDeviceInDataBase(bleDeviceAddress)";
    private static final String GET_DEV_TYPE_FROM_DEV_ADDRESS_BLE_DEVICE_ADDRESS = "getDevTypeFromDevAddress(bleDeviceAddress)";
    private static final String FETCH_DEVICE_ADDRESS_BY_NAME_NAME = "fetchDeviceAddressByName(name) ";
    private static final String FETCH_DEVICE_DATA_BLE_DEVICE_ADDRESS = "fetchDeviceData(bleDeviceAddress) ";
	private final DBHelper mDBHelper;
	private SQLiteDatabase bleDeviceDB;
    private final String logtag = this.getClass().getSimpleName();
    private final String string_model;
    private final String string_serial;
    private final String string_sw_rev;
    private final SharedPreferences settings;
    private final String string_batt_status;
    private final String string_wheel_circum;
    private final String string_calibration;
    private final String string_search_pair;

    BLEDBAdapter(Context context) {
        mDBHelper = new DBHelper(context);
        string_model = context.getString(R.string.model_);
        string_serial = context.getString(R.string.serial_);
        string_sw_rev = context.getString(R.string.sw_rev_);
        settings = context.getSharedPreferences(PREFS_NAME, 0);
        string_batt_status = context.getString(R.string.battery_status_);
        string_wheel_circum = context.getString(R.string.wheel_circumference_);
        string_calibration = context.getString(R.string.calibration_data_);
        string_search_pair = context.getString(R.string.search_pair);
    }

    String getModelMessage(Cursor deviceData) {
        String manufacturer = deviceData.getString(deviceData.getColumnIndexOrThrow(DB_KEY_MANUFACTURER));

        return "\n" + manufacturer +"\n" + string_model
                + deviceData.getString(deviceData.getColumnIndexOrThrow(DB_KEY_MODEL_NUM)) + "\n"
                + string_serial
                + deviceData.getString(deviceData.getColumnIndexOrThrow(DB_KEY_SERIAL_NUM)) + "\n"
                + string_sw_rev
                + deviceData.getString(deviceData.getColumnIndexOrThrow(DB_KEY_SOFTWARE_REV)) + "\n";
    }
    @SuppressLint("DefaultLocale")
    String getOtherMessage(Cursor deviceData) {
        String response = "";
        BLEDeviceType deviceType = BLEDeviceType.UNKNOWN_DEVICE;
        try {
            deviceType = BLEDeviceType.valueOf(deviceData.getInt(deviceData.getColumnIndexOrThrow(DB_KEY_DEV_TYPE)));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        String battStatus = deviceData.getString(deviceData.getColumnIndexOrThrow(DB_KEY_BATT_STATUS));
        String powerCal = deviceData.getString(deviceData.getColumnIndexOrThrow(DB_KEY_POWER_CAL));
        String wheelCirc = String.format(FORMAT_4_3F,
                Double.valueOf(settings.getString(WHEEL_CIRCUM, String.valueOf(DEFAULT_WHEEL_CIRCUM))));
        switch (deviceType) {
            case HEARTRATE_DEVICE:
                response = string_batt_status + battStatus;
                break;
            case BIKE_SPD_DEVICE:
                response = string_batt_status + battStatus
                        + string_wheel_circum
                        + wheelCirc;
                break;
            case BIKE_CADENCE_DEVICE:
                response = string_batt_status + battStatus;
                break;
            case BIKE_SPDCAD_DEVICE:
                response = string_wheel_circum
                        + wheelCirc;
                break;
            case BIKE_POWER_DEVICE:
            case BIKE_LEFT_POWER_DEVICE:
            case BIKE_RIGHT_POWER_DEVICE:
                response = string_batt_status + battStatus
                        + "\n" + string_calibration + powerCal;
                break;
            default:
                break;
        }// switch
        return response;
    }

    /**
     * Read the data pertaining to a particular deviceAddress
     *
     * @param deviceAddress the BLE device number of the device to read
     * @return a Cursor containing the data
     */
    Cursor fetchDeviceData(String deviceAddress) {
        if (MainActivity.debugAppState) Log.d(logtag, FETCH_DEVICE_DATA_BLE_DEVICE_ADDRESS + deviceAddress);

        String filter = DB_KEY_DEV_ADDRESS + "= '" + deviceAddress + "'";
        Cursor returnCursor = null;
        try {
            if (bleDeviceDB != null && !isClosed()) {
                returnCursor = bleDeviceDB.query(BLE_DEVICE_TABLE, null, filter,
                        null, null, null, null);
                if (returnCursor != null) {
                    returnCursor.moveToFirst();
                }
            }
        } catch (IllegalStateException e){
            e.printStackTrace();
        }
        return returnCursor;
    }

	/**
	 * Get the device address by specifying the device name. Also specify device
	 * type and position in case there are devices with the same name, but
	 * different device type.
	 * 
	 * @param name
	 *            the device name
	 * @param groupPosition
	 *            is the position in the list of device types like HRM, power
	 *            sensors, etc
	 * @return the device address
	 * */
	String fetchDeviceAddressByName(String name, int groupPosition) {
        if (MainActivity.debugAppState) Log.d(logtag, FETCH_DEVICE_ADDRESS_BY_NAME_NAME + name);

		String deviceAddress = "0";
		String[] columns = {DB_KEY_DEV_ADDRESS};
        try {
            if (bleDeviceDB != null && !isClosed()) {
                String filter = DB_KEY_DEV_NAME + " = '" + name + "'";
                Cursor mCursor = bleDeviceDB.query(BLE_DEVICE_TABLE, columns,
                        filter, null, null, null, null);
                if (mCursor != null && mCursor.moveToFirst()) {
                    try {
                        deviceAddress = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_DEV_ADDRESS));
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } finally {
                        mCursor.close();
                    }
                }
            }
        } catch (IllegalStateException e){
            e.printStackTrace();
        }
		return deviceAddress;
	}

	/**
	 * After deleting a device, reset the searchPriority starting at 1 for the
	 * lowest remaining search priority
	 * 
	 * @param deviceType
	 *            the device type to reset search priority
	 */
	private void resetSearchPriorityAfterForget(int deviceType) {
		if (bleDeviceDB == null || bleDeviceDB.isOpen()) {
			return;
		}
		ContentValues content = new ContentValues();
		String orderByPriority = DB_KEY_SEARCH_PRIORITY;
		// if devType is speed or cadence, also include speed-cadence type
		String devTypeFilter = DB_KEY_DEV_TYPE + "= '" + Integer.toString(deviceType) + "'";
		String[] columns = {DB_KEY_DEV_ADDRESS, DB_KEY_SEARCH_PRIORITY };
        // if devType is BIKE_POWER, also add LEFT_ and RIGHT_ to filter
        if (deviceType == BIKE_POWER_DEVICE.intValue()
                        || deviceType == BIKE_LEFT_POWER_DEVICE.intValue()
                        || deviceType == BIKE_RIGHT_POWER_DEVICE.intValue()){
            devTypeFilter = DB_KEY_DEV_TYPE + "= '"
                    + Integer.toString(deviceType) + "' OR "
                    + DB_KEY_DEV_TYPE + "= '"
                    + Integer.toString(BIKE_LEFT_POWER_DEVICE.intValue()) + "' OR "
                    + DB_KEY_DEV_TYPE + "= '"
                    + Integer.toString(BIKE_RIGHT_POWER_DEVICE.intValue()) + "'";
        }
		// 1) get all deviceType entries from the table, ordered by search priority
		Cursor mCursor = null;
		try {
			mCursor  = bleDeviceDB.query(BLE_DEVICE_TABLE, columns,
					devTypeFilter, null, null, null, orderByPriority);
			if (mCursor != null && mCursor.moveToFirst()) {
				// If there are no other devices? mCursor.moveToFirst() returns false
				// Set the lowest ordered devAddress searchPriority to 1
				int searchPriority = 1;
				String devAddress;
				do {
					devAddress = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_DEV_ADDRESS));
					// set priority and increment
					content.clear();
					content.put(DB_KEY_SEARCH_PRIORITY, searchPriority++);
					bleDeviceDB.update(BLE_DEVICE_TABLE, content,
							DB_KEY_DEV_ADDRESS + "=" + devAddress, null);
				} while (mCursor.moveToNext());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			if (mCursor != null) {
				mCursor.close();
			}
		}
	}

	/**
	 * Returns the type of device (power sensor, HRM, etc), knowing the unique
	 * ANT+ device number
	 * 
	 * @param devAddress
	 *            the device in question
	 * @return devType
	 */
	private int getDevTypeFromDevAddress(String devAddress) {
        if (MainActivity.debugAppState) Log.d(logtag, GET_DEV_TYPE_FROM_DEV_ADDRESS_BLE_DEVICE_ADDRESS + devAddress);

        int devType = -1;
		String devNumFilter = DB_KEY_DEV_ADDRESS + "= '" + devAddress + "'";
		String[] columns = { DB_KEY_DEV_TYPE };
		if (bleDeviceDB != null
				&& bleDeviceDB.isOpen()
				&& !bleDeviceDB.isDbLockedByCurrentThread()) {
			Cursor mCursor = bleDeviceDB.query(BLE_DEVICE_TABLE, columns,
					devNumFilter, null, null, null, null);
			if (mCursor != null && mCursor.moveToFirst()) {
				try {
					devType = mCursor.getInt(mCursor.getColumnIndexOrThrow(DB_KEY_DEV_TYPE));
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					mCursor.close();
				}
			}
		}
		return devType;
	}

	/**
	 * Erases all knowledge of the device from the data base
	 * 
	 * @param deviceAddress
	 *            the device in question
	 */
	void doForget(String deviceAddress) {
        if (MainActivity.debugAppState) Log.d(logtag, "DBAdapter.doForget() address - " + deviceAddress);
        int devType = getDevTypeFromDevAddress(deviceAddress);
		if (bleDeviceDB != null && bleDeviceDB.isOpen()) {
			try {
				bleDeviceDB.delete(BLE_DEVICE_TABLE,
						DB_KEY_DEV_ADDRESS + "= '" + deviceAddress + "'",
						null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (devType > -1) {
			resetSearchPriorityAfterForget(devType);
		}
	}

	/**
	 * Determines if a device is stored in the data base
	 * 
	 * @param bleDeviceAddress
	 *            the device in question
	 * @return true if that device is in the data base
	 */
	boolean isDeviceInDataBase(String bleDeviceAddress) {
        //if (MainActivity.debugAppState) Log.i(logtag, IS_DEVICE_IN_DATA_BASE_BLE_DEVICE_ADDRESS + bleDeviceAddress);
		boolean found = false;
		if (bleDeviceDB != null && !isClosed()) {
			// query db for matching deviceNum, ordered by search priority
			String devNumFilter = DB_KEY_DEV_ADDRESS + "= '"
					+ bleDeviceAddress + "'";
			String[] columns = {DB_KEY_DEV_ADDRESS};
			Cursor mCursor = null;
			try {
				mCursor = bleDeviceDB.query(BLE_DEVICE_TABLE, columns, devNumFilter, null, null, null, null);
				if (mCursor != null && mCursor.moveToFirst()) {
					String devAddress;
					do {
						devAddress = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_DEV_ADDRESS));
						found = (devAddress.equals(bleDeviceAddress));
					} while (mCursor.moveToNext());
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (mCursor != null){
					mCursor.close();
				}
			}
		}
		return found;
	}
	/**
	 * When setting-up childItems to display device data, want all device names

	 * @return an ArrayList of ActiveANTDeviceData
	 */
	ArrayList<BLEDeviceData> getAllDeviceData(){
        if (MainActivity.debugAppState) Log.d(logtag, GET_ALL_DEVICE_DATA);
		ArrayList<BLEDeviceData> deviceDBData = new ArrayList<>();
		String[] columns = { DB_KEY_DEV_NAME, DB_KEY_SEARCH_PRIORITY, DB_KEY_DEV_ADDRESS, DB_KEY_ACTIVE, DB_KEY_DEV_TYPE };
		String orderBy = DB_KEY_SEARCH_PRIORITY;
		if (bleDeviceDB != null && !isClosed()) {
			// query db for matching deviceType, ordered by search priority
			Cursor mCursor = null;
			try {
				mCursor = bleDeviceDB.query(BLE_DEVICE_TABLE, columns, null, null, null, null, orderBy);
				if (mCursor != null && mCursor.moveToFirst()) {
					do {
                        BLEDeviceData theData = new BLEDeviceData("", BLEDeviceType.UNKNOWN_DEVICE);
						ContentValues values = new  ContentValues();
						values.put(DB_KEY_DEV_NAME, mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_DEV_NAME)));
						values.put(DB_KEY_SEARCH_PRIORITY, mCursor.getInt(mCursor.getColumnIndexOrThrow(DB_KEY_SEARCH_PRIORITY)));
						String devAddress = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_DEV_ADDRESS));
						values.put(DB_KEY_DEV_ADDRESS, devAddress);
						theData.setAddress(devAddress);
						values.put(DB_KEY_DEV_TYPE, mCursor.getInt(mCursor.getColumnIndexOrThrow(DB_KEY_DEV_TYPE)));
						values.put(DB_KEY_ACTIVE, mCursor.getInt(mCursor.getColumnIndexOrThrow(DB_KEY_ACTIVE)));
						theData.setData(values);
                        theData.setDeviceType(BLEDeviceType.valueOf(mCursor.getInt(mCursor.getColumnIndexOrThrow(DB_KEY_DEV_TYPE))));
						deviceDBData.add(theData);
					} while (mCursor.moveToNext());
				}
			}catch (Exception e){
				e.printStackTrace();
			} finally {
				if (mCursor != null) {
					mCursor.close();
				}
			}
		}
		return deviceDBData;
	}

	/**
	 * When setting-up childItems to display device data, want all device names
	 * of a particular type.
	 * 
	 * @param deviceType
	 *            the type of device in question
	 * @return an ArrayList of device names of that type
	 */
	ArrayList<String> getAllDeviceNames(BLEDeviceType deviceType) {

        if (MainActivity.debugAppState) Log.d(logtag, GET_ALL_DEVICE_NAMES_DEVICE_TYPE + deviceType.name());
		ArrayList<String> deviceNames = new ArrayList<>();
		String[] columns = { DB_KEY_DEV_NAME , DB_KEY_DEV_ADDRESS};
		String orderBy = DB_KEY_SEARCH_PRIORITY;
		deviceNames.add(string_search_pair);
		if (bleDeviceDB != null && !isClosed()) {
			String filter = DB_KEY_DEV_TYPE + "= '"
					+ Integer.toString(deviceType.intValue()) + "'";
            // if devType is BIKE_POWER, also add LEFT_ and RIGHT_ to filter
            if (deviceType == BIKE_POWER_DEVICE){
                filter = DB_KEY_DEV_TYPE + "= '"
                        + Integer.toString(deviceType.intValue()) + "' OR "
                        + DB_KEY_DEV_TYPE + "= '"
                        + Integer.toString(BIKE_LEFT_POWER_DEVICE.intValue()) + "' OR "
                        + DB_KEY_DEV_TYPE + "= '"
                        + Integer.toString(BIKE_RIGHT_POWER_DEVICE.intValue()) + "'";
            }
			// query db for matching deviceType, ordered by search priority
			Cursor mCursor = null;
			try {
				mCursor = bleDeviceDB.query(BLE_DEVICE_TABLE, columns,
						filter, null, null, null, orderBy);
				if (mCursor != null && mCursor.moveToFirst()) {
					do {
						String name = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_DEV_NAME));
						if (name == null || ("").equals(name)){
							name = "<" + mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_DEV_ADDRESS) )+ ">";
						}
						deviceNames.add(name);
					} while (mCursor.moveToNext());
				}
			} catch (Exception e){
				e.printStackTrace();
			} finally {
				if (mCursor != null) {
					mCursor.close();
				}
			}
		}
		return deviceNames;
	}

	/**
	 * When tracking data after acquisition add that device to the data base if
	 * its not already there
	 * 
	 * @param content
	 *            has the device name, BLE device address, type; other data will be added later
	 */
	void addDeviceToDB(ContentValues content) {
/*              + DB_KEY_DEV_ADDRESS + TEXT_NOT_NULL
                + DB_KEY_DEV_NAME + TEXT_NOT_NULL
                + DB_KEY_DEV_TYPE + TEXT_NOT_NULL
                + DB_KEY_BATT_STATUS + TEXT_NOT_NULL
                + DB_KEY_SERIAL_NUM + TEXT_NOT_NULL
                + DB_KEY_MANUFACTURER + TEXT_NOT_NULL
                + DB_KEY_SOFTWARE_REV + TEXT_NOT_NULL
                + DB_KEY_MODEL_NUM + TEXT_NOT_NULL
                + DB_KEY_POWER_CAL + TEXT_NOT_NULL
                + DB_KEY_SEARCH_PRIORITY + INTEGER_NOT_NULL + COMMA_SPACE
                + DB_KEY_ACTIVE + INTEGER_NOT_NULL + PAREN_COLON;*/
        if (MainActivity.debugLEScan) Log.d(logtag, ADD_DEVICE_TO_DB_DEVICE_NAME + content.getAsString(DB_KEY_DEV_NAME));
        try {
            if (bleDeviceDB != null && !isClosed()) {
                if (MainActivity.debugLEScan) Log.w(logtag, "bleDeviceDB == null? " + (bleDeviceDB == null?"yes":"no")
                        + " Is db closed? " + (isClosed()?"yes":"no"));
                content.put(DB_KEY_BATT_STATUS, "");
                content.put(DB_KEY_SERIAL_NUM, "");
                content.put(DB_KEY_MANUFACTURER, "");
                content.put(DB_KEY_SOFTWARE_REV, "");
                content.put(DB_KEY_MODEL_NUM, "");
                content.put(DB_KEY_POWER_CAL, "");
                content.put(DB_KEY_SEARCH_PRIORITY, 1);
                content.put(DB_KEY_ACTIVE, 1);
                long conflictResult = bleDeviceDB.insertWithOnConflict(
                        BLE_DEVICE_TABLE, "", content,
                        SQLiteDatabase.CONFLICT_IGNORE);
                if (MainActivity.debugAppState) Log.w(logtag, "conflictResult: " + conflictResult);
            }
        } catch (IllegalStateException e){
            e.printStackTrace();
        }
        //dumpDBToLog("adding device to DB");
	}

	/**
	 * When receiving BLE device data, update the data base
	 * 
	 * @param deviceAddress
	 *            the device transmitting the new data
	 * @param content
	 *            an Object containing the new data
	 */
	void updateDeviceRecord(String deviceAddress, ContentValues content) {
        if (MainActivity.debugAppState) Log.i(logtag, UPDATE_DEVICE_RECORD_DEVICE_NUM + deviceAddress);
		String[] whereArgs = { deviceAddress };
		try {
			if (bleDeviceDB != null && bleDeviceDB.isOpen()) {
				bleDeviceDB.update(BLE_DEVICE_TABLE, content, DB_KEY_DEV_ADDRESS + "=?", whereArgs);
			} else {
				Log.w(logtag, COULDN_T_UPDATE_DB_FOR_DEVICE_NUM + deviceAddress);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
        if (MainActivity.debugLEScan)dumpDBToLog("updateDeviceRecord for: " + deviceAddress);
	}

	private void dumpDBToLog(String message) {
		// list the entire database in Log file, message can be where the dump was requested
		Log.v(logtag, message);
		Log.d(logtag, "num, name, type, batt-status, serial#, man, SW-rev, model#, cal, priority, uptime, active");
		if (bleDeviceDB == null || isClosed()) {
			return;
		}
        try {
            Cursor cursor = bleDeviceDB.rawQuery("SELECT * FROM bleDeviceTable", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Log.i(logtag,
                            cursor.getString(1) + "," + cursor.getString(2) + ","
                                    + cursor.getString(3) + "," + cursor.getString(4) + ","
                                    + cursor.getString(5) + "," + cursor.getString(6) + ","
                                    + cursor.getString(7) + "," + cursor.getString(8) + ","
                                    + cursor.getString(9) + "," + cursor.getString(10) + ","
                                    + cursor.getString(11));
                } while (cursor.moveToNext());
            } else {
                Log.i(this.getClass().getName(), "dumpDB null cursor");
            }
            if (cursor != null) {
                cursor.close();
            }
        }catch (IllegalStateException e){
            e.printStackTrace();
        }
	}

	private class DBHelper extends SQLiteOpenHelper {
        private static final String CREATE_TABLE_LONG = "CREATE TABLE IF NOT EXISTS bleDeviceTable ( _id INTEGER PRIMARY KEY AUTOINCREMENT, ";
        private static final String COMMA_SPACE = ", ";
        private static final String PAREN_COLON = ");";
        private static final String TEXT_NOT_NULL = " TEXT NOT NULL, ";
        private static final String INTEGER_NOT_NULL = " INTEGER NOT NULL";
        private static final String CREATE_STRING = CREATE_TABLE_LONG
                + DB_KEY_DEV_ADDRESS + TEXT_NOT_NULL
                + DB_KEY_DEV_NAME + TEXT_NOT_NULL
                + DB_KEY_DEV_TYPE + TEXT_NOT_NULL
                + DB_KEY_BATT_STATUS + TEXT_NOT_NULL
                + DB_KEY_SERIAL_NUM + TEXT_NOT_NULL
                + DB_KEY_MANUFACTURER + TEXT_NOT_NULL
                + DB_KEY_SOFTWARE_REV + TEXT_NOT_NULL
                + DB_KEY_MODEL_NUM + TEXT_NOT_NULL
                + DB_KEY_POWER_CAL + TEXT_NOT_NULL
                + DB_KEY_SEARCH_PRIORITY + INTEGER_NOT_NULL + COMMA_SPACE
                + DB_KEY_ACTIVE + INTEGER_NOT_NULL + PAREN_COLON;
        private static final int DB_VERSION = 1;

		DBHelper(Context context) {
			super(context, ANT_DEVICE_DB, null, DB_VERSION);
			if (MainActivity.debugAppState)
				Log.i(logtag, CREATE_DBHELPER);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (MainActivity.debugAppState) Log.i(logtag, ON_UPGRADE);
			String dropString = "DROP TABLE IF EXISTS bleDeviceTable;";
			db.execSQL(dropString);
			onCreate(db);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
            if (MainActivity.debugAppState) Log.i(logtag, ON_CREATE_SQLTABLE);
            try {
                db.execSQL(CREATE_STRING);
            } catch (SQLiteException e){
                e.printStackTrace();
            }
		}
	}

	public BLEDBAdapter open() throws SQLiteException {
        if (MainActivity.debugAppState) {Log.i(logtag, OPEN_DB);}
        bleDeviceDB = mDBHelper.getWritableDatabase();
		return this;
	}

	/** close the bleDeviceTable database */
	public void close() {
		 if (MainActivity.debugAppState) {Log.i(logtag, CLOSE_DB);}
		try {
			if (mDBHelper != null) {
					mDBHelper.close();
			}
		} catch (IllegalStateException e) {
				e.printStackTrace();
		}
	}

	boolean isClosed() {
		return bleDeviceDB == null || !bleDeviceDB.isOpen();
	}

}
