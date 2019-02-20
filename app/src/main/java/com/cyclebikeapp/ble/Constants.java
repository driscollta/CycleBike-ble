package com.cyclebikeapp.ble;

import android.view.Gravity;

final class Constants {
    static final String PAUSED = "paused? ";
    static final String TRUE = "true";
    static final String FALSE = "false";
    static final String DO_PAIR_DEVICE = "doPair() device: ";
    static final String PAIRING_REQUEST_RESULT = "pairing request result = ";
    static final String INITIATED = "initiated";
    static final String FAILED = "failed";
    static final String TRACKING_POWER = "trackingPower()";
    static final String UPDATE_DBTRACKING = "updateDBTracking()";
    static final String NOT_TRACKING_POWER = "notTrackingPower()";
    static final String TRACKING_SPEED = "trackingSpeed()";
    static final String NOT_TRACKING_SPEED = "notTrackingSpeed()";
    static final String TRACKING_CAD = "trackingCad()";
    static final String NOT_TRACKING_CAD = "notTrackingCad()";
    static final String TRACKING_HRM = "trackingHRM()";
    static final String NOT_TRACKING_HRM = "notTrackingHRM()";
    static final String UPDATE_DB = "update DB - ";
    static final String ACTIVE_LIST_SIZE = " activeListSize: ";
    static final String DEVICE_DATA_DEV_ADDRESS = "deviceData - device address: ";
    static final int BYTE_PER_MB = 1000000;
    static final int SMALL_FIT_FILE_SIZE = 1500;
    static final String ACTIVITY_FILE_PATH = "/Android/data/com.cyclebikeapp.ble/files/";
    // adjust the titles in the cad_hr_power row if the screen width is small
    static final int SMALL_SCREEN_WIDTH = 250;
    static final String DDF_KEY_TITLE = "DDF_key_title";
    static final String DDF_KEY_MESSAGE = "DDF_key_message";
    static final String DDF_KEY_ADDRESS = "DDF_key_address";
    static final String DDF_KEY_DEVICE_TYPE = "DDF_key_deviceType";
    static final String DDF_KEY_DEVICE_ACTIVE = "DDF_key_device_active";
    static final String NO_INFO = " x | ";
    static final String UNK = "UNK";
    static final String GAL = "GAL";
    static final String BEID = "BEID";
    static final String QZSS = "QZSS";
    static final String GLO = "GLO";
    static final String SBAS = "SBAS";
    static final String GPS = "GPS";
    static final String SATELLITES_USED = "\nSatellites used - \n";
    static final String GPS_STATUS = "gps_status";
    static final String NOTIFICATION_CHANNEL_ID = "cyclebike_ble_notification_channel";
    static final String ENABLED = "enabled: ";

    private Constants() {}
	// some random static Strings
	static final int MY_PERMISSIONS_REQUEST_LOCATION = 924;
	static final int MY_PERMISSIONS_REQUEST_WRITE = 824;
    static final int REQUEST_CHECK_SETTINGS = 94;
    static final int RC_SHOW_FILE_LIST = 66;
    static final int RC_BLE_SETTINGS = 56;
    static final int RC_BLE_DEVICE_EDIT = 42;
    static final int RC_BLUETOOTH_MODE_SETTINGS = 54;
    static final int REQUEST_CHANGE_LOCATION_SETTINGS = 92;
    static  final int RC_BATT_OPT_SETTINGS = 87;
    static final String TMP_CB_ROUTE = ".tmpCBRoute";
	static final String TP_DENSITY = "tpDensity_";
	static final String RESTORE_ROUTE_FILE_GPXFILENAME = "restoreRouteFile() - gpxfilename: ";
	static final String EXCEPTION = "Exception";
	static final String FILE_NOT_FOUND = "file not found";
    static final String AUTO_PAUSE = "auto_pause";
	static final String FIT_ACTIVITY_TYPE  = "1";
	static final String TCX_ACTIVITY_TYPE  = "0";
    static final String SHARING_FILENAME = "sharing_filename";
	static final String UPLOAD_FILENAME = "upload_filename";
	static final String SHOW_SHARING = "show_sharing_alert";
	static final String USER_CANCELED = "user-canceled";
	static final String INITIALIZING_ROUTE = "Initializing Route";
	static final String NO_ROUTE_DATA_IN_FILE = "No route data in file!";
	static final String LOOKING_FOR_ROUTE_DATA = "Looking for route data";
	static final String LOADING_FILE = "Loading File";
	static final String XML = ".xml";
	static final String TCX = ".tcx";
	static final String GPX = ".gpx";
    static final String FIT = ".fit";
    // Unique tag for the error dialog fragment
    static final String DIALOG_ERROR = "dialog_error";
//database key tags
	static final String DB_KEY_DEV_ADDRESS = "db_key_device_number";
	static final String DB_KEY_DEV_NAME = "db_key_device_name";
	static final String DB_KEY_DEV_TYPE = "db_key_device_type";
	static final String DB_KEY_BATT_STATUS = "db_key_batt_status";
	static final String DB_KEY_SERIAL_NUM = "db_key_serial_num";
	static final String DB_KEY_MANUFACTURER = "db_key_manufacturer";
	static final String DB_KEY_SOFTWARE_REV = "db_key_software_rev";
	static final String DB_KEY_MODEL_NUM = "db_key_model_num";
	static final String DB_KEY_POWER_CAL = "db_key_power_cal";
	static final String DB_KEY_SEARCH_PRIORITY = "db_key_priority";
	static final String DB_KEY_ACTIVE = "db_key_active";
	static final String CHOOSER_TYPE = "type";
	static final int CHOOSER_TYPE_TCX_DIRECTORY = 300;
	static final int CHOOSER_TYPE_TCX_FILE = 400;
	static final int CHOOSER_TYPE_GPX_DIRECTORY = 100;
	static final int CHOOSER_TYPE_GPX_FILE = 200;
	static final int BLESETTINGS_TYPE_SEARCH_PAIR = 600;
	static final int BLESETTINGS_TYPE_CAL = 700;

    // Request code to use when launching the resolution activity
    static final int REQUEST_RESOLVE_ERROR = 1001;
	static final int UPLOAD_FILE_SEND_REQUEST_CODE = 2000;
	static final int ACTIVITY_FILE_TYPE = 1;
	static final int ROUTE_FILE_TYPE = 0;

	/**	key tags for shared preferences with restore route */	
	static final String DEVICE_ADDRESS_POWER = "DeviceAddressPower";
	static final String DEVICE_ADDRESS_CADENCE = "DeviceAddressCadence";
	static final String DEVICE_ADDRESS_SPEED = "DeviceAddressSpeed";
	static final String DEVICE_ADDRESS_SPEED_CADENCE = "DeviceAddressSpeedCadence";
	static final String DEVICE_ADDRESS_HRM = "DeviceAddressHRM";
	static final int BLE_TOAST_GRAVITY = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
	static final String DOUBLE_ZERO = "0.0";
    static final String PREV_PED_CNTS = "prev_ped_cnts";
	static final String PED_CNTS_INIT = "ped_cnts_init";
    static final String OPPOSITE_PED_CNTS_INIT = "opposite_ped_cnts_init";
    static final String OPPOSITE_PREV_PED_CNTS = "opposite_prev_ped_cnts";
	static final String NUM_PEDAL_CAD = "num_pedal_cadence";
	static final String TOTAL_PEDAL_CAD = "total_pedal_cadence";
	static final String AVG_CADENCE = "avg_cadence";
	static final String MAX_CADENCE = "max_cadence";
	//HR data Keys to shared preferences
	static final String TOTAL_HR_COUNTS = "total_hr_counts";
	static final String NUM_HR_EVENTS = "num_hr_events";
	static final String AVG_HR = "avg_heartrate";
	static final String MAX_HR = "max_heartrate";
	//wheel data Keys to shared preferences

	static final String WHEEL_CUMREV = "wheel_cumrev";
	static final String WHEEL_CUMREV_AT_START = "wheel_cumrev_at_start";
	static final String NUM_WHEEL_CNTS = "tot_wheelCounts";	
	static final String WHEEL_PREV_COUNT = "wheel_prev_count";
	static final String WHEEL_IS_CAL = "wheel_is_cal";
	static final String WHEEL_CIRCUM = "wheel_circumference";
    static final String CRANK_LENGTH = "crank_length";
	static final String START_DIST = "wheel_start_distance";
	static final String MAX_SPEED = "maxSpeed";
	//power data Keys to shared preferences

	static final String POWER_WHEEL_CUMREV = "power_wheel_cumrev";
	static final String POWER_WHEEL_PREV_COUNT = "power_wheel_prev_count";
	static final String POWER_WHEEL_CUMREV_AT_START = "power_wheel_cumrev_at_start";
	static final String POWER_WHEEL_IS_CAL = "power_wheel_is_cal";

	static final String POWER_CNTS_INIT = "power_cnts_init";
    static final String OPPOSITE_POWER_CNTS_INIT = "opposite_power_cnts_init";
	static final String CUM_ENERGY = "cum_energy";
	static final String CUM_POWER_TIME = "cum_power_time";
	static final String AVG_POWER = "avg_power";
	static final String MAX_POWER = "max_power";
	static final String POWER_WHEEL_CIRCUM = "power_wheel_circumference";
	static final String NUM_POWER_WHEEL_CNTS = "tot_power_wheelCounts";
	static final String POWER_START_DIST = "power_wheel_start_distance";
	//calculated crank cadence data Keys to shared preferences
	static final String TOTAL_CALC_CAD = "total_calc_crank_cadence";
	static final String NUM_CALC_CAD = "num_calc_crank_cadence";
	static final String AVG_CALC_CADENCE = "avg_calc_crank_cadence";
	static final String MAX_CALC_CADENCE = "max_calc_crank_cadence";
	// Keys to shared preferences
    static final String SAVED_LAT = "savedLat";
    static final String SAVED_LON = "savedLon";
    static final String PREF_SAVED_LOC_TIME = "prefs_saved_time";
    static final String TCX_LOG_FILE_NAME = "tcxLogFileName";
	static final String TCX_LOG_FILE_FOOTER_LENGTH = "tcxLogFileFooterLength";
	static final String CURR_WP = "curr_WP";
	static final String FIRST_LIST_ELEM = "first_ListElem";
	static final String TRIP_DISTANCE = "tripDistance";
	static final String TRIP_TIME = "tripTime";
	static final String WHEEL_TRIP_DISTANCE = "wheelTripDistance";
	static final String WHEEL_TRIP_TIME = "wheeltriptime";
	static final String POWER_WHEEL_TRIP_TIME = "powerwheeltriptime";
	static final String POWER_WHEEL_TRIP_DISTANCE = "powerwheelTripDistance";
	static final String PREV_WHEEL_TRIP_DISTANCE = "prevWheelTripDistance";
	static final String SPOOF_WHEEL_TRIP_DISTANCE = "spoofWheelTripDistance";
	static final String PREV_SPOOF_WHEEL_TRIP_DISTANCE = "prevSpoofWheelTripDistance";
    static final int _360 = 360;
	static final String PREFS_NAME = "MyPrefsFile_smart";
	static final String APP_NAME = "CycleBike Smart";
	static final String BONUS_MILES = "bonusMiles ";
	static final String AUTH_NO_NETWORK_INTENT_RC = "88";
	static final String KEY_CHOSEN_GPXFILE = "chosenGPXFile";
	static final String KEY_CHOSEN_TCXFILE = "chosenTCXFile";
	static final String KEY_GPXPATH = "gpxPath";
	static final String KEY_PAIR_CHANNEL = "key_pair_channel";
	static final String KEY_CAL_CHANNEL = "key_cal_channel";
	static final String KEY_CHOOSER_CODE = "chooserCode";
    static final String KEY_SP_DEVICE_NAME = "KEY_SP_DEVICE_NAME";
    static final String KEY_SP_INDB_ICON = "KEY_SP_INDB_ICON";
    static final String IS_INDB = "12";
    static final String NOT_INDB = "13";
	static final String MOBILE_DATA_SETTING_KEY = "mobile_data_setting_key";
	static final String STATE_RESOLVING_ERROR = "resolving_error";
	static final String[] RWGPS_EMAIL = {"upload@rwgps.com"};
    static final String USE_BLE = "USE_BLE";
	static final String SHOW_BLE = "SHOW_BLE";
	static final String HI_VIZ = "hi_viz";
	// ANT constants
	/** Pair to any device. */
	static final int WILDCARD = 0;
	static final String KEY_TRAINER_MODE = "key_trainer_mode";
	static final String KEY_VELO_CHOICE = "velo_default";
	static final String KEY_FORCE_NEW_TCX = "force_new_tcx";
    static final double msecPerSec = 1000.;
	static final long ONE_SEC = 1000;
	/** set location current if no older than this (in millisec)*/
	static final long TEN_SEC = 10 * 1000;
    /** set sensor data current if no older than this (in millisec)*/
	static final long THREE_SEC = 3 * 1000;
    /** autoConnect BLE this often (in millisec)*/
	static final long THIRTY_SEC = 30 * 1000;
    static final long THREE_MINUTES = 3 * 60 * 1000;
	static final long TWENTYFOUR_HOURS = 24 * 60 * 60 * 1000;
    static final long JAN_1_2000 = 975596581L;
    // distance conversions
	static final double mph_per_mps = 2.23694;
	static final double kph_per_mps = 3.6;
	static final double km_per_meter = 0.001;
	static final double mile_per_meter = 0.00062137119224;
    static final double faultSpeed = 2.2352; // powertap fault (m/s)
    /**detection threshold for the paused condition: speed less than */
    static final double speedPausedVal = 0.5 / mph_per_mps;
    /**detection threshold for the paused condition: delta direction of travel less than */
    static final double dotPausedVal = .01;
	// some GPS constants and threshold constants
    static final String PREFS_DEFAULT_LATITUDE = "37.1";
    static final String PREFS_DEFAULT_LONGITUDE = "-122.1";
    static final long PREFS_DEFAULT_TIME = 123456;
	/** glitch protection for Stages crank-torque power meter limit max power to 1500 W */
    static final double MAXIMUM_CT_POWER = 1500.;
    /** glitch protection for Stages crank-torque power meter limit cadence to 150 */
    static final long MAXIMUM_CT_CADENCE = 250;
    /** glitch protection for speed sensors(mps) = 650 mph */
    static final double MAXIMUM_SPEED = 290.;
	/** min trip distance (meters) before calibrating wheel circumference
	 * calWheel is actually called every minute, so distance may be longer */
	static final double MIN_CAL_DIST = 1606.1;
	/** smallest wheel circumference */
	static final double LOWER_WHEEL_CIRCUM = 1.075;
	/** largest wheel circumference */
	static final double UPPER_WHEEL_CIRCUM = 2.51;
	/** default wheel circumference */
	static final double DEFAULT_WHEEL_CIRCUM = 2.142;
    /** shortest crank length */
    static final double LOWER_CRANK_LENGTH = 110.;
    /** longest crank length */
    static final double UPPER_CRANK_LENGTH = 236.5;
    /** default crank length */
    static final double DEFAULT_CRANK_LENGTH = 172.5;
	/** smallest time difference between BLE events (ms) */
	static final long MIN_DELTAT = 200;
	/** largest time difference between BLE power events (ms) */
	static final long MAX_DELTAT = THREE_SEC;
	/**want good location accuracy when calibrating the speed sensors (meters) */
	static final float goodLocationAccuracy = 18;
	/**want good enough location accuracy when writing locations to track file (meters) */
	static final float goodEnoughLocationAccuracy = 50;
	 /** when re-starting nav from long-pressed WP make sure we're nearEnough (meters) */
	static final double nearEnough = 402.25;
	/** gps speed at which we can trust that the direction of travel bearing is
	 * accurate (m/sec) */
	static final double accurateGPSSpeed = 2. / mph_per_mps;
    static final double DEG_PER_BEARING_ICON = 22.5;
    //are we resolving the Play Services error
    static final String RESOLVING_PLAYSERVICES = "resolving_playservices";
    static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 0 meters
    static final long MIN_TIME_BW_UPDATES = 100; //.1 sec
	//program constants
    static final int TITLE_SIZE_PIXELS = 11;
    static final int VALUE_SIZE_PIXELS = 18;
    static final int SPEED_SIZE_PIXELS = 34;
    static final String TIME_STR_FORMAT = "%02d";
	static final String FORMAT_3D = "%3d";
	static final String FORMAT_4_1F = "%4.1f";
	static final String FORMAT_4_3F = "%4.3f";
	static final String FORMAT_3_1F = "%3.1f";
	static final String FORMAT_1F = "%.1f";
	static final String MILE = "mi";
	static final String KM = "km";
	static final String METER = "m";
	static final String FOOT = "ft";
	static final String ZERO = "0";
	static final String DASHES = "---";
    static final String SPEED_TRIPLE_X = "XX.x";
    static final String _CAL = " cal";
    static final String QUESTION = "??";
    static final int DISTANCE_TYPE_MILE = 0;
    static final int DISTANCE_TYPE_METRIC = 1;
    static final int ROUTE_DISTANCE_TYPE = 0;
    static final int DIRECT_DISTANCE_TYPE = 1;
	/** auto-connect to devices using all known devices in database, or just last connected device*/
	static final String KEY_AUTO_CONNECT_ALL = "autoconnect_all";
	// track density of the current merged route
	static final String KEY_TRACK_DENSITY = "track_density";
    static final String UNIT_DEFAULT = "unit_default";
    static final String KEY_TRACKPOINT_DENSITY_DEFAULT = "trackpoint_density_default";
	//routeHashMap Keys
	/** street is the text string of the street name */
	static final String KEY_STREET = "street";
	/** street unit is the units to display (ft, mi, m, km) */
	static final String KEY_UNIT = "street_unit";
	/** distance is the distance to the next turn, updated as locations are received */
	static final String KEY_DISTANCE = "distance";
	/** turn level is the numeric value that defines the turn icon to display
		 defined in the turn_levels.xml document in res/drawable */
	static final String KEY_TURN = "turn_level";
	/** bearing level is the numeric value that defines the bearing arrow to display
		 the icons are defined in the arrow_levels.xml document in res/drawable */
	static final String KEY_BEARING = "bearing_level";
	/** dimmed is an indication of how to display the data, dimmed when a way
		 point has been passed or just within reach */
	static final String KEY_DIM = "dimmed";
    static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_location_updates";

}
