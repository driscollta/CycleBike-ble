package com.cyclebikeapp.ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteException;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;
import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_CAD_DATA_AVAILABLE;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_CAD_GATT_CONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_CAD_GATT_DISCONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_CAD_GATT_SERVICES_DISCOVERED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_DATA_AVAILABLE;
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
import static com.cyclebikeapp.ble.BLEConstants.CADENCE_TIME_RESOLUTION;
import static com.cyclebikeapp.ble.BLEConstants.CALIBRATE_POWER_OPCODE;
import static com.cyclebikeapp.ble.BLEConstants.DEVICE_DISCOVERY_TIME;
import static com.cyclebikeapp.ble.BLEConstants.EXTRAS_DEVICE_ADDRESS;
import static com.cyclebikeapp.ble.BLEConstants.EXTRAS_DEVICE_OPPOSITE_POWER_ADDRESS;
import static com.cyclebikeapp.ble.BLEConstants.EXTRAS_DEVICE_POWER_ADDRESS;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_CAD_DATA;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_CSC_DATA_TYPE;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_CSC_DEVICE_TYPE;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_DATA;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_HRM_DATA;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_HRM_DATA_TYPE;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_OPPOSITE_PCP_OPCODE;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_OPPOSITE_POWER_DATA_TYPE;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_PCP_OPCODE;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_CAL_VALUE;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_CRANK_LENGTH;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_CRANK_REVS;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_CRANK_REV_TIME;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_DATA;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_DATA_TYPE;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_HAS_CAD;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_HAS_SPEED;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_IPOWER;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_WHEEL_REVS;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_WHEEL_REV_TIME;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_RESPONSE_VALUE;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_REV_DATA;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_REV_TIME_DATA;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_SPEED_DATA;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_WHEEL_REVS;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_WHEEL_REV_TIME;
import static com.cyclebikeapp.ble.BLEConstants.POWER_CRANK_TIME_RESOLUTION;
import static com.cyclebikeapp.ble.BLEConstants.POWER_WHEEL_TIME_RESOLUTION;
import static com.cyclebikeapp.ble.BLEConstants.REQUEST_CRANK_LENGTH_OPCODE;
import static com.cyclebikeapp.ble.BLEConstants.RESPONSE_VALUE_FAILURE;
import static com.cyclebikeapp.ble.BLEConstants.RESPONSE_VALUE_SUCCESS;
import static com.cyclebikeapp.ble.BLEConstants.SCANLE_DEVICES;
import static com.cyclebikeapp.ble.BLEConstants.SCAN_PERIOD;
import static com.cyclebikeapp.ble.BLEConstants.SECONDS_PER_MINUTE;
import static com.cyclebikeapp.ble.BLEConstants.WHEEL_TIME_RESOLUTION;
import static com.cyclebikeapp.ble.BLEConstants.WRITE_CRANK_LENGTH_OPCODE;
import static com.cyclebikeapp.ble.BLEDataType.BIKE_POWER_CONTROL_PT;
import static com.cyclebikeapp.ble.BLEDataType.BIKE_POWER_FEATURE;
import static com.cyclebikeapp.ble.BLEDataType.BIKE_POWER_MEAS;
import static com.cyclebikeapp.ble.BLEDataType.CSC_MEAS;
import static com.cyclebikeapp.ble.BLEDataType.HEARTRATE_MEAS;
import static com.cyclebikeapp.ble.BLEDataType.SENSOR_LOCATION;
import static com.cyclebikeapp.ble.BLEDeviceStatus.DEAD;
import static com.cyclebikeapp.ble.BLEDeviceStatus.SEARCHING;
import static com.cyclebikeapp.ble.BLEDeviceStatus.TRACKING;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_CADENCE_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_LEFT_POWER_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_POWER_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_RIGHT_POWER_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_SPDCAD_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_SPDCAD_DEVICE_OTHER;
import static com.cyclebikeapp.ble.BLEDeviceType.BIKE_SPD_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.HEARTRATE_DEVICE;
import static com.cyclebikeapp.ble.BLEDeviceType.UNKNOWN_DEVICE;
import static com.cyclebikeapp.ble.BLEUtilities.composeDeviceDialogMessage;
import static com.cyclebikeapp.ble.BLEUtilities.composeDeviceDialogTitle;
import static com.cyclebikeapp.ble.BLEUtilities.composeGPSDialogTitle;
import static com.cyclebikeapp.ble.BLEUtilities.getBTAdapterStatusString;
import static com.cyclebikeapp.ble.BLEUtilities.handleBLEDeviceInformation;
import static com.cyclebikeapp.ble.BLEUtilities.handleBikeFeature;
import static com.cyclebikeapp.ble.BLEUtilities.handleSensorLocation;
import static com.cyclebikeapp.ble.BLEUtilities.isCharacterisiticNotifiable;
import static com.cyclebikeapp.ble.BLEUtilities.isCharacteristicReadable;
import static com.cyclebikeapp.ble.BLEUtilities.isDeviceInSPList;
import static com.cyclebikeapp.ble.BLEUtilities.logCharacteristicDescriptors;
import static com.cyclebikeapp.ble.BLEUtilities.makeGattCadenceIntentFilter;
import static com.cyclebikeapp.ble.BLEUtilities.makeGattDiscoveryIntentFilter;
import static com.cyclebikeapp.ble.BLEUtilities.makeGattHRMIntentFilter;
import static com.cyclebikeapp.ble.BLEUtilities.makeGattOppositePowerIntentFilter;
import static com.cyclebikeapp.ble.BLEUtilities.makeGattPowerIntentFilter;
import static com.cyclebikeapp.ble.BLEUtilities.makeGattSpeedIntentFilter;
import static com.cyclebikeapp.ble.BLEUtilities.parseAdvertisedData;
import static com.cyclebikeapp.ble.Constants.*;
import static com.cyclebikeapp.ble.LocationUpdatesService.EXTRA_LOCATION_STATUS;
import static com.cyclebikeapp.ble.LocationUpdatesService.EXTRA_LOCATION_STATUS_TYPE;
import static com.cyclebikeapp.ble.Utilities.composeGPSDialogMessage;
import static com.cyclebikeapp.ble.Utilities.getScreenDensity;
import static com.cyclebikeapp.ble.Utilities.hasBLE;
import static com.cyclebikeapp.ble.Utilities.hasWifiInternetConnection;
import static com.cyclebikeapp.ble.Utilities.isBluetoothOn;
import static com.cyclebikeapp.ble.Utilities.isDeviceDataFaulty;
import static com.cyclebikeapp.ble.Utilities.isGPSLocationEnabled;
import static com.cyclebikeapp.ble.Utilities.isScreenWidthSmall;
import static com.cyclebikeapp.ble.Utilities.updateGPSProviderDisplay;
import static com.cyclebikeapp.ble.Utilities.updateGPSStatusDisplay;

/**
 * Copyright 2013 cyclebikeapp. All Rights Reserved.
 */

@SuppressWarnings({"deprecation", "Convert2Lambda"})
@SuppressLint("DefaultLocale")
public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    // organized static strings into a private class called .Constants
    // http://stackoverflow.com/questions/320588/interfaces-with-static-fields-in-java-for-sharing-constants
    private static int textColorWhite;
    private static int textColorHiViz;
    /**
     * this HashMap contains the data in the turn list. It is updated in the
     * refreshHashMap method and passed to CrazyAdapter to display on the
     * screen. Also use this HashMap when long-pressing item to extract the
     * distance
     */
    private final ArrayList<HashMap<String, String>> routeHashMap = new ArrayList<>();

    // display Views
    private View searchPairLayout;
    private ArrayList<HashMap<String, String>> searchPairNamesList;
    // holds the data for the searchPairNamesList
    private SearchPairAdapter mSPAdapter;
    // a scrolling list of possible devices to pair to
    private ListView spListView;
    private View mLayout;
    private TextView cadenceLabel, heartLabel, powerLabel;
    private View cadHRPowerLayout;
    private TextView tripDistLabel, tripDistTitle;
    private TextView avgSpeedLabel, maxSpeedLabel, gpsSpeedLabel;
    private TextView avgSpeedTitle, maxSpeedTitle, gpsSpeedTitle;
    private TextView tripTimeLabel, tripTimeTitle;
    private TextView appMessage, trainerModeComment;
    private View trainerModeCommentScroller;
    private Button exitTMBtn;
    private Context context;
    private View speedCell, powerCell, hrCell, cadCell;
    private View toastAnchor;
    private Snackbar mBluetoothSettingsSnackBar, mLocationSettingsSnackBar;
    private Snackbar mRequestPowerPairingSnackBar, mWarnPowerInactiveSnackBar;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothAdapter.LeScanCallback mLeDiscoveryScanCallback;
    // This is a list of devices I'll test to see if they're bike sensors
    private ArrayList<BluetoothDevice> discoveredDevicesList;
    // Lists of active Bike Devices for Pairing
    private ArrayList<HashMap<String, String>> powerDeviceSPList = new ArrayList<>();
    private ArrayList<HashMap<String, String>> hrmDeviceSPList = new ArrayList<>();

    private ArrayList<HashMap<String, String>> cadenceDeviceSPList = new ArrayList<>();
    private ArrayList<HashMap<String, String>> speedDeviceSPList = new ArrayList<>();
    private ArrayList<HashMap<String, String>> spdcadDeviceSPList = new ArrayList<>();
    private boolean deviceDiscovered;
    private BLEDeviceData hrmDeviceData, speedDeviceData, cadDeviceData;
    private BLEDeviceData powerDeviceData, oppositePowerDeviceData;

    private BLEDiscoveryService mBLEDiscoveryService;
    private BLEHRMService mBLEHRMService;
    private BLESpeedService mBLESpeedService;
    private BLECadenceService mBLECadenceService;
    private BLEPowerService mBLEPowerService;
    private BLEOppositePowerService mBLEOppositePowerService;
    // Tracks the bound state of the service.
    private boolean mLocationServiceBound = false;
    // A reference to the service used to get location updates.
    private LocationUpdatesService mLocationService;

    private PowerManager pm = null;
    private PowerManager.WakeLock mWakeLock;

    // The BroadcastReceiver used to listen for broadcasts from the Location service.
    private MyLocationReceiver myLocationsReceiver;
    /**
     * name of the route file being followed
     */
    private String chosenGPXFile = "";
    /**
     * name of the previous route file being followed need this in case the
     * chosen file doesn't load, and we have to revert
     */
    private String prevChosenFile = "";
    private BikeStat myBikeStat;
    private NavRoute myNavRoute;
    // the current location as received from the GPS sensor
    private Location myPlace = new Location(LocationManager.GPS_PROVIDER);
    // temporary Location for distance/bearing calculations
    private final Location there = new Location(LocationManager.GPS_PROVIDER);
    // generate fake locations in trainer mode
    private LocationSpoofer spoofer;
    private int prefTextColor;
    private int prefBackgroundColor;
    private boolean mResolvingBluetoothMode = false;
    private boolean mResolvingPowerPairing = false;
    private boolean mWarnedPowerInactive = false;
    private boolean testedBatterySaver = false;
    private boolean alreadyCheckedBTLEScanning = false;
    static boolean apiOkay = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH;
    /**
     * if this is the first location received, do something different in
     * BikeStat to calculate distance
     */
    private boolean gpsFirstLocation = true;

    // the scrolling turn-by-turn list
    private ListView turnByturnList;
    /**
     * a means of assembling icons and text in each row of the turn list
     */
    private TurnByTurnListAdapter turnByturnAdapter;
    // is the list still scrolling?
    private boolean scrolling = false;
    // have the unit preferences changed?
    private boolean prefChanged = true;
    /**
     * if last location is older than 3 seconds, this will be false and //
     * speedo display will show xx.x; miles to turns will show ??
     */
    //private boolean gpsLocationCurrent;
    // alternate satAcq message with this switch
    private boolean satAcqMess = true;
    /**
     * only open new tcx file in Location Listener; use this tag to force a new
     * tcx rather than re-open the old one; for example in reset()
     */
    private boolean forceNewTCX_FIT = false;
    /**
     * if we're resuming the route and loading a file, need a switch to open a
     * new or re-open the old tcx file
     */
    private boolean resumingRoute = false;
    // all the Location functions
    private LocationHelper mLocationHelper;
    // Class to manage all the BLE messaging and setup
    private BLEDeviceManager mBLEDeviceManager;
    // switch in Settings check box to use BLE sensors and initialize BLE
    private boolean useBLEData;
    private boolean autoConnectBLEAll = false;
    private BLEDBAdapter dataBaseAdapter = null;
    // switch to prevent reading and writing from BikeStat at the same time
    private boolean writingTrackRecord = false;
    // use trainer mode to spoof locations
    private boolean trainerMode = false;
    private boolean firstSpoofLocation = true;
    private final String logtag = this.getClass().getSimpleName();
    static final boolean debugOldTCXFile = false;
    static final boolean debugAppState = false;
    static final boolean debugLEScan = false;
    static final boolean debugRefreshTiming = false;
    private static final boolean debugLocation = false;
    private static final boolean debugMessageBox = false;
    static final boolean debugfit = false;
    static final boolean debugBLEService = false;
    static final boolean debugBLEData = false;
    static final boolean debugBLEPowerData = false;
    private static final boolean debugBLEPowerCadence = false;
    static final boolean debugBLEPowerCrank = false;
    static final boolean debugBLEPowerWheel = false;
    static final boolean debugBLEPowerCal = false;
    private static final boolean debugBLESearchPair = false;

    private BLEDeviceType searchPairDeviceType = BLEDeviceType.UNKNOWN_DEVICE;
    private View myCoordinatorLayout;
    private boolean allowBLEScanner = true;


    @SuppressLint("WakelockTimeout")
    @Override

    public void onCreate(Bundle savedInstanceState) {
        if (debugAppState) Log.i(logtag, "onCreate()");
        super.onCreate(savedInstanceState);
        mBLEDeviceManager = new BLEDeviceManager(getApplicationContext());
        setContentView(R.layout.activity_scroller);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        turnByturnList = findViewById(R.id.list);
		/* should the app keep the screen on - not if we're paused */
        turnByturnList.setKeepScreenOn(true);
		/* handles the scrolling action */
        turnByturnList.setOnScrollListener(scrollListener);
		/* responds to long-press in turn list */
        turnByturnList.setOnItemLongClickListener(longClickListener);
        turnByturnList.setOnItemClickListener(turnListOnClickListener);
        context = getApplicationContext();
        Utilities.setRequestingLocationUpdates(context, false);
        textColorWhite = ContextCompat.getColor(context, R.color.white);
        textColorHiViz = ContextCompat.getColor(context, R.color.texthiviz);
        prefTextColor = textColorWhite;
        prefBackgroundColor = ContextCompat.getColor(context, R.color.bkgnd_gray);
        myBikeStat = new BikeStat(context);
        myNavRoute = new NavRoute(context);
        spoofer = new LocationSpoofer(context);
        discoveredDevicesList = new ArrayList<>();
        searchPairNamesList = new ArrayList<>();
        mSPAdapter = new SearchPairAdapter(this, searchPairNamesList);

        speedDeviceData = new BLEDeviceData(null, BIKE_SPD_DEVICE);
        cadDeviceData = new BLEDeviceData(null, BIKE_CADENCE_DEVICE);
        hrmDeviceData = new BLEDeviceData(null, HEARTRATE_DEVICE);
        powerDeviceData = new BLEDeviceData(null, BIKE_POWER_DEVICE);
        oppositePowerDeviceData = new BLEDeviceData(null, BIKE_POWER_DEVICE);

        myLocationsReceiver = new MyLocationReceiver();
        initializeScreen();
        myPlace = Utilities.getLocFromSharedPrefs(context);
        mLocationSettingsSnackBar = Snackbar.make(
                myCoordinatorLayout,
                getString(R.string.open_location_settings),
                Snackbar.LENGTH_INDEFINITE);

        mLocationHelper = new LocationHelper(getApplicationContext());

        googlePlayAvailable(context);
        mBLEDeviceManager = new BLEDeviceManager(context);
        // show screen early
        refreshScreen();
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"CBSMART");
        }
        mWakeLock.acquire();
        autoResumeRoute();
        dataBaseAdapter = new BLEDBAdapter(context);
    }// onCreate


    /**
     * if tcx file is not old, resume previous route
     */
    private void autoResumeRoute() {
        if (debugOldTCXFile) { Log.i(logtag, "autoResumeRoute()"); }
        // called from onCreate()
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean old = myBikeStat.tcxLog.readTCXFileLastModTime(
                settings.getString(TCX_LOG_FILE_NAME, ""),
                Utilities.getTCXFileAutoReset(getApplicationContext()));
        // read .lastModifiedTime from SharedPreferences rather than from File
        if (!old) {
            restoreSharedPrefs();
            myNavRoute.mChosenFile = new File(chosenGPXFile);
            prefChanged = true;
            refreshScreen();
            resumingRoute = true;
            // load file in async task with progress bar in case file is big it would generate ANR error
            new LoadData().execute();
        } else {// output file is old
            mBLEDeviceManager.restartHR(myBikeStat.getAvgHeartRate(), myBikeStat.getAvgHeartRate());
            myBikeStat.setAvgHeartRate(0);
            myBikeStat.setMaxHeartRate(0);
            mBLEDeviceManager.restartCadence();
            mBLEDeviceManager.restartPower();
            mBLEDeviceManager.restartWheelCal(myBikeStat.getWheelTripDistance());
            mBLEDeviceManager.restartPowerWheelCal(myBikeStat.getWheelTripDistance());
            deleteAllTmpRouteFiles();
            // have to put this in shared prefs, or the old file name is loaded in onResume
            chosenGPXFile = "";
            settings.edit().putString(KEY_CHOSEN_GPXFILE, chosenGPXFile).apply();
        }
    }

    /**
     * Retrieve application persistent data.
     */
    private void loadBLEConfiguration() {
        if (debugAppState) Log.i(logtag, "loadBLEConfiguration()");
        // Restore static preferences; is called from initializeBLE()
        // note that all sensor data from a "live" session are loaded from restoreSharedPrefs()
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mBLEDeviceManager.wheelCnts.isCalibrated = settings.getBoolean(WHEEL_IS_CAL, false);
        mBLEDeviceManager.powerWheelCnts.isCalibrated = settings.getBoolean(POWER_WHEEL_IS_CAL, false);
        // trainer mode parameters
        trainerMode = settings.getBoolean(KEY_TRAINER_MODE, false);
        autoConnectBLEAll = settings.getBoolean(KEY_AUTO_CONNECT_ALL, false);
        //new loadDBDeviceListBackground().execute();
        new ThreadPerTaskExecutor().execute(loadDBDeviceListRunnable);
    }

    private void initializeBLE() {
        // Called from onResume() -> startSensors()
        if (debugAppState) Log.i(logtag, "initializeBLE()");
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (Utilities.hasBLE(context)) {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                mBluetoothAdapter = bluetoothManager.getAdapter();
            }
        } else {
            Toast.makeText(this, R.string.ble_not_supported_or_inactive, Toast.LENGTH_SHORT).show();
        }
        loadBLEConfiguration();
        initializeLEScanCallback();
        if (hrmDeviceData != null) {
            myBikeStat.hasHR = (hrmDeviceData.status == TRACKING);
        }
        if (cadDeviceData != null) {
            myBikeStat.hasCadence = (cadDeviceData.status == TRACKING);
        }
        if (speedDeviceData != null) {
            myBikeStat.hasSpeedSensor = (speedDeviceData.status == TRACKING);
            myBikeStat.hasCalSpeedSensor = myBikeStat.hasSpeedSensor && mBLEDeviceManager.wheelCnts.isCalibrated;
        }
        if (powerDeviceData != null) {
            myBikeStat.hasPower = (powerDeviceData.status == TRACKING);
            myBikeStat.hasCalPowerSpeedSensor = myBikeStat.hasPower && mBLEDeviceManager.powerWheelCnts.isCalibrated;
        }
        // if the speed sensor is not calibrated, read shared prefs to use entered value, or default
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if (!myBikeStat.hasCalSpeedSensor) {
            mBLEDeviceManager.wheelCnts.wheelCircumference = Double.valueOf(settings
                    .getString(WHEEL_CIRCUM, DOUBLE_ZERO));
            // set wheel circumference to a default value if stored value out of range
            if ((mBLEDeviceManager.wheelCnts.wheelCircumference > UPPER_WHEEL_CIRCUM)
                    || (mBLEDeviceManager.wheelCnts.wheelCircumference < LOWER_WHEEL_CIRCUM)) {
                mBLEDeviceManager.wheelCnts.wheelCircumference = DEFAULT_WHEEL_CIRCUM;
            }
        }
        // if the power speed sensor is not calibrated, read shared prefs to use same default value
        if (!myBikeStat.hasCalPowerSpeedSensor) {
            mBLEDeviceManager.powerWheelCnts.wheelCircumference = Double
                    .valueOf(settings.getString(POWER_WHEEL_CIRCUM, DOUBLE_ZERO));
            // set wheel circumference to a default value if stored value out of range
            if ((mBLEDeviceManager.powerWheelCnts.wheelCircumference > UPPER_WHEEL_CIRCUM)
                    || (mBLEDeviceManager.powerWheelCnts.wheelCircumference < LOWER_WHEEL_CIRCUM)) {
                mBLEDeviceManager.powerWheelCnts.wheelCircumference = DEFAULT_WHEEL_CIRCUM;
            }
        }
    }

    private void initializeMergedRouteTurnList() {
        // Called from changeTrackDensityBackground and LoadData. Don't calculate the route distances
        // for the whole list here. Maybe do that in a background task, but it has to be re-done for refresh hashmap anyway
        String unit;
        int dimLevel = prefTextColor;
        int distanceUnit = Utilities.getDistanceUnit(context);
        if (distanceUnit == DISTANCE_TYPE_MILE) {
            unit = MILE;
        } else {
            unit = KM;
        }
        // This is the TrackPoint density-reduced array that matches the index of
        // the routeHashMap. This makes it easy to match index in refreshHashMap
        myNavRoute.mergedRoute_HashMap.clear();
        routeHashMap.clear();// clears the HashMap
        // go thru the mergedRoute and convert to HashMap
        // called from dealWithGoodData after LoadData
        if (myNavRoute.mergedRoute != null) {
            for (int i = 0; i < myNavRoute.mergedRoute.size(); i++) {
                if (!myNavRoute.mergedRoute.get(i).delete) {
                    GPXRoutePoint tempRP;
                    tempRP = myNavRoute.mergedRoute.get(i);
                    // copying to new GPXRoutePoint ArrayList
                    myNavRoute.mergedRoute_HashMap.add(tempRP);
                }
            }
        } else {// mergedRoute == null
            return;
        }
        for (int i = 0; i < myNavRoute.mergedRoute_HashMap.size(); i++) {
            HashMap<String, String> hmItem = new HashMap<>();
            int turnDirIcon = myNavRoute.mergedRoute_HashMap.get(i).turnIconIndex;
            if (Utilities.isColorSchemeHiViz(context)) {
                // don't put hiViz "X" icon; other hiViz icons have iconLevel +18 for high-viz color
                if (turnDirIcon != 99) {
                    turnDirIcon += 18;
                }
            }
            String streetName = myNavRoute.mergedRoute_HashMap.get(i).getStreetName();
            String distanceString = QUESTION;
            if ((i > turnByturnList.getFirstVisiblePosition() - 1)
                    && (i < turnByturnList.getLastVisiblePosition() + 1)) {
                float result[];
                result = distFromMyPlace2WPMR(i, Utilities.getDistanceType(context));
                // results returns in miles; convert to meters, if needed
                double distMultiplier = mile_per_meter;
                if (distanceUnit == DISTANCE_TYPE_MILE) {
                    unit = MILE;
                } else {
                    distMultiplier = km_per_meter;
                    unit = KM;
                }
                double distance = result[0] * distMultiplier;
                if (isGPSLocationCurrent()) {
                    distanceString = String.format(FORMAT_1F, distance);
                }
                if (distance < 0.1) {// switch to display in feet / m
                    int dist;
                    // increment in multiples of 20', a likely resolution limit
                    if (distanceUnit == DISTANCE_TYPE_MILE) {
                        dist = (int) Math.floor(distance * 264) * 20;
                        unit = FOOT;
                    } else {
                        dist = (int) Math.floor(distance * 100) * 10;
                        unit = METER;
                    }
                    if (isGPSLocationCurrent()) {
                        distanceString = String.format(FORMAT_3D, dist);
                    }
                }// if dist<0.1
            }// only calculate distance for visible turns; this will be re-done
            // in refreshHashMap, but it may take a while in large lists
            int bearingIcon = myNavRoute.mergedRoute_HashMap.get(i).relBearIconIndex;
            // creating new HashMap
            hmItem.put(KEY_TURN, Integer.toString(turnDirIcon));
            hmItem.put(KEY_STREET, streetName);
            hmItem.put(KEY_DISTANCE, distanceString);
            hmItem.put(KEY_UNIT, unit);
            hmItem.put(KEY_BEARING, Integer.toString(bearingIcon));
            hmItem.put(KEY_DIM, Integer.toString(dimLevel));
            routeHashMap.add(hmItem);
        }// for loop
        // add a blank item to the bottom of the list
        HashMap<String, String> hmItem = new HashMap<>();
        hmItem.put(KEY_TURN, Integer.toString(99));
        hmItem.put(KEY_STREET, "");
        hmItem.put(KEY_DISTANCE, "");
        hmItem.put(KEY_UNIT, "");
        hmItem.put(KEY_BEARING, Integer.toString(0));
        hmItem.put(KEY_DIM, ZERO);
        routeHashMap.add(hmItem);
        turnByturnAdapter.notifyDataSetChanged();
    }

    private void initializeScreen() {
        float screenDensity = getScreenDensity(context);
        if (debugAppState){
            int[] screenSize = Utilities.getScreenSize(context);
            Log.i(logtag, "screen width/height: " + screenSize[0] + "/" + screenSize[1]
            + " screen density: " + getScreenDensity(context)
                    + " screen is small: " + (isScreenWidthSmall(context)?"yes":"no"));
        }
        View mGPSBLELayout = findViewById(R.id.gps_bt_status_layout);
        if (debugLocation || debugBLEService || debugLEScan || Utilities.showGPSStatus(context)){
            mGPSBLELayout.setVisibility(View.VISIBLE);
        } else {
            mGPSBLELayout.setVisibility(View.GONE);
        }
        if (Utilities.isColorSchemeHiViz(context)) {
            prefBackgroundColor = ContextCompat.getColor(context, R.color.bkgnd_black);
            prefTextColor = textColorHiViz;
        } else {
            prefTextColor = textColorWhite;
            prefBackgroundColor = ContextCompat.getColor(context, R.color.bkgnd_gray);
        }
        mLayout = findViewById(R.id.RelativeLayout101);
        mLayout.setBackgroundColor(prefBackgroundColor);
        myCoordinatorLayout = findViewById(R.id.myCoordinatorLayout);
        initHashMap();
        // the area of the screen displaying power, heart-rate or cadence
        // need the view to set on-click listener
        View distanceCell = findViewById(R.id.distanceCell);
        distanceCell.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                dismissAllSnackbars();
                showGPSDialog();
                return true;
            }
        });
        speedCell = findViewById(R.id.speedcell);
        speedCell.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View view) {
                doCellLongClick(BIKE_SPD_DEVICE);
                return true;
            }
        });
        powerCell = findViewById(R.id.powercell);
        powerCell.setOnClickListener(powerCellClickListener);
        powerCell.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View view) {
                doCellLongClick(BIKE_POWER_DEVICE);
                return true;
            }
        });
        hrCell = findViewById(R.id.hrcell);
        hrCell.setOnClickListener(hrCellClickListener);
        hrCell.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View view) {
                doCellLongClick(HEARTRATE_DEVICE);
                return true;
            }
        });
        cadCell = findViewById(R.id.cadcell);
        cadCell.setOnClickListener(cadCellClickListener);
        cadCell.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View view) {
                doCellLongClick(BIKE_CADENCE_DEVICE);
                return true;
            }
        });
        // need to be able to view or hide search/pair layout
        searchPairLayout = findViewById(R.id.search_pair_layout);
        searchPairLayout.setVisibility(View.GONE);
        spListView = findViewById(R.id.sp_device_list);
        spListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        spListView.setAdapter(mSPAdapter);
        // need to be able to view or hide Cad-HR-Power layout
        cadHRPowerLayout = findViewById(R.id.cad_hr_power_layout);
        // this is a place to display sensor data messages
        toastAnchor = findViewById(R.id.bikestat_layout);
        cadenceLabel = findViewById(R.id.cad_text);
        heartLabel = findViewById(R.id.hr_text);
        powerLabel = findViewById(R.id.power_text);
        tripDistTitle = findViewById(R.id.tripdist_cell_title);
        tripDistLabel = findViewById(R.id.tripdist_cell_value);
        tripTimeTitle = findViewById(R.id.ridetime_cell_title);
        tripTimeLabel = findViewById(R.id.ridetime_cell_value);
        avgSpeedTitle = findViewById(R.id.avgspeed_cell_title);
        avgSpeedLabel = findViewById(R.id.avgspeed_cell_value);
        maxSpeedTitle = findViewById(R.id.maxspeed_cell_title);
        maxSpeedLabel = findViewById(R.id.maxspeed_cell_value);
        appMessage = findViewById(R.id.message_box);
        gpsSpeedTitle = findViewById(R.id.speedcell_speed_title);
        gpsSpeedLabel = findViewById(R.id.speedcell_speed_value);
        float titleSize = TITLE_SIZE_PIXELS/screenDensity;
        float valueSize = VALUE_SIZE_PIXELS/screenDensity;
        float speedSize = SPEED_SIZE_PIXELS/screenDensity;
        if (isScreenWidthSmall(context)) {
            tripDistTitle.setTextSize(COMPLEX_UNIT_DIP, titleSize);
            tripDistLabel.setTextSize(COMPLEX_UNIT_DIP, valueSize);
            tripTimeTitle.setTextSize(COMPLEX_UNIT_DIP, titleSize);
            tripTimeLabel.setTextSize(COMPLEX_UNIT_DIP, valueSize);
            avgSpeedTitle.setTextSize(COMPLEX_UNIT_DIP, titleSize);
            avgSpeedLabel.setTextSize(COMPLEX_UNIT_DIP, valueSize);
            maxSpeedTitle.setTextSize(COMPLEX_UNIT_DIP, titleSize);
            maxSpeedLabel.setTextSize(COMPLEX_UNIT_DIP, valueSize);
            gpsSpeedTitle.setTextSize(COMPLEX_UNIT_DIP, titleSize);
            gpsSpeedLabel.setTextSize(COMPLEX_UNIT_DIP, speedSize);
        }
        trainerModeCommentScroller = findViewById(R.id.trainer_mode_comment_scrollview);
        trainerModeComment = findViewById(R.id.trainer_mode_comment);
        exitTMBtn = findViewById(R.id.exit_trainer_mode_btn);
        exitTMBtn.setOnClickListener(exitTrainerModeButtonClickListener);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        useBLEData = settings.getBoolean(USE_BLE, true);
        prefChanged = true;
        testBatteryOptimization();
    }// initializeScreen()

    private void doCellLongClick(BLEDeviceType aDeviceType) {
        if (debugAppState) Log.i(logtag, "doCellLongClick()");
        if (!isBluetoothOn(getApplicationContext())) {
            dismissAllSnackbars();
            requestBluetoothModeOn();
            return;
        }
        //if DeviceData is null or DEAD, do search/pair, else show device info
        int backgroundColor = ContextCompat.getColor(context, R.color.bkgnd_green);
        switch (aDeviceType) {
            case BIKE_CADENCE_DEVICE:
                if (cadDeviceData == null || cadDeviceData.status != TRACKING) {
                    cadCell.setBackgroundColor(backgroundColor);
                    doSearchPair(aDeviceType);
                } else {
                    //show device info
                    showDialog(cadDeviceData.getAddress());
                }
                break;
            case HEARTRATE_DEVICE:
                if (hrmDeviceData == null || hrmDeviceData.status != TRACKING) {
                    hrCell.setBackgroundColor(backgroundColor);
                    doSearchPair(aDeviceType);
                } else {
                    //show device info
                    showDialog(hrmDeviceData.getAddress());
                }
                break;
            case HEARTRATE_DEVICE_BELT:
                if (hrmDeviceData == null || hrmDeviceData.status != TRACKING) {
                    hrCell.setBackgroundColor(backgroundColor);
                    doSearchPair(aDeviceType);
                } else {
                    //show device info
                    showDialog(hrmDeviceData.getAddress());
                }
                break;
            case NOT_BIKE_DEVICE:
            case UNKNOWN_DEVICE:
                break;
            case BIKE_POWER_DEVICE:
                if (powerDeviceData == null || powerDeviceData.status != TRACKING) {
                    powerCell.setBackgroundColor(backgroundColor);
                    doSearchPair(aDeviceType);
                } else {
                    //show device info
                    showDialog(powerDeviceData.getAddress());
                }
                break;
            case BIKE_SPD_DEVICE:
                if (speedDeviceData == null || speedDeviceData.status != TRACKING) {
                    doSearchPair(aDeviceType);
                } else {
                    //show device info
                    showDialog(speedDeviceData.getAddress());
                }
                break;
            case BIKE_SPDCAD_DEVICE:
                if (speedDeviceData == null || speedDeviceData.status != TRACKING) {
                    doSearchPair(aDeviceType);
                } else {
                    //show device info
                    showDialog(speedDeviceData.getAddress());
                }
                break;
            case BIKE_SPDCAD_DEVICE_OTHER:
                if (speedDeviceData == null || speedDeviceData.status != TRACKING) {
                    doSearchPair(aDeviceType);
                } else {
                    //show device info
                    showDialog(speedDeviceData.getAddress());
                }
                break;
            case BIKE_LEFT_POWER_DEVICE:
                if (powerDeviceData == null || powerDeviceData.status != TRACKING) {
                    doSearchPair(aDeviceType);
                } else {
                    //show device info
                    showDialog(powerDeviceData.getAddress());
                }
                break;
            case BIKE_RIGHT_POWER_DEVICE:
                if (powerDeviceData == null || powerDeviceData.status != TRACKING) {
                    doSearchPair(aDeviceType);
                } else {
                    //show device info
                    showDialog(powerDeviceData.getAddress());
                }
                break;
        }
    }

    /**
     * the HashMap is the turn-by-turn rows in the scrolling list
     */
    private void initHashMap() {
        routeHashMap.clear();
        HashMap<String, String> hmItem = new HashMap<>();
        for (int j = 0; j < 7; j++) {
            // just put an X for turn and north direction arrow in the list
            hmItem.put(KEY_TURN, Integer.toString(99));
            hmItem.put(KEY_STREET, "");
            hmItem.put(KEY_DISTANCE, "");
            hmItem.put(KEY_UNIT, "");
            hmItem.put(KEY_BEARING, ZERO);
            hmItem.put(KEY_DIM, ZERO);
            routeHashMap.add(hmItem);
        }
        turnByturnAdapter = new TurnByTurnListAdapter(this, routeHashMap);
        turnByturnList.setAdapter(turnByturnAdapter);
    }

    // this operates the turn-list scroller
    private final OnScrollListener scrollListener = new OnScrollListener() {
        public void onScroll(AbsListView view, int firstItem,
                int visibleItemCount, int totalItemCount) {
        }

        public void onScrollStateChanged(AbsListView view, int scrollState) {
            switch (scrollState) {
                case OnScrollListener.SCROLL_STATE_IDLE:
                    if (scrolling) {
                        myNavRoute.firstListElem = view.getFirstVisiblePosition();
                        scrolling = false;
                    }
                    refreshScreen();
                    break;
                case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                case OnScrollListener.SCROLL_STATE_FLING:
                    scrolling = true;
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * display max, average power and total energy when the power display area is clicked
     */
    private final View.OnClickListener powerCellClickListener = new View.OnClickListener() {

        @SuppressLint("RtlHardcoded")
        @Override
        public void onClick(final View v) {
            dismissAllSnackbars();
            String toastText = getString(R.string.max_power, myBikeStat.getMaxPower())
                    + getString(R.string.avg_power, myBikeStat.getAvgPower())
                    + getString(R.string.tot_energy, (int) (mBLEDeviceManager.getCumEnergy() / 1000));
            viewToast(toastText, -80, Gravity.TOP | Gravity.RIGHT, v, textColorWhite);
        }
    };

    /**
     * display max, average heart rate when the hr display area is clicked
     */
    private final View.OnClickListener hrCellClickListener = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            dismissAllSnackbars();
            String toastText = getString(R.string.max_hr, myBikeStat.getMaxHeartRate())
                    + getString(R.string.avg_hr, myBikeStat.getAvgHeartRate());
            viewToast(toastText, -80, Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                    v, textColorWhite);
        }
    };

    /**
     * display max, average cadence when the cad display area is clicked
     */
    private final View.OnClickListener cadCellClickListener = new View.OnClickListener() {

        @SuppressLint("RtlHardcoded")
        @Override
        public void onClick(final View v) {
            dismissAllSnackbars();
            String avgCadText = ZERO;
            String maxCadText = ZERO;
            // if we have a cadence sensor (max cad > 0) use that
            if (myBikeStat.getMaxCadence() > 0) {
                avgCadText = Integer.toString(myBikeStat.getAvgCadence());
                maxCadText = Integer.toString(myBikeStat.getMaxCadence());
            }
            String toastText = getString(R.string.max_cad, maxCadText) + getString(R.string.avg_cad, avgCadText);
            viewToast(toastText, -80, Gravity.TOP | Gravity.LEFT, v, textColorWhite);
        }
    };

    private final View.OnClickListener exitTrainerModeButtonClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            exitTrainerMode();
        }
    };

    private final AdapterView.OnItemClickListener turnListOnClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            dismissAllSnackbars();
        }
    };

    private final OnItemLongClickListener longClickListener = new OnItemLongClickListener() {
        public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos,
                long id) {
            // only respond to long-click if location is current & there is a route
            if ((myNavRoute.mergedRoute_HashMap.size() < 1)
                    || (!isGPSLocationCurrent() && !isFusedLocationCurrent())) {
                return true;
            }
            if (checkNearEnough(pos)) {
                // only if we're near to the clicked way point
                // Now do all the nitty-gritty of re-navigating from here
                // set bonus miles so logic will recognize way points
                myNavRoute.setBonusMiles(myBikeStat.getGPSTripDistance()
                        - myNavRoute.mergedRoute_HashMap.get(pos).getRouteMiles());
                if (debugAppState)
                    Log.i(logtag + "LongClick", "pos: " + pos
                            + " tripDist: " + myBikeStat.getGPSTripDistance()
                            + " routeMiles: " + myNavRoute.mergedRoute_HashMap.get(pos).getRouteMiles());
                // set .beenThere = false for all way points
                // refreshHashMap will re-set .beenThere for all waypoints
                for (int index = 0; index < myNavRoute.mergedRoute_HashMap.size(); index++) {
                    GPXRoutePoint tempRP;
                    tempRP = myNavRoute.mergedRoute_HashMap.get(index);
                    tempRP.setBeenThere(false);
                    myNavRoute.mergedRoute_HashMap.set(index, tempRP);
                }// for all way points in the route
            }
            return true;
        }
    };


    private void saveState() {
        if (debugAppState) {
            Log.i(logtag, "saveState()");
        }
        final long startTime = System.nanoTime();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        // remove all the paired BLE device numbers so it doesn't get re-loaded
        editor.putInt(DEVICE_ADDRESS_HRM, 0);
        editor.putInt(DEVICE_ADDRESS_SPEED, 0);
        editor.putInt(DEVICE_ADDRESS_CADENCE, 0);
        editor.putInt(DEVICE_ADDRESS_SPEED_CADENCE, 0);
        editor.putInt(DEVICE_ADDRESS_POWER, 0);
        // all the cadence sensor parameters
        editor.putInt(PREV_PED_CNTS, (int) mBLEDeviceManager.crankCadenceCnts.prevCount);
        editor.putBoolean(PED_CNTS_INIT, mBLEDeviceManager.crankCadenceCnts.initialized);
        editor.putInt(OPPOSITE_PREV_PED_CNTS, (int) mBLEDeviceManager.oppositeCrankCadenceCnts.prevCount);
        editor.putBoolean(OPPOSITE_PED_CNTS_INIT, mBLEDeviceManager.oppositeCrankCadenceCnts.initialized);
        editor.putInt(NUM_PEDAL_CAD, (int) mBLEDeviceManager.getNumPedalCad());
        editor.putInt(TOTAL_PEDAL_CAD, (int) mBLEDeviceManager.getTotalPedalCad());
        editor.putInt(AVG_CADENCE, myBikeStat.getAvgCadence());
        editor.putInt(MAX_CADENCE, myBikeStat.getMaxCadence());

        // all the HR sensor parameters
        editor.putInt(TOTAL_HR_COUNTS, (int) mBLEDeviceManager.getTotalHRCounts());
        editor.putInt(NUM_HR_EVENTS, (int) mBLEDeviceManager.getNumHREvents());
        editor.putInt(AVG_HR, myBikeStat.getAvgHeartRate());
        editor.putInt(MAX_HR, myBikeStat.getMaxHeartRate());

        // all the speed sensor parameters
        editor.putInt(NUM_WHEEL_CNTS, (int) mBLEDeviceManager.wheelCnts.calTotalCount);
        editor.putLong(WHEEL_CUMREV, mBLEDeviceManager.wheelCnts.cumulativeRevolutions);
        editor.putLong(WHEEL_CUMREV_AT_START, mBLEDeviceManager.wheelCnts.cumulativeRevsAtCalStart);
        editor.putBoolean(WHEEL_IS_CAL, mBLEDeviceManager.wheelCnts.isCalibrated);
        editor.putString(WHEEL_CIRCUM, Double.toString(mBLEDeviceManager.wheelCnts.wheelCircumference));
        editor.putString(START_DIST, Double.toString(mBLEDeviceManager.wheelCnts.calGPSStartDist));
        editor.putString(MAX_SPEED, Double.toString(myBikeStat.getMaxSpeed()));
        editor.putString(WHEEL_TRIP_TIME, Double.toString(myBikeStat.getWheelRideTime()));
        editor.putString(WHEEL_TRIP_DISTANCE, Double.toString(myBikeStat.getWheelTripDistance()));
        editor.putLong(WHEEL_PREV_COUNT, mBLEDeviceManager.wheelCnts.prevCount);

        // power data
        editor.putBoolean(POWER_CNTS_INIT, mBLEDeviceManager.calcPowerData.initialized);
        editor.putBoolean(OPPOSITE_POWER_CNTS_INIT, mBLEDeviceManager.oppositeCalcPowerData.initialized);
        editor.putInt(CUM_ENERGY, (int) mBLEDeviceManager.getCumEnergy());
        editor.putString(CUM_POWER_TIME, Double.toString(mBLEDeviceManager.getCumPowerTime()));
        editor.putInt(AVG_POWER, myBikeStat.getAvgPower());
        editor.putInt(MAX_POWER, myBikeStat.getMaxPower());

        // calculated crank cadence data from power meter
        editor.putInt(TOTAL_CALC_CAD, (int) mBLEDeviceManager.getTotalCalcCrankCad());
        editor.putInt(NUM_CALC_CAD, (int) mBLEDeviceManager.getNumCalcCrankCad());
        editor.putInt(AVG_CALC_CADENCE, myBikeStat.getAvgCadence());
        editor.putInt(MAX_CALC_CADENCE, myBikeStat.getMaxCadence());

        // power wheel data
        editor.putLong(POWER_WHEEL_CUMREV, mBLEDeviceManager.powerWheelCnts.cumulativeRevolutions);
        editor.putLong(POWER_WHEEL_PREV_COUNT, mBLEDeviceManager.powerWheelCnts.prevCount);
        editor.putLong(POWER_WHEEL_CUMREV_AT_START, mBLEDeviceManager.powerWheelCnts.cumulativeRevsAtCalStart);
        editor.putInt(NUM_POWER_WHEEL_CNTS, (int) mBLEDeviceManager.powerWheelCnts.calTotalCount);
        editor.putBoolean(POWER_WHEEL_IS_CAL, mBLEDeviceManager.powerWheelCnts.isCalibrated);
        editor.putString(POWER_WHEEL_CIRCUM, Double.toString(mBLEDeviceManager.powerWheelCnts.wheelCircumference));
        editor.putString(POWER_START_DIST, Double.toString(mBLEDeviceManager.powerWheelCnts.calGPSStartDist));
        editor.putString(POWER_WHEEL_TRIP_TIME, Double.toString(myBikeStat.getPowerWheelRideTime()));
        editor.putString(POWER_WHEEL_TRIP_DISTANCE, Double.toString(myBikeStat.getPowerWheelTripDistance()));
        editor.putBoolean(KEY_AUTO_CONNECT_ALL, autoConnectBLEAll);

        editor.putString(SAVED_LAT, Double.toString(myPlace.getLatitude()));
        editor.putString(SAVED_LON, Double.toString(myPlace.getLongitude()));
        editor.putLong(PREF_SAVED_LOC_TIME, myPlace.getTime());
        editor.putString(TRIP_TIME, Double.toString(myBikeStat.getGPSRideTime()));
        editor.putString(TRIP_DISTANCE, Double.toString(myBikeStat.getGPSTripDistance()));
        editor.putString(WHEEL_TRIP_DISTANCE, Double.toString(myBikeStat.getWheelTripDistance()));
        editor.putString(PREV_WHEEL_TRIP_DISTANCE, Double.toString(myBikeStat.getPrevWheelTripDistance()));
        editor.putString(SPOOF_WHEEL_TRIP_DISTANCE, Double.toString(myBikeStat.getSpoofWheelTripDistance()));
        editor.putString(PREV_SPOOF_WHEEL_TRIP_DISTANCE, Double.toString(myBikeStat.getPrevSpoofWheelTripDistance()));
        editor.putBoolean(KEY_TRAINER_MODE, trainerMode);
        editor.putString(BONUS_MILES, Double.toString(myNavRoute.getBonusMiles()));
        editor.putInt(CURR_WP, myNavRoute.currWP);
        editor.putInt(FIRST_LIST_ELEM, myNavRoute.firstListElem);
        editor.putString(KEY_CHOSEN_GPXFILE, chosenGPXFile);
        editor.putString(TCX_LOG_FILE_NAME, myBikeStat.tcxLog.outFileName);
        editor.putInt(TCX_LOG_FILE_FOOTER_LENGTH, myBikeStat.tcxLog.outFileFooterLength);
        editor.putBoolean(KEY_FORCE_NEW_TCX, forceNewTCX_FIT);
        editor.apply();
        updateDBData(" - from saveState()");
        if (debugRefreshTiming) {
            Log.i(logtag, "saving state duration: "
                    + String.format(FORMAT_4_1F, (System.nanoTime() - startTime) / 1000000.) + " msec");
        }
    }
    @Override
    protected void onStart() {
        if (debugAppState) { Log.i(logtag, "onStart()"); }
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(new Intent(this, LocationUpdatesService.class), mLocationServiceConnection, BIND_AUTO_CREATE);

        bindService(new Intent(this, BLEDiscoveryService.class), mServiceConnection, BIND_AUTO_CREATE);
        bindService(new Intent(this, BLEHRMService.class), mHRMServiceConnection, BIND_AUTO_CREATE);
        bindService(new Intent(this, BLESpeedService.class), mSpeedServiceConnection, BIND_AUTO_CREATE);
        bindService(new Intent(this, BLECadenceService.class), mCadenceServiceConnection, BIND_AUTO_CREATE);
        bindService(new Intent(this, BLEPowerService.class), mPowerServiceConnection, BIND_AUTO_CREATE);
        bindService(new Intent(this, BLEOppositePowerService.class), mOppositePowerServiceConnection, BIND_AUTO_CREATE);
        registerReceiver(mGattDiscoveryReceiver, makeGattDiscoveryIntentFilter());
        registerReceiver(mGattHRMMeasurementReceiver, makeGattHRMIntentFilter());
        registerReceiver(mGattSpeedMeasurementReceiver, makeGattSpeedIntentFilter());
        registerReceiver(mGattCadenceMeasurementReceiver, makeGattCadenceIntentFilter());
        registerReceiver(mGattPowerMeasurementReceiver, makeGattPowerIntentFilter());
        registerReceiver(mGattOppositePowerMeasurementReceiver, makeGattOppositePowerIntentFilter());
    }
    @Override
    protected void onResume() {
        if (debugAppState) {
            Log.i(logtag, "onResume()");
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myLocationsReceiver);
        LocalBroadcastManager.getInstance(this).registerReceiver(myLocationsReceiver,
                new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));
        try {
            if (dataBaseAdapter == null) {
                Log.e(logtag, "dataBaseAdapter is null");
            } else {
                dataBaseAdapter.close();
                dataBaseAdapter.open();
            }
        } catch (SQLiteException e) { e.printStackTrace(); }
        super.onResume();
        //save the name of the route file temporarily until its validated
        prevChosenFile = chosenGPXFile;
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        chosenGPXFile = settings.getString(KEY_CHOSEN_GPXFILE, "");
        myBikeStat.tcxLog.outFileName = settings.getString(TCX_LOG_FILE_NAME, "");
        myBikeStat.tcxLog.outFileFooterLength = settings.getInt(TCX_LOG_FILE_FOOTER_LENGTH, 1);
        refreshScreen();
        startSensors();
        if (askLocationPermission()) {
            if (mLocationServiceBound){
                mLocationService.requestLocationUpdates();
                if (debugLocation) { Log.i(logtag, "mLocationService.requestLocationUpdates()"
                + (Utilities.requestingLocationUpdates(context)?" not requesting":" are already requesting")); }
            }
            mLocationHelper.stopLocationUpdates();
            mLocationHelper.startLocationUpdates();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // handle the preference change here
        if (debugAppState) { Log.i(logtag, "onSharedPreferenceChanged() - key: " + key); }
        switch (key) {
            case HI_VIZ:
                if (Utilities.isColorSchemeHiViz(context)) {
                    prefBackgroundColor = ContextCompat.getColor(context, R.color.bkgnd_black);
                    prefTextColor = textColorHiViz;
                } else {
                    prefTextColor = textColorWhite;
                    prefBackgroundColor = ContextCompat.getColor(context, R.color.bkgnd_gray);
                }
                break;
            case GPS_STATUS:
                View mGPSBLELayout = findViewById(R.id.gps_bt_status_layout);
                if (sharedPreferences.getBoolean(GPS_STATUS, false)) {
                    mGPSBLELayout.setVisibility(View.VISIBLE);
                } else {
                    mGPSBLELayout.setVisibility(View.GONE);
                }
                break;
            case KEY_TRACKPOINT_DENSITY_DEFAULT:
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                //if we're returning from SettingsActivity, test if Track Point density
                // has changed. If so we must re-load chosenFile
                // see if SharedPreferences value of trackDensity is different than DefaultSharedPreferences
                int trackDensity = settings.getInt(KEY_TRACK_DENSITY, 0);
                myNavRoute.defaultTrackDensity = 0;
                SharedPreferences defaultSettings = PreferenceManager.getDefaultSharedPreferences(this);
                String defTrackDensity = defaultSettings.getString(KEY_TRACKPOINT_DENSITY_DEFAULT, ZERO);
                myNavRoute.defaultTrackDensity = Integer.valueOf(defTrackDensity);
                if (trackDensity != myNavRoute.defaultTrackDensity && myNavRoute.mergedRoute_HashMap.size() > 0) {
                    // save RouteMiles @ firstListElem so we can recalculate firstListElem with new track density
                    myNavRoute.routeMilesatFirstListElem = myNavRoute.mergedRoute_HashMap.get(
                            myNavRoute.firstListElem).getRouteMiles();
                    editor.putInt(KEY_TRACK_DENSITY, myNavRoute.defaultTrackDensity);
                    editor.apply();
                    new ChangeTPDensityBackground().execute();
                }
                break;
        }
        // distancePref = 0 for Route distance display; 1 for direct distance display
        prefChanged = true;
    }

    @Override
    protected void onPause() {
        if (debugAppState) {
            Log.i(logtag, "onPause()");
        }
        if (pm != null && pm.isScreenOn()) {
            // shouldn't stop location, sensor or autoconnect watchdogs if screen off and recording data
            // timerTask can persist; make sure nothing autoConnects by clearing the device list
            stopSensorWatchdog();
            stopSpoofingLocations();
        }
        saveState();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myLocationsReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (debugAppState) { Log.i(logtag, "onStop()"); }
        super.onStop();
        if (dataBaseAdapter != null) {
            dataBaseAdapter.close();
        }
        dismissAllSnackbars();
        stopAutoConnectBLE();
        if (mLocationServiceBound) {
            // Unbind from the Location Service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mLocationServiceConnection);
            mLocationServiceBound = false;
        }
    }

    private void dismissAllSnackbars() {
        if (mBluetoothSettingsSnackBar != null) {
            mResolvingBluetoothMode = false;
            mBluetoothSettingsSnackBar.dismiss();
        }
        if (mLocationSettingsSnackBar != null) {
            mLocationSettingsSnackBar.dismiss();
        }
        if (mRequestPowerPairingSnackBar != null) {
            mResolvingPowerPairing = false;
            mRequestPowerPairingSnackBar.dismiss();
        }
        if (mWarnPowerInactiveSnackBar != null) {
            mWarnPowerInactiveSnackBar.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        if (debugAppState) Log.i(logtag, "onDestroy()");
        if (trainerMode) { exitTrainerMode(); }
        mWakeLock.release();
        stopSensorWatchdog();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(mGattDiscoveryReceiver);
        unregisterReceiver(mGattHRMMeasurementReceiver);
        unregisterReceiver(mGattSpeedMeasurementReceiver);
        unregisterReceiver(mGattCadenceMeasurementReceiver);
        unregisterReceiver(mGattPowerMeasurementReceiver);
        unregisterReceiver(mGattOppositePowerMeasurementReceiver);
        mLocationService.removeLocationUpdates();
        myBikeStat.tcxLog.closeTCXLogFile();
        new ThreadPerTaskExecutor().execute(closeFitFileRunnable);
        stopSpoofingLocations();
        unbindService(mHRMServiceConnection);
        mBLEHRMService = null;
        unbindService(mSpeedServiceConnection);
        mBLESpeedService = null;
        unbindService(mCadenceServiceConnection);
        mBLECadenceService = null;
        unbindService(mPowerServiceConnection);
        mBLEPowerService = null;
        unbindService(mOppositePowerServiceConnection);
        mBLEOppositePowerService = null;
        unbindService(mServiceConnection);
        mBLEDiscoveryService = null;
        mLocationHelper.stopLocationUpdates();
        super.onDestroy();
    }

    private void startSensors() {
        // called from onResume()
        if (debugAppState) { Log.i(logtag, "startSensors()"); }
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        useBLEData = settings.getBoolean(USE_BLE, true);
        if (!Utilities.hasBLE(context)) {
            useBLEData = false;
        }
        myBikeStat.setPaused(true);
        myBikeStat.mergeSpeedSensors(trainerMode, Utilities.isColorSchemeHiViz(context));
        // Since startSensors is called from onResume, we may already have these
        // watchdogs running. Stop them first before starting.
        stopAutoConnectBLE();
        stopSensorWatchdog();
        startSensorWatchdog();
        if (useBLEData) {
            initializeBLE();
            startAutoConnectBLE();
        }
        if (trainerMode) {
            prefChanged = true;
            refreshScreen();
            stopSpoofingLocations();
            startSpoofingLocations();
        }
    }

    private void requestBluetoothModeOn() {
        mResolvingBluetoothMode = true;
        mBluetoothSettingsSnackBar = Snackbar.make(
                myCoordinatorLayout,
                getString(R.string.reqBluetoothOn_message),
                Snackbar.LENGTH_INDEFINITE);
        mBluetoothSettingsSnackBar.setAction(R.string.allow, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
                // fire an intent to display a dialog asking the user to grant permission to enable it.
                if (mBluetoothAdapter != null) {
                    if (!mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.enable();
                    }
                }
            }
        }).show();
    }

    private void dealWithDialog(int message, int title, final int dialogType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        // Add the buttons
        builder.setPositiveButton(R.string.ok,
                new OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        handleDialogAction(dialogType);
                    }
                });
        builder.setNegativeButton(R.string.cancel,
                new OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        // Set other dialog properties
        builder.setMessage(message).setTitle(title).show();
    }

    private void handleDialogAction(final int dialogType) {// OK button pressed in Alert Dialog
        if (debugOldTCXFile) {
            Log.i(logtag, "handle dialog dialogType: " + dialogType);
        }
        switch (dialogType) {
            case 100:
                break;
            case 200: // menu reset type
                if (debugOldTCXFile) {
                    Log.i(logtag, "menu reset, closing fit file");
                }
                // new CloseFitFileBackground().execute("");
                new ThreadPerTaskExecutor().execute(closeFitFileRunnable);
                resetData();
                // clear the NavRoute
                myNavRoute.mergedRoute.clear();
                myNavRoute.mergedRoute_HashMap.clear();
                chosenGPXFile = "";
                createTitle("");
                // clear the turn list
                initHashMap();
                firstSpoofLocation = true;
                gpsFirstLocation = true;
                refreshScreen();
                // open a new tcx log file when we get the next location
                // the previous Log File will be closed when the new one is opened
                forceNewTCX_FIT = true;
                // reset the NavRoute filename
                break;
            case 300: // GPS enable type
                enableLocationSettings();
                break;
            default:
                break;
        }
    }

    /**
     * clear all the trip data
     */
    private void resetData() {
        if (debugAppState) Log.i(logtag, "resetData()");
        myBikeStat.reset();
        myNavRoute.setBonusMiles(0);
        SharedPreferences settings1 = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor1 = settings1.edit();
        editor1.putString(BONUS_MILES, Double.toString(myNavRoute.getBonusMiles()));
        editor1.putString(TRIP_TIME, Double.toString(myBikeStat.getGPSRideTime()));
        editor1.putString(TRIP_DISTANCE, Double.toString(myBikeStat.getGPSTripDistance()));
        editor1.putString(WHEEL_TRIP_TIME, Double.toString(myBikeStat.getWheelRideTime()));
        editor1.putString(WHEEL_TRIP_DISTANCE, Double.toString(myBikeStat.getWheelTripDistance()));
        editor1.putString(POWER_WHEEL_TRIP_TIME, Double.toString(myBikeStat.getPowerWheelRideTime()));
        editor1.putString(POWER_WHEEL_TRIP_DISTANCE, Double.toString(myBikeStat.getPowerWheelTripDistance()));
        editor1.putString(MAX_SPEED, Double.toString(myBikeStat.getMaxSpeed()));
        // save the pedCount and wheelCount values here, too
        editor1.putInt(NUM_WHEEL_CNTS, (int) mBLEDeviceManager.wheelCnts.calTotalCount);
        editor1.putInt(NUM_POWER_WHEEL_CNTS, (int) mBLEDeviceManager.powerWheelCnts.calTotalCount);
        editor1.putBoolean(OPPOSITE_PED_CNTS_INIT, mBLEDeviceManager.oppositeCrankCadenceCnts.initialized);
        editor1.putBoolean(PED_CNTS_INIT, mBLEDeviceManager.crankCadenceCnts.initialized);
        editor1.putString(START_DIST, Double.toString(mBLEDeviceManager.wheelCnts.calGPSStartDist));
        editor1.putBoolean(KEY_TRAINER_MODE, trainerMode);
        editor1.putBoolean(KEY_FORCE_NEW_TCX, true);
        editor1.apply();
        mBLEDeviceManager.restartHR(0, 0);
        mBLEDeviceManager.restartCadence();
        mBLEDeviceManager.restartPower();
        mBLEDeviceManager.restartWheelCal(myBikeStat.getWheelTripDistance());
        mBLEDeviceManager.restartPowerWheelCal(myBikeStat.getWheelTripDistance());
        myBikeStat.hasCalSpeedSensor = false;
        myBikeStat.hasCalPowerSpeedSensor = false;
        prefChanged = true;
    }

    private void enableLocationSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(settingsIntent);
    }

    private void restoreSharedPrefs() {
        if (debugAppState) Log.i(logtag, "restoreSharedPrefs()");
        // called from autoResumeRoute() when tcx file is not old and menu:Restore
        myPlace = Utilities.getLocFromSharedPrefs(context);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String s = settings.getString(TRIP_TIME, Double.toString(0.1));
        myBikeStat.setGPSTripTime(Double.valueOf(s));
        s = settings.getString(TRIP_DISTANCE, DOUBLE_ZERO);
        myBikeStat.setGPSTripDistance(Double.valueOf(s));
        s = settings.getString(WHEEL_TRIP_TIME, Double.toString(0.1));
        myBikeStat.setWheelRideTime(Double.valueOf(s));
        s = settings.getString(POWER_WHEEL_TRIP_TIME, Double.toString(0.1));
        myBikeStat.setPowerWheelRideTime(Double.valueOf(s));
        s = settings.getString(WHEEL_TRIP_DISTANCE, DOUBLE_ZERO);
        myBikeStat.setWheelTripDistance(Double.valueOf(s));
        s = settings.getString(POWER_WHEEL_TRIP_DISTANCE, DOUBLE_ZERO);
        myBikeStat.setPowerWheelTripDistance(Double.valueOf(s));
        s = settings.getString(PREV_WHEEL_TRIP_DISTANCE, DOUBLE_ZERO);
        myBikeStat.setPrevWheelTripDistance(Double.valueOf(s));
        s = settings.getString(SPOOF_WHEEL_TRIP_DISTANCE, DOUBLE_ZERO);
        myBikeStat.setSpoofWheelTripDistance(Double.valueOf(s));
        s = settings.getString(PREV_SPOOF_WHEEL_TRIP_DISTANCE, DOUBLE_ZERO);
        myBikeStat.setPrevSpoofWheelTripDistance(Double.valueOf(s));
        myNavRoute.currWP = settings.getInt(CURR_WP, WILDCARD);
        myNavRoute.firstListElem = settings.getInt(FIRST_LIST_ELEM, WILDCARD);
        s = settings.getString(BONUS_MILES, DOUBLE_ZERO);
        myNavRoute.setBonusMiles(Double.valueOf(s));
        chosenGPXFile = settings.getString(KEY_CHOSEN_GPXFILE, "");
        myBikeStat.tcxLog.outFileName = settings.getString(TCX_LOG_FILE_NAME, "");
        myBikeStat.tcxLog.outFileFooterLength = settings.getInt(TCX_LOG_FILE_FOOTER_LENGTH, 1);
        forceNewTCX_FIT = settings.getBoolean(KEY_FORCE_NEW_TCX, false);
        // all the BLE device measurements
        // cadence data
        mBLEDeviceManager.oppositeCrankCadenceCnts.prevCount = settings.getInt(OPPOSITE_PREV_PED_CNTS, 0);
        mBLEDeviceManager.oppositeCrankCadenceCnts.initialized = settings.getBoolean(OPPOSITE_PED_CNTS_INIT, false);
        mBLEDeviceManager.crankCadenceCnts.prevCount = settings.getInt(PREV_PED_CNTS, 0);
        mBLEDeviceManager.crankCadenceCnts.initialized = settings.getBoolean(PED_CNTS_INIT, false);
        mBLEDeviceManager.setNumPedalCad(settings.getInt(NUM_PEDAL_CAD, 0));
        mBLEDeviceManager.setTotalPedalCad(settings.getInt(TOTAL_PEDAL_CAD, 0));
        myBikeStat.setAvgCadence(settings.getInt(AVG_CADENCE, 0));
        myBikeStat.setMaxCadence(settings.getInt(MAX_CADENCE, 0));
        // HR data
        mBLEDeviceManager.setTotalHRCounts(settings.getInt(TOTAL_HR_COUNTS, 0));
        mBLEDeviceManager.setNumHREvents(settings.getInt(NUM_HR_EVENTS, 0));
        myBikeStat.setAvgHeartRate(settings.getInt(AVG_HR, 0));
        myBikeStat.setMaxHeartRate(settings.getInt(MAX_HR, 0));
        // wheel data
        mBLEDeviceManager.wheelCnts.calTotalCount = settings.getInt(NUM_WHEEL_CNTS, 0);
        mBLEDeviceManager.wheelCnts.cumulativeRevsAtCalStart = settings.getLong(WHEEL_CUMREV_AT_START, 0);
        mBLEDeviceManager.wheelCnts.cumulativeRevolutions = settings.getLong(WHEEL_CUMREV, 0);
        mBLEDeviceManager.wheelCnts.isCalibrated = settings.getBoolean(WHEEL_IS_CAL, false);
        mBLEDeviceManager.wheelCnts.prevCount = settings.getLong(WHEEL_PREV_COUNT, 0);
        mBLEDeviceManager.wheelCnts.wheelCircumference = Double.valueOf(settings.getString(WHEEL_CIRCUM, DOUBLE_ZERO));
        mBLEDeviceManager.wheelCnts.calGPSStartDist = Double.valueOf(settings.getString(START_DIST, DOUBLE_ZERO));
        myBikeStat.setMaxSpeed(Double.valueOf(settings.getString(MAX_SPEED, DOUBLE_ZERO)));
        // power data
        mBLEDeviceManager.setCumEnergy(settings.getInt(CUM_ENERGY, 0));
        mBLEDeviceManager.setCumPowerTime(Double.valueOf(settings.getString(CUM_POWER_TIME, DOUBLE_ZERO)));
        myBikeStat.setAvgPower(settings.getInt(AVG_POWER, 0));
        myBikeStat.setMaxPower(settings.getInt(MAX_POWER, 0));
        mBLEDeviceManager.powerWheelCnts.calTotalCount = settings.getInt(NUM_POWER_WHEEL_CNTS, 0);
        mBLEDeviceManager.powerWheelCnts.cumulativeRevsAtCalStart = settings.getLong(POWER_WHEEL_CUMREV_AT_START, 0);
        mBLEDeviceManager.powerWheelCnts.cumulativeRevolutions = settings.getLong(POWER_WHEEL_CUMREV, 0);
        mBLEDeviceManager.powerWheelCnts.prevCount = settings.getLong(POWER_WHEEL_PREV_COUNT, 0);
        mBLEDeviceManager.powerWheelCnts.isCalibrated = settings.getBoolean(POWER_WHEEL_IS_CAL, false);
        mBLEDeviceManager.powerWheelCnts.wheelCircumference = Double.valueOf(settings.getString(POWER_WHEEL_CIRCUM, DOUBLE_ZERO));
        mBLEDeviceManager.powerWheelCnts.calGPSStartDist = Double.valueOf(settings.getString(POWER_START_DIST, DOUBLE_ZERO));
        // calculated crank cadence data from power meter
        mBLEDeviceManager.setTotalCalcCrankCad(settings.getInt(TOTAL_CALC_CAD, 0));
        mBLEDeviceManager.setNumCalcCrankCad(settings.getInt(NUM_CALC_CAD, 0));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_layout, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_load:
                if (trainerMode) {
                    viewToast(getString(R.string.can_t_navigate_in_trainer_mode_),
                            40, BLE_TOAST_GRAVITY, toastAnchor, textColorWhite);
                } else {
                    Intent loadFileIntent = new Intent(this, ShowFileList.class);
                    //tell the ShowFileList chooser to display route files
                    loadFileIntent.putExtra(CHOOSER_TYPE, ROUTE_FILE_TYPE);
                    loadFileIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivityForResult(loadFileIntent, RC_SHOW_FILE_LIST);
                }
                break;
            case R.id.menu_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                // use this to tell refreshScreen to re-write the titles
                prefChanged = true;
                break;
            case R.id.menu_reset:
                int dialogType = 200;
                dealWithDialog(R.string.reset_message, R.string.reset_title, dialogType);
                break;
            case R.id.menu_reset_strava:
                // call stravashare with empty filename to just authorize, test for empty filename before doing upload
                Intent stravaUploadIntent = new Intent(MainActivity.this, StravaShareCBBLE.class);
                stravaUploadIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                stravaUploadIntent.putExtra(UPLOAD_FILENAME, "");
                startActivityForResult(stravaUploadIntent, UPLOAD_FILE_SEND_REQUEST_CODE);
                break;
            case R.id.action_share:
                // this intent displays a list of files for the user to select
                Intent shareFileIntent = new Intent(MainActivity.this, ShowFileList.class);
                // change "type" parameter to display .tcx or .fit files in ShowFileList()
                shareFileIntent.putExtra(CHOOSER_TYPE, ACTIVITY_FILE_TYPE);
                startActivityForResult(shareFileIntent, RC_SHOW_FILE_LIST);
                break;
            case R.id.menu_about:
                Intent i11 = new Intent(this, AboutScroller.class);
                startActivity(i11);
                break;
            case R.id.menu_ble:
                Intent ble_settings = new Intent(this, BLESettings.class);
                ble_settings.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivityForResult(ble_settings, RC_BLE_SETTINGS);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }// onOptionsItemSelected()

    private void refreshScreen() {
        // from onCreate(), autoResumeRoute(), save/reset, changing TrackDensity, on each new Location
        // when exiting TrainerMode, inActivityResult when we've chosen a route file, if Location is not current, from SensorWatchdog
        boolean screenOn = pm != null && ((apiOkay && pm.isInteractive()) || pm.isScreenOn());
           if (screenOn) {
                if (prefChanged) {
                    // if units changed, refresh units in titles
                    mLayout.invalidate();
                    View mGPSBLELayout = findViewById(R.id.gps_bt_status_layout);
                    if (debugLocation || debugBLEService || debugLEScan || Utilities.showGPSStatus(context)) {
                        mGPSBLELayout.setVisibility(View.VISIBLE);
                    } else {
                        mGPSBLELayout.setVisibility(View.GONE);
                    }
                    mLayout.setBackgroundColor(prefBackgroundColor);
                    refreshTitles();
                    // if display pref changed, update CAD, HR, Power display
                    refreshCadHRPower();
                    if (trainerMode) {
                        trainerModeCommentScroller.setVisibility(View.VISIBLE);
                        exitTMBtn.setVisibility(View.VISIBLE);
                        turnByturnList.setVisibility(View.GONE);
                    } else {
                        trainerModeCommentScroller.setVisibility(View.GONE);
                        exitTMBtn.setVisibility(View.GONE);
                        turnByturnList.setVisibility(View.VISIBLE);
                    }
                    prefChanged = false;
                }
                refreshMergedRouteHashMap();
                refreshBikeStatRow();
            }
    }// refreshScreen()


    /*
     * show or hide the Cad-HR-power display and the percent power display
     */
    private void refreshCadHRPower() {
        // only do this when returning from settings menu or in startSensors()
        // also change the HR Title from BPM to %max
        int visibility = View.GONE;
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean showBLEData = settings.getBoolean(SHOW_BLE, true);
        if (showBLEData && hasBLE(context)) {
            visibility = View.VISIBLE;
            // update the values
            updateCadHrPowerLabels();
            updateCadHrPowerTitles();
        }
        cadHRPowerLayout.setVisibility(visibility);
        cadHRPowerLayout.invalidate();
    }

    private void updateCadHrPowerTitles() {
        //set titles depending on screen width
        TextView cadTitle = findViewById(R.id.cad_title);
        TextView hrTitle = findViewById(R.id.hr_title);
        TextView powTitle = findViewById(R.id.power_title);

        if (isScreenWidthSmall(context)) {
            cadTitle.setText(R.string.short_cadence_title);
            hrTitle.setText(R.string.short_heart_rate_bpm_title);
            powTitle.setText(R.string.short_power_watts_title);
        } else {
            cadTitle.setText(R.string.cadence_title);
            hrTitle.setText(R.string.heart_rate_bpm_title);
            powTitle.setText(R.string.power_watts_title);
        }
    }

    /**
     * refresh the cadence, heart-rate and power values
     */
    private void updateCadHrPowerLabels() {
        refreshCadence();
        refreshHR();
        refreshPower();
    }

    private void refreshPower() {
        powerCell.post(new Runnable() {
            @Override
            public void run() {
                String powerText = DASHES;
                if ((powerDeviceData != null) && (powerDeviceData.status == TRACKING)) {
                    powerText = Integer.toString((int) ((myBikeStat.getPower()
                            + myBikeStat.getPrevPower()) / 2.));
                }
                int backgroundColor = prefBackgroundColor;
                if (powerDeviceData != null) {
                    if (powerDeviceData.status == SEARCHING) {
                        backgroundColor = ContextCompat.getColor(context, R.color.bkgnd_green);
                    } else if (powerDeviceData.status == BLEDeviceStatus.DEAD) {
                        backgroundColor = ContextCompat.getColor(context, R.color.bkgnd_red);
                    }
                }
                powerLabel.setText(powerText);
                powerCell.setBackgroundColor(backgroundColor);
                powerLabel.setTextColor(prefTextColor);
            }
        });// post(Runnable)
    }

    private void refreshHR() {
        hrCell.post(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                String hrText = DASHES;
                if (myBikeStat.hasHR && mBLEDeviceManager.hrData.isDataCurrent) {
                    hrText = Integer.toString(myBikeStat.getHR());
                }
                // change hrCell bkgnd color to indicate searching status
                int backgroundColor = prefBackgroundColor;
                if (hrmDeviceData != null) {
                    if (hrmDeviceData.status == SEARCHING) {
                        backgroundColor = ContextCompat.getColor(context, R.color.bkgnd_green);
                    } else if (hrmDeviceData.status == BLEDeviceStatus.DEAD) {
                        backgroundColor = ContextCompat.getColor(context, R.color.bkgnd_red);
                    }
                }
                heartLabel.setText(hrText);
                hrCell.setBackgroundColor(backgroundColor);
                heartLabel.setTextColor(prefTextColor);
            }
        });// post(Runnable)
    }

    private void refreshCadence() {
        cadCell.post(new Runnable() {
            @Override
            public void run() {
                String cadenceText = DASHES;
                if ((myBikeStat.hasCadence && mBLEDeviceManager.pedalCadenceCnts.isDataCurrent)
                        || (myBikeStat.hasPowerCadence && mBLEDeviceManager.crankCadenceCnts.isDataCurrent)) {
                    cadenceText = String.format(FORMAT_3D, myBikeStat.getCadence());
                }
                // change cadCell bkgnd color to indicate searching status
                int backgroundColor = prefBackgroundColor;
                if (cadDeviceData != null) {
                    if (cadDeviceData.status == SEARCHING) {
                        backgroundColor = ContextCompat.getColor(context, R.color.bkgnd_green);
                    } else if (cadDeviceData.status == BLEDeviceStatus.DEAD) {
                        backgroundColor = ContextCompat.getColor(context, R.color.bkgnd_red);
                    }
                }
                cadenceLabel.setText(cadenceText);
                cadCell.setBackgroundColor(backgroundColor);
                cadenceLabel.setTextColor(prefTextColor);
            }
        });// post(Runnable)
    }

    private void refreshMergedRouteHashMap() {
        turnByturnList.post(new Runnable() {
            @Override
            public void run() {
                double distMultiplier = mile_per_meter;
                String unit;
                int distanceUnit = Utilities.getDistanceUnit(context);
                int distanceType = Utilities.getDistanceType(context);
                int j = turnByturnList.getFirstVisiblePosition();
                while ((j < routeHashMap.size())
                        && (j < myNavRoute.mergedRoute_HashMap.size())
                        && (j < turnByturnList.getLastVisiblePosition() + 1)) {
                    HashMap<String, String> hmItem = new HashMap<>();
                    float result[];
                    result = distFromMyPlace2WPMR(j, distanceType);
                    // results returns in miles; convert to meters, if needed
                    double distance = result[0] * distMultiplier;
                    int turnDirIcon = myNavRoute.mergedRoute_HashMap.get(j).turnIconIndex;
                    String streetName = myNavRoute.mergedRoute_HashMap.get(j).getStreetName();
                    String distanceString;
                    if (isGPSLocationCurrent() || isFusedLocationCurrent()) {
                        distanceString = String.format(FORMAT_1F, distance);
                    } else {
                        distanceString = QUESTION;
                    }
                    if (distanceUnit == DISTANCE_TYPE_MILE) {
                        unit = MILE;
                    } else {
                        distMultiplier = km_per_meter;
                        unit = KM;
                    }
                    if (distance < 0.1) {// switch to display in feet / m
                        int dist;
                        // increment in multiples of 20', a likely resolution limit
                        if (distanceUnit == DISTANCE_TYPE_MILE) {
                            dist = (int) Math.floor(distance * 264) * 20;
                            unit = FOOT;
                        } else {
                            dist = (int) Math.floor(distance * 100) * 10;
                            unit = METER;
                        }
                        if (isGPSLocationCurrent() || isFusedLocationCurrent()) {
                            distanceString = String.format(FORMAT_3D, dist);
                        } else {
                            distanceString = QUESTION;
                        }
                    }// if dist<0.1
                    // make sure the bearing is between 0 & 360 degrees
                    double bearing = (result[1] + _360) % _360;
                    // we only get an accurate DOT when we're moving at a speed greater
                    // than "accurateGPSSpeed"
                    // this will use the last accurate DOT from GPS location
                    // make sure the relative bearing is between 0 & 360 degrees
                    double relBearing = (bearing - myNavRoute.accurateDOT + _360) % _360;
                    // convert between relative bearing and the bearing icon (arrow)
                    // want north arrow for rel bearing between
                    // (360 - 11.5) to (360 + 11.5) or 348 to 11.5; north arrow icon
                    // is #0, nne arrow is #1, etc
                    int bearingIcon = (int) Math
                            .floor((((relBearing + DEG_PER_BEARING_ICON / 2) % _360) / DEG_PER_BEARING_ICON));

                    int dimLevel = prefTextColor;
                    if (Utilities.isColorSchemeHiViz(context)) {
                        // don't put hiViz "X" icon; other hiViz icons have iconLevel + 18 for high-viz color
                        if (turnDirIcon != 99) {
                            turnDirIcon = myNavRoute.mergedRoute_HashMap.get(j).turnIconIndex + 18;
                        }
                    }
                    boolean dimmed = (myNavRoute.mergedRoute_HashMap.get(j).isBeenThere());
                    if (dimmed) {
                        dimLevel = ContextCompat.getColor(context, R.color.textdim);
                        // if turn icons are dimmed the icon level is at + 9
                        turnDirIcon = myNavRoute.mergedRoute_HashMap.get(j).turnIconIndex + 9;
                        // dimmed bearing icon (arrow) levels are + 16
                        bearingIcon += 16;
                    }
                    if (myNavRoute.isProximate() & (j == myNavRoute.currWP)) {
                        dimLevel = ContextCompat.getColor(context, R.color.gpsgreen);
                    }
                    // Change distance, distance unit, bearing icon level
                    // Don't need to change turn, street name unless firstListElem changed
                    hmItem.put(KEY_TURN, Integer.toString(turnDirIcon));
                    hmItem.put(KEY_STREET, streetName);
                    hmItem.put(KEY_DISTANCE, distanceString);
                    hmItem.put(KEY_UNIT, unit);
                    hmItem.put(KEY_BEARING, Integer.toString(bearingIcon));
                    hmItem.put(KEY_DIM, Integer.toString(dimLevel));
                    routeHashMap.set(j, hmItem);
                    j++;
                }// while visible item
                // after all edits are done
                turnByturnAdapter.notifyDataSetChanged();
                // Decide which element should be at the top of the list. If we're close to a WayPoint,
                // but it's not at the top of the list, smooth scroll to top and set 1st element to the
                // current WayPoint. This could be because we've manually scrolled the list away from currWP
                if (myNavRoute.isProximate()) {
                    // convert myNavRoute.currWP to a hash map index
                    myNavRoute.firstListElem = myNavRoute.currWP;
                }
                // After we've moved away from the Proximate Way Point need to bump the scroll list up one row
                turnByturnList.setSelectionFromTop(myNavRoute.firstListElem, 0);
            }
        });
    }

    private void refreshTitles() {
        // only refresh titles if unit preference changed, or wheel is now calibrated
        String calString;
        if (mBLEDeviceManager.wheelCnts.isCalibrated || mBLEDeviceManager.powerWheelCnts.isCalibrated) {
            calString = _CAL;
        } else {
            calString = "";
        }
        String tripDistString, avgSpeedString, maxSpeedString, gpsSpeedString;

        if (Utilities.getDistanceUnit(context) == DISTANCE_TYPE_MILE) {
            tripDistString = getResources().getString(R.string.trip_dist_mi);
            avgSpeedString = getResources().getString(R.string.avg_speed_mph);
            maxSpeedString = getResources().getString(R.string.max_speed_mph);
            gpsSpeedString = getResources().getString(R.string.curr_gps_speed_mph, calString);
        } else {
            tripDistString = getResources().getString(R.string.trip_dist_km);
            avgSpeedString = getResources().getString(R.string.avg_speed_kph);
            maxSpeedString = getResources().getString(R.string.max_speed_kph);
            gpsSpeedString = getResources().getString(R.string.curr_gps_speed_kph, calString);
        }
        tripDistTitle.setText(tripDistString);
        avgSpeedTitle.setText(avgSpeedString);
        maxSpeedTitle.setText(maxSpeedString);

        gpsSpeedTitle.setText(gpsSpeedString);
        tripTimeTitle.setText(getResources().getString(R.string.trip_time));
    }

    private void refreshTimeDistance() {
        // called from RefreshBikeStatRow and handleSpeedData()
        tripTimeLabel.post(new Runnable() {
            @Override
            public void run() {
                double value;
                double distMultiplier = mile_per_meter;
                int distanceUnit = Utilities.getDistanceUnit(context);

                if (distanceUnit == DISTANCE_TYPE_METRIC) {
                    distMultiplier = km_per_meter;
                }
                // Trip Time
                String timeText;
                // use calibrated speed sensor for trip time if available.
                if (trainerMode) {
                    if (myBikeStat.hasSpeedSensor) {
                        timeText = myBikeStat.getTripTimeStr(myBikeStat.getWheelRideTime());
                    } else if (myBikeStat.hasPowerSpeedSensor) {
                        timeText = myBikeStat.getTripTimeStr(myBikeStat.getPowerWheelRideTime());
                    } else {
                        timeText = myBikeStat.getTripTimeStr(0);
                    }
                } else {
                    if (myBikeStat.hasCalSpeedSensor) {
                        timeText = myBikeStat.getTripTimeStr(myBikeStat.getWheelRideTime());
                        // or use calibrated Power Wheel sensor
                    } else if (myBikeStat.hasCalPowerSpeedSensor) {
                        timeText = myBikeStat.getTripTimeStr(myBikeStat.getPowerWheelRideTime());
                    } else {
                        timeText = myBikeStat.getTripTimeStr(myBikeStat.getGPSRideTime());
                    }
                }
                tripTimeLabel.setText(timeText);
                tripTimeLabel.setTextColor(prefTextColor);
                // Trip Distance
                if (trainerMode) {
                    if (myBikeStat.hasSpeedSensor) {
                        value = myBikeStat.getWheelTripDistance() * distMultiplier;
                    } else if (myBikeStat.hasPowerSpeedSensor) {
                        value = myBikeStat.getPowerWheelTripDistance() * distMultiplier;
                    } else {
                        value = 0.;
                    }
                } else {
                    // use calibrated speed sensor for distance if available
                    if (myBikeStat.hasCalSpeedSensor) {
                        value = myBikeStat.getWheelTripDistance() * distMultiplier;
                        // or use calibrated Power Wheel sensor
                    } else if (myBikeStat.hasCalPowerSpeedSensor) {
                        value = myBikeStat.getPowerWheelTripDistance() * distMultiplier;
                    } else {
                        value = myBikeStat.getGPSTripDistance() * distMultiplier;
                    }
                }
                tripDistLabel.setText(String.format(FORMAT_4_1F, value));
                tripDistLabel.setTextColor(prefTextColor);
            }
        });
    }

    /**
     * called from trainer mode and refreshScreen()
     */
    private void refreshBikeStatRow() {
        refreshTimeDistance();
        tripTimeLabel.post(new Runnable() {
            @Override
            public void run() {
                double avgValue;
                double speedMultiplier = mph_per_mps;
                if (Utilities.getDistanceUnit(context) == DISTANCE_TYPE_METRIC) {
                    speedMultiplier = kph_per_mps;
                }
                // if gps location not current, try speed sensor
                if (!isGPSLocationCurrent() && myBikeStat.hasSpeedSensor
                        && (myBikeStat.getWheelRideTime() > 0)) {
                    avgValue = (myBikeStat.getWheelTripDistance() / myBikeStat
                            .getWheelRideTime()) * speedMultiplier;
                    // or try powertap sensor
                } else if (!isGPSLocationCurrent()
                        && myBikeStat.hasPowerSpeedSensor
                        && (myBikeStat.getPowerWheelRideTime() > 0)) {
                    avgValue = (myBikeStat.getPowerWheelTripDistance() / myBikeStat
                            .getPowerWheelRideTime()) * speedMultiplier;
                } else {
                    avgValue = myBikeStat.getAvgSpeed() * speedMultiplier;
                }
                // average can't be greater than maximum speed but early values can be screwy
                if (avgValue > myBikeStat.getMaxSpeed() * speedMultiplier) {
                    avgValue = myBikeStat.getMaxSpeed() * speedMultiplier;
                }
                avgSpeedLabel.setText(String.format(FORMAT_3_1F, avgValue));
                avgSpeedLabel.setTextColor(prefTextColor);

                // Max Speed
                double maxValue = myBikeStat.getMaxSpeed() * speedMultiplier;
                maxSpeedLabel.setText(String.format(FORMAT_3_1F, maxValue));
                maxSpeedLabel.setTextColor(prefTextColor);
            }// run
        });// runnable
        //refreshBikeStatRow();
    }

    private void refreshSpeed() {
        speedCell.post(new Runnable() {
            @Override
            public void run() {
                myBikeStat.mergeSpeedSensors(trainerMode, Utilities.isColorSchemeHiViz(context));
                String speedString;
                double speedMult = mph_per_mps;
                if (Utilities.getDistanceUnit(context) == DISTANCE_TYPE_METRIC) {
                    speedMult = kph_per_mps;
                }
                double spd = myBikeStat.getSpeed();
                if (spd >= 0) {
                    speedString = String.format(FORMAT_3_1F, spd * speedMult);
                } else {
                    speedString = SPEED_TRIPLE_X;
                }
                gpsSpeedLabel.setText(speedString);
                gpsSpeedLabel.setTextColor(myBikeStat.getSpeedColor());

            }// run
        });// runnable
    }// refreshSpeed()

    /**
     * let the screen dim as per user settings when paused
     */
    private void setScreenDim() {
        if (myBikeStat.isPaused()) { turnByturnList.setKeepScreenOn(false);
        } else { turnByturnList.setKeepScreenOn(true); }
    }

    private void writeAppMessage(String message, int color) {
        boolean shouldShowAppMessageBox;
        // write a message in the App message area
        if (!Utilities.hasGPSPermission(getApplicationContext()) && !trainerMode) {
            if (debugMessageBox) Log.i(logtag, "appMessage: noGPSPerm");
            shouldShowAppMessageBox = true;
            message = getString(R.string.loc_permission_denied);
            color = ContextCompat.getColor(context, R.color.gpsred);
        } else if (!Utilities.hasStoragePermission(getApplicationContext())) {
            shouldShowAppMessageBox = true;
            message = getString(R.string.write_permission_denied);
            color = ContextCompat.getColor(context, R.color.gpsred);
            if (debugMessageBox) Log.i(logtag, "appMessage: noWritePerm");
        } else if (!isGPSLocationEnabled(getApplicationContext()) && !trainerMode) {
            if (debugMessageBox) Log.i(logtag, "appMessage: GPS provider off");
            color = textColorWhite;
            shouldShowAppMessageBox = true;
            message = getResources().getString(R.string.req_gps_on);
        } else if (!isGPSLocationCurrent() && !isFusedLocationCurrent() && !trainerMode) {
            if (debugMessageBox) Log.i(logtag, "appMessage: GPS loc old");
            color = textColorWhite;
            shouldShowAppMessageBox = true;
            if (satAcqMess) {
                satAcqMess = false;
                message = getResources().getString(R.string.acq_satellites1);
            } else {
                satAcqMess = true;
                message = getResources().getString(R.string.acq_satellites2);
            }
        } else {
            if (debugMessageBox) {
                Log.i(logtag, "appMessage: trainer mode");
                Log.i(logtag, "appMessage: no MessageBox");
            }
            shouldShowAppMessageBox = trainerMode;
        }
        showAppMessBox(shouldShowAppMessageBox);
        appMessage.setText(message);
        appMessage.setTextColor(color);
    }

    private void showAppMessBox(boolean b) {
        int visibility = View.GONE;
        if (b) { visibility = View.VISIBLE; }
        appMessage.setVisibility(visibility);
    }

    private float[] distFromMyPlace2WPMR(int index, int distanceType) {

        float[] results;
        if (distanceType == ROUTE_DISTANCE_TYPE) {
            results = calcMRRouteDistance(index);
        } else {
            results = calcMRDirectDistance(index);
        }
        return results;
    }// distFromMyPlace2WP()

    private float[] calcMRDirectDistance(int index) {
        float[] results = {0, 0};
        if (index > myNavRoute.mergedRoute_HashMap.size())
            return results;
        there.setLatitude(myNavRoute.mergedRoute_HashMap.get(index).lat);
        there.setLongitude(myNavRoute.mergedRoute_HashMap.get(index).lon);
        results[0] = myPlace.distanceTo(there);
        results[1] = myPlace.bearingTo(there);
        return results;
    }

    private float[] calcMRRouteDistance(int index) {
        // in meters
        double distance = 0, distCurrWP;
        float[] results = {0, 0};
        if (index > myNavRoute.mergedRoute_HashMap.size())
            return results;
        if (index >= myNavRoute.currWP) {
            // calculate distance from myPlace to currWP
            there.setLatitude(myNavRoute.mergedRoute_HashMap.get(myNavRoute.currWP).lat);
            there.setLongitude(myNavRoute.mergedRoute_HashMap.get(myNavRoute.currWP).lon);
            distCurrWP = myPlace.distanceTo(there);
            if (index == myNavRoute.currWP) {
                distance = distCurrWP;
            } else {
                // distance to any entry in the list is difference in RouteMiles
                // between that entry and the RouteMiles at the currWP, plus the
                // distance from
                // where we are to the currWP (calculated before)
                distance += (myNavRoute.mergedRoute_HashMap.get(index).getRouteMiles()
                        - myNavRoute.mergedRoute_HashMap.get(myNavRoute.currWP).getRouteMiles());
                // If currWP.beenThere is false, add distCurrWP to running total
                // because we're before the currWP
                // If currWP.beenThere is true subtract distCurrWP from running
                // distance total for other WPs because we're after the currWP,
                // just not farEnough away to increment the currWP
                if (myNavRoute.mergedRoute_HashMap.get(myNavRoute.currWP).isBeenThere()) {
                    distance -= distCurrWP;
                } else {
                    distance += distCurrWP;
                }
            }
            results[0] = (float) distance;
            there.setLatitude(myNavRoute.mergedRoute_HashMap.get(index).lat);
            there.setLongitude(myNavRoute.mergedRoute_HashMap.get(index).lon);
            results[1] = myPlace.bearingTo(there);
        }
        return results;
    }

    /**
     * Set up a timer to call spoofLocations every second, simulating a GPS
     * provider The bike speed sensor will have put distance information into
     * myBikeStat.spoofWheelTripDistance() and
     * myBikeStat.prevSpoofWheelTripDistance()
     * <p/>
     * For simulation purposes, each time the Timer fires, put a distance and
     * speed into myBikeStat, as subscribeCalibratedSpeed() would. Delete
     * simulation mode for production version
     */

    private TimerTask spoofLocationsTimerTask;
    private final Handler spoofLocationsHandler = new Handler();
    private final Timer spoofLocationsTimer = new Timer();

    /**
     * a watchdog timer to spoof locations in trainer mode
     */
    private void startSpoofingLocations() {
        spoofLocationsTimerTask = new TimerTask() {
            @Override
            public void run() {
                spoofLocationsHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (trainerMode) {
                            spoofLocations();
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!scrolling && (pm != null) && pm.isScreenOn()) {
                                        refreshBikeStatRow();
                                    }
                                    if (myBikeStat.hasSpeedSensor || myBikeStat.hasPowerSpeedSensor) {
                                        writeAppMessage(
                                                getString(R.string.trainer_mode_recording_track),
                                                ContextCompat.getColor(context, R.color.gpsgreen));
                                    } else {
                                        writeAppMessage(
                                                getString(R.string.trainer_mode_no_speed_sensor),
                                                ContextCompat.getColor(context, R.color.gpsred));
                                    }
                                }// run
                            });// post(Runnable)
                        }// trainerMode
                    }// run
                });// spoofLocationsHandler Runnable
            }
        };// TimerTask()
        final long spoofInterval = 1000;
        spoofLocationsTimer.schedule(spoofLocationsTimerTask, 300, spoofInterval);
    }

    private void stopSpoofingLocations() {
        if (debugAppState) { Log.i(logtag, "stopSpoofingLocations()"); }
        spoofLocationsHandler.removeCallbacksAndMessages(null);
        if (spoofLocationsTimerTask != null) { spoofLocationsTimerTask.cancel(); }
    }

    /**
     * Use bike speed-distance sensor to create an elliptical track of timed
     * locations; called by the spoofLocationsTimer every second
     * spoofer.spoofLocations() will write coordinates in BikeStat
     * writeTrackRecord() will pick these up for the tcx file
     */
    private void spoofLocations() {
        //if (debugAppState) { Log.i(logtag, "spoofLocations()"); }
        // don't display GPS speed
        // location is always current tho'
        myBikeStat.newFusedLocSysTimeStamp = System.currentTimeMillis();
        myBikeStat.newGPSLocSysTimeStamp = System.currentTimeMillis();
        SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, 0);
        // this is the last choice in the velodrome list in Settings prefs
        int veloChoice = sharedPref.getInt(KEY_VELO_CHOICE, 0);
        String[] spoofResult = spoofer.spoofLocations(firstSpoofLocation, myBikeStat, veloChoice);
        forceNewTCX_FIT = sharedPref.getBoolean(KEY_FORCE_NEW_TCX, false);
        if (forceNewTCX_FIT) {
            firstSpoofLocation = true;
            resetData();
        }
        if (firstSpoofLocation) {
            myBikeStat.setFirstLocSysTimeStamp(System.currentTimeMillis());
            createTitle(getString(R.string._spoofing_locations_, spoofResult[0]));
            trainerModeComment.setText(getString(R.string.tm_comment, spoofResult[0], spoofResult[1]));
            firstSpoofLocation = false;
            myBikeStat.setPaused(true);
            openReopenTCX_FIT();
            forceNewTCX_FIT = false;
            sharedPref.edit().putBoolean(KEY_FORCE_NEW_TCX, false).apply();
            // now un-pause
            myBikeStat.setPaused(false);
        }
        writeTrackRecord();
    }

    /**
     * Receiver for broadcasts sent by {@link LocationUpdatesService}.
     */
    private class MyLocationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String statusType = intent.getStringExtra(EXTRA_LOCATION_STATUS_TYPE);
            switch (statusType) {
                case EXTRA_LOCATION_STATUS:
                    Location location = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
                    if (debugLocation) {
                        Log.i(logtag, "onFusedLocationChanged()");
                        Log.i(logtag, "location.getAccuracy() " + location.getAccuracy()
                                + "location.hasSpeed() " + location.hasSpeed()
                                + " location.getTime() " + location.getTime()
                                + " location.getProvider() " + location.getProvider()
                                + " gpsLocationOld " + (!isGPSLocationCurrent()? "yes" : "no"));
                        if (Math.abs(System.currentTimeMillis() - location.getTime()) > TWENTYFOUR_HOURS){
                            Log.wtf(logtag, "time-stamp weird");
                        }
                    }
                    // if location time-stamp is weird, skip the data
                    // also demand "goodEnoughLocationAccuracy" before using the data
                    if (trainerMode
                            || location.getTime() < JAN_1_2000
                            || location.getAccuracy() > goodEnoughLocationAccuracy
                            || Math.abs(System.currentTimeMillis() - location.getTime()) > TWENTYFOUR_HOURS) {
                        return;
                    }
                    myBikeStat.newFusedLocSysTimeStamp = System.currentTimeMillis();
                    // only use fused location if we don't have a current gps location; it's probably the same anyway
                    if (!isGPSLocationCurrent()) {
                        updateGPSStatusDisplay(GpsStatus.GPS_EVENT_STARTED,
                                location.getAccuracy() > goodEnoughLocationAccuracy,
                                context, findViewById(R.id.gps_status_button));
                        updateGPSProviderDisplay(location.getProvider(),
                                true,
                                context, findViewById(R.id.gps_provider_button));
                        dealWithNewLocation(location);
                    }
                    break;
            }

        }
    }

    @SuppressWarnings("Convert2Lambda")
    private class LocationHelper {
        GpsListener gpsListener;
        MyLocationListener mLocationListener;
        LocationManager mLocationManager;
        boolean gpsProviderDisabled = true;

        LocationHelper(Context context) {
            mLocationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            mLocationListener = new MyLocationListener();
            gpsListener = new GpsListener();
        }

        /**
         * Use Location Manager to handle location updates unless CycleBike is in the background.
         * Then the Location Service kicks-in
         */
        void startLocationUpdates() {
            if (debugLocation) { Log.i(logtag, "startLocationUpdates() - getting Providers"); }
            boolean locationAllowed = ActivityCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!locationAllowed) {
                if (debugLocation) {Log.wtf(logtag, "startLocationUpdates() - location not allowed");}
                return;
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mLocationManager.registerGnssStatusCallback(new GnssStatus.Callback() {
                        /**
                         * Called periodically to report GNSS satellite status.
                         *
                         * @param gpsSatelliteStatus the current status of all satellites.
                         */
                        @Override
                        public void onSatelliteStatusChanged(GnssStatus gpsSatelliteStatus) {
                            super.onSatelliteStatusChanged(gpsSatelliteStatus);
                            myBikeStat.gpsSatelliteStatus = gpsSatelliteStatus;
                        }
                    });
                }
                if (mLocationManager != null) {
                    mLocationManager.addGpsStatusListener(gpsListener);
                    mLocationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            Constants.MIN_TIME_BW_UPDATES,
                            Constants.MIN_DISTANCE_CHANGE_FOR_UPDATES, mLocationListener);
                myBikeStat.isGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                myBikeStat.isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                } else {
                    if (debugLocation) { Log.wtf(logtag, "locationManager is null!"); }
                }
            } catch (IllegalStateException ignore) {
            }
            if (debugLocation) {
                Log.i(logtag, "startLocationUpdates()"
                + " isGPSEnabled? " + (myBikeStat.isGPSEnabled ? "yes" : "no")
                + " isNetworkEnabled? " + (myBikeStat.isNetworkEnabled? "yes" : "no"));
            }
        }
        class MyLocationListener implements LocationListener {
            /**
             * Called when the location has changed.
             * <p>
             * <p> There are no restrictions on the use of the supplied Location object.
             *
             * @param location The new location, as a Location object.
             */
            @Override
            public void onLocationChanged(Location location) {
                if (debugLocation) {
                    Log.i(logtag, "GPS location changed");
                    Log.i(logtag, "location.getAccuracy() " + location.getAccuracy()
                            + "location.hasSpeed() " + location.hasSpeed()
                            + " location.getTime() " + location.getTime()
                            + " location.getProvider() " + location.getProvider()
                            + " location.getTime() < JAN_1_2000? " + ((location.getTime() < JAN_1_2000) ? "yes" : "no")
                            + " System.currentTimeMillis() " + System.currentTimeMillis()
                            + " Math.abs(System.currentTimeMillis() - location.getTime()) "
                            + Math.abs(System.currentTimeMillis() - location.getTime()));
                }
                updateGPSStatusDisplay(GpsStatus.GPS_EVENT_STARTED,
                        location.getAccuracy() > goodEnoughLocationAccuracy,
                        context, findViewById(R.id.gps_status_button));
                updateGPSProviderDisplay(location.getProvider(),
                        true,
                        context, findViewById(R.id.gps_provider_button));
                // if location time-stamp is weird, skip the data
                // also demand "goodEnoughLocationAccuracy" before using the data
                if (trainerMode
                        || location.getTime() < JAN_1_2000
                        || location.getAccuracy() > goodEnoughLocationAccuracy
                        || Math.abs(System.currentTimeMillis() - location.getTime()) > TWENTYFOUR_HOURS) {
                    return;
                }
                myBikeStat.newGPSLocSysTimeStamp = System.currentTimeMillis();
                dealWithNewLocation(location);
            }

            /**
             * Called when the provider status changes. This method is called when
             * a provider is unable to fetch a location or if the provider has recently
             * become available after a period of unavailability.
             *
             * @param provider the name of the location provider associated with this
             *                 update.
             * @param status the status
             * @param extras   an optional Bundle which will contain provider specific
             *                 status variables.
             *                 <p>
             *                 <p> A number of common key/value pairs for the extras Bundle are listed
             *                 below. Providers that use any of the keys on this list must
             *                 provide the corresponding value as described below.
             *                 <p>
             *                 <ul>
             *                 <li> satellites - the number of satellites used to derive the fix
             */
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                if (debugLocation) {
                    Log.i(logtag, "onStatusChanged()" + " provider: " + provider + " status: " + status);
                }
                if (provider.equals(LocationManager.GPS_PROVIDER)){
                    gpsProviderDisabled = status!=LocationProvider.AVAILABLE;
                }
                updateGPSProviderDisplay(provider, status == LocationProvider.AVAILABLE,
                        context, findViewById(R.id.gps_provider_button));
            }

            /**
             * Called when the provider is enabled by the user.
             *
             * @param provider the name of the location provider associated with this
             *                 update.
             */
            @Override
            public void onProviderEnabled(String provider) {
                if (provider.equals(LocationManager.GPS_PROVIDER)){
                    gpsProviderDisabled = false;
                }
                updateGPSProviderDisplay(provider, true,
                        context, findViewById(R.id.gps_provider_button));
            }

            /**
             * Called when the provider is disabled by the user. If requestLocationUpdates
             * is called on an already disabled provider, this method is called
             * immediately.
             *
             * @param provider the name of the location provider associated with this
             *                 update.
             */
            @Override
            public void onProviderDisabled(String provider) {
                if (provider.equals(LocationManager.GPS_PROVIDER)){
                    gpsProviderDisabled = true;
                }
                updateGPSProviderDisplay(provider, false,
                        context, findViewById(R.id.gps_provider_button));
            }

        }
        class GpsListener implements GpsStatus.Listener {
            /**
             * event event number for this notification
             */
            GpsStatus mGnssStatus;
            @Override
            public void onGpsStatusChanged(int event) {
                if (debugLocation) {
                    Log.i(logtag, "onGpsStatusChanged()" + event);
                    if (event == GpsStatus.GPS_EVENT_STOPPED){
                        updateGPSStatusDisplay(GpsStatus.GPS_EVENT_STOPPED, true,
                                context, findViewById(R.id.gps_status_button));
                    }  else if (event == GpsStatus.GPS_EVENT_STARTED){
                        updateGPSStatusDisplay(GpsStatus.GPS_EVENT_STARTED, false,
                                context, findViewById(R.id.gps_status_button));
                    }
                }

                int iCountInView = 0;
                int iCountInUse = 0;
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                myBikeStat.satellites = mLocationManager.getGpsStatus(mGnssStatus).getSatellites();
                if (myBikeStat.satellites != null) {
                    for (GpsSatellite gpsSatellite : myBikeStat.satellites) {
                        iCountInView++;
                        if (gpsSatellite.usedInFix()) {
                            iCountInUse++;
                        }
                    }
                }
                if (debugLocation) {
                    Log.i(logtag, "# satellites in view: " + iCountInView + " # satellites in use: " + iCountInUse);
                }
                myBikeStat.setSatellitesInUse(iCountInUse);
            }
        }
        /**
         * Disable locationListener when Main Activity is destroyed
         */
        void stopLocationUpdates() {
            if (mLocationManager != null) {
                mLocationManager.removeUpdates(mLocationListener);
                mLocationManager.removeGpsStatusListener(gpsListener);
            }
        }

    }

    void googlePlayAvailable(Context context) {

        if (!hasWifiInternetConnection(context)) {
            return;
        }
        int googlePlayAvailableResponse = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        if (googlePlayAvailableResponse != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, googlePlayAvailableResponse, 0).show();
        }
    }
    private void dealWithNewLocation(Location location) {
        myPlace = location;
        myNavRoute.setPrevDOT(myNavRoute.getDOT());
        myNavRoute.setDOT(location.getBearing());
        // DOT bearing from gps locations is not too accurate if traveling slowly
        // To display relative bearing to waypoints in refreshHashMap use the last accurate DOT
        if (location.getSpeed() > accurateGPSSpeed && location.hasSpeed()) {
            myNavRoute.accurateDOT = location.getBearing();
        }
        myNavRoute.setDeltaDOT(Math.abs(myNavRoute.getDOT() - myNavRoute.getPrevDOT()));
        myBikeStat.setDeltaDOT((float) myNavRoute.getDeltaDOT());
        // Unless we have a speed sensor, this doesn't account for time ticking while loc not current.
        // Situation where GPS is switched off, or drops-out, or rider went inside for lunch.
        // When new location is received, distance may only be 10', but elapsed time may be an hour.
        // Average speed now includes this elapsed time. When new location received,
        // use the new deltaDistance, but calculate delta time based on current average speed.
        // This only affects the display, data written to file is not changed.
        // this also calculates trip distance, ride time, etc
        new ThreadPerTaskExecutor().execute(dealWithLocationRunnable);
        // If speed and power-speed sensors are not calibrated (!myBikeStat.hasCalSpeedSensor), copy GPS
        // TripTime and TripDistance
        if (!myBikeStat.hasCalSpeedSensor) {
            myBikeStat.setWheelTripDistance(myBikeStat.getGPSTripDistance());
            myBikeStat.setWheelRideTime(myBikeStat.getGPSRideTime());
        }
        if (!myBikeStat.hasCalPowerSpeedSensor) {
            myBikeStat.setPowerWheelTripDistance(myBikeStat.getGPSTripDistance());
            myBikeStat.setPowerWheelRideTime(myBikeStat.getGPSRideTime());
        }
        // firstLocation is used to start the track record; it is true after a reset()
        if (gpsFirstLocation) {
            if (debugAppState) Log.i(logtag, "gpsFirstLocation");
            writeAppMessage("", textColorWhite);
            gpsFirstLocation = false;
            // Re-open or create a new tcx file if it's old.
            openReopenTCX_FIT();
        }// first location
        // write to tcx file
        writeTrackRecord();
        // If location accuracy is poor and have an uncalibrated speed sensor or an
        // uncalibrated power-speed sensor, restart the calibration process
        if ((location.getAccuracy() > goodLocationAccuracy)
                && ((myBikeStat.hasSpeedSensor && !mBLEDeviceManager.wheelCnts.isCalibrated)
                || (myBikeStat.hasPowerSpeedSensor && !mBLEDeviceManager.powerWheelCnts.isCalibrated))) {
            mBLEDeviceManager.restartWheelCal(myBikeStat.getWheelTripDistance());
            mBLEDeviceManager.restartPowerWheelCal(myBikeStat.getWheelTripDistance());
        }
        if (!scrolling && (pm != null) && pm.isScreenOn()) {
            refreshScreen();
            // speedSensors will update speed display, don't need to do it from LocationReceiver
            if (!myBikeStat.hasPowerSpeedSensor && !myBikeStat.hasSpeedSensor) {
                refreshSpeed();
            }
        }
    }

    // Use a background Thread to deal with a new Location, calculating Route waypoint distances, etc
    // This prevents blocking the UI Thread
    Runnable dealWithLocationRunnable = new Runnable() {
        @Override
        public void run() {
            myBikeStat.setLastGoodWP(myPlace, gpsFirstLocation);
            boolean screenOn = pm != null && ((apiOkay && pm.isInteractive()) || pm.isScreenOn());
            if (screenOn) {
                // update the Route Waypoint distances if screen is on
                myNavRoute.refreshRouteWayPoints(myPlace, myBikeStat.getGPSTripDistance());
            }
        }
    };

    /**
     * Test for paused condition so we won't write log entries, allow screen to
     * dim, increment ride time, etc
     * This is called from spoofLocations in TrainerMode, LocationListener when we get a new location
     * and from Sensor Watchdog every three seconds
     */
    private void testZeroPaused() {

        boolean paused = false;
        boolean freeRideModeCondition = !trainerMode && (!isGPSLocationCurrent() && !isFusedLocationCurrent()
                || ((myBikeStat.getGpsSpeed() < speedPausedVal) && (myNavRoute.getDeltaDOT() < dotPausedVal)));
        // when wheel slows down, time between revs is more than three seconds in Sensor Watchdog
        boolean trainerModeCondition1 = trainerMode && myBikeStat.hasSpeedSensor
                && (!mBLEDeviceManager.wheelCnts.isDataCurrent || myBikeStat.getSensorSpeed() < speedPausedVal);
        // when power wheel slows down, time between revs is more than three seconds in Sensor Watchdog
        boolean trainerModeCondition2 = trainerMode && myBikeStat.hasPowerSpeedSensor
                && (!mBLEDeviceManager.powerWheelCnts.isDataCurrent || myBikeStat.getPowerSpeed() < speedPausedVal);
        if (trainerModeCondition1 || trainerModeCondition2 || freeRideModeCondition) {
            paused = true;
            myBikeStat.setPausedClock(SystemClock.elapsedRealtime());
            // update "previous-time" so ride-time clock stops when paused
            mBLEDeviceManager.wheelCnts.prevTime = SystemClock.elapsedRealtime();
            mBLEDeviceManager.powerWheelCnts.prevTime = SystemClock.elapsedRealtime();
        }
        myBikeStat.setPaused(paused);
        myBikeStat.mergeSpeedSensors(trainerMode, Utilities.isColorSchemeHiViz(context));
        if (debugLocation) { Log.i(logtag, PAUSED + (myBikeStat.isPaused() ? TRUE : FALSE)); }
        int event = isGPSLocationCurrent() || isFusedLocationCurrent()?GpsStatus.GPS_EVENT_STARTED:GpsStatus.GPS_EVENT_STOPPED;
        boolean locAccuracyNotOK = myBikeStat.getLocationAccuracy() > goodEnoughLocationAccuracy;
        updateGPSStatusDisplay(event, locAccuracyNotOK, context, findViewById(R.id.gps_status_button));
    }

	private void writeTrackRecord() {
		if (!Utilities.hasStoragePermission(getApplicationContext())) {
			return;
		}
        if (debugOldTCXFile) { Log.i(logtag, "writeTrackRecord()");}
		// If we're rebuilding or closing the fit file from re-opening tcx, or sharing a file,
		// don't write a new tcx record, just return
		if (myBikeStat.fitLog.isFileEncoderBusy()) {
            if (debugOldTCXFile) { Log.w(logtag, "writeTrackRecord(), FileEncoder busy");}
			return;
		} else if (debugOldTCXFile) { Log.w(logtag, "writeTrackRecord(), FileEncoder not busy");}

		if (myBikeStat.isPaused() && Utilities.isAutoPause(context)) {
			// close the current track on pause()
			myNavRoute.trackClosed = true;
		} else {
            new ThreadPerTaskExecutor().execute(writeTrackRecordRunnable);
			// if file not open, try to re-open; getError() will return SD card error if !fileHasPermission
			if ((myBikeStat.tcxLog.getError().equals(""))) {
				// no error, successfully wrote record
				myNavRoute.trackClosed = false;
			} else {
				forceNewTCX_FIT = true;
				openReopenTCX_FIT();
			}
		}
	}

    // Use a background Thread to write the data when we get a new Location.
    // This prevents blocking the UI Thread
    Runnable writeTrackRecordRunnable = new Runnable() {
        @Override
        public void run() {
            if (!myBikeStat.fitLog.isFileEncoderBusy()) {
                writingTrackRecord = true;

                synchronized (myBikeStat.tcxLog){
                    myBikeStat.tcxLog.writeTCXRecord(myBikeStat, myNavRoute);
                }
                synchronized (myBikeStat.fitLog){
                    myBikeStat.fitLog.writeRecordMesg(myBikeStat);
                }
                writingTrackRecord = false;
            }
        }
    };

    /**
	 * Since route files could be changed by user and the name could be the
	 * same, we can't keep route files in private storage for very long. We'll
	 * delete all files in private storage when autoResumeRoute() decides that
	 * .tcx file is old. Loading routes from private storage was only intended
	 * to avoid the long LoadData() process when navigating the app
	 */
	private void deleteAllTmpRouteFiles() {
		// Delete all cached route files if .tcx file is old
		String[] routeFiles = fileList();
		for (String file : routeFiles) {
			deleteFile(file);
		}
	}// deleteAllRouteFiles()

    /**
     * Read the BLE device database and add everything to the bleActiveBikeDeviceList where we'll look when LEScan finds a device.
     * Also test for situation where we're tracking a Device, but it's not in the database. Disconnect it.
     * This may arise if we've done forget() in DeviceEditor
     */
    private Runnable loadDBDeviceListRunnable = new Runnable() {
        @Override
        public void run() {
            mBLEDeviceManager.bleActiveBikeDeviceList.clear();
            if (dataBaseAdapter != null && !dataBaseAdapter.isClosed()) {
                mBLEDeviceManager.bleActiveBikeDeviceList.addAll(dataBaseAdapter.getAllDeviceData());
            }
            if (isDeviceDataFaulty(hrmDeviceData, dataBaseAdapter)) {
                hrmDeviceData.status = BLEDeviceStatus.DEAD;
                mBLEHRMService.disconnect();
                Log.w(logtag, "found tracking HRM device not in DB; disconnecting");
            }
            if (isDeviceDataFaulty(cadDeviceData, dataBaseAdapter)) {
                cadDeviceData.status = BLEDeviceStatus.DEAD;
                mBLECadenceService.disconnect();
                Log.w(logtag, "found tracking Cadence device not in DB; disconnecting");
            }
            if (isDeviceDataFaulty(speedDeviceData, dataBaseAdapter)) {
                speedDeviceData.status = BLEDeviceStatus.DEAD;
                mBLESpeedService.disconnect();
                Log.w(logtag, "found tracking Speed device not in DB; disconnecting");
            }
            if (isDeviceDataFaulty(powerDeviceData, dataBaseAdapter)) {
                powerDeviceData.status = BLEDeviceStatus.DEAD;
                mBLEPowerService.disconnect();
                Log.w(logtag, "found tracking Power device not in DB; disconnecting");
            }
            if (debugLEScan){
                mBLEDeviceManager.logActiveBikeDeviceListData("after loading devices from database in loadBLEConfiguration()");
            }
        }
    };
    /**
     * Whenever we close the fit file, the FileEncoder writes
     * the fit file in a background task. When finished, set a flag that allows
     * us to accept new data.
     */
    @SuppressLint("StaticFieldLeak")
    private class ChangeTPDensityBackground extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            // save RouteMiles @ firstListElem so we can recalculate firstListElem with new track density
            myNavRoute.routeMilesatFirstListElem = myNavRoute.mergedRoute_HashMap
                    .get(myNavRoute.firstListElem).getRouteMiles();
            // save RouteMiles @ currWP so we can recalculate currWP with new track density
            myNavRoute.changeTrkPtDensity(myNavRoute.defaultTrackDensity);

            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            initializeMergedRouteTurnList();// set-up the HashMap for street turns
            // recalculate firstListElem using previous RouteMiles at top of list
            myNavRoute.recalcFirstListElem();
            // recalculate currWP using TripDistance minus bonus miles
            myNavRoute.recalcCurrWP(myBikeStat.getGPSTripDistance() - myNavRoute.getBonusMiles());
            // save the new firstListElem and currWP in shared Prefs
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(CURR_WP, myNavRoute.currWP);
            editor.putInt(FIRST_LIST_ELEM, myNavRoute.firstListElem);
            editor.apply();
            // save the route here in a background task
            new ThreadPerTaskExecutor().execute(saveRouteFileRunnable);
            refreshScreen();
        }
    }

    private Runnable restoreRouteFileRunnable = new Runnable() {
        @Override
        public void run() {
            restoreRouteFile(chosenGPXFile);
        }
    };

    private Runnable saveRouteFileRunnable = new Runnable() {
        @Override
        public void run() {
            saveRouteFile(chosenGPXFile);
        }
    };

	/**
	 * Bypass the SAX parser when restoring a route or changing TrackPoint
	 * density and load the route ArrayList from private file storage. Returns
	 * an error if the route is not in private storage; then we'll have to use
	 * the SAX parser
	 *
	 * @param fileName the file to restore
	 **/
	@SuppressWarnings("unchecked")
	private String restoreRouteFile(String fileName) {
		if (debugOldTCXFile) {
            Log.i(logtag, RESTORE_ROUTE_FILE_GPXFILENAME + fileName);
		}
		FileInputStream fis;
		String error = "";
		// add prefix denoting track point density and removing path characters
		fileName = adjustFileName(fileName);
		try {
			fis = openFileInput(fileName);
			ObjectInputStream ois = new ObjectInputStream(fis);
			myNavRoute.mergedRoute = (ArrayList<GPXRoutePoint>) ois.readObject();
			ois.close();
			fis.close();
		} catch (FileNotFoundException e) {
			error = FILE_NOT_FOUND;
		} catch (Exception e) {
			error = EXCEPTION;
			e.printStackTrace();
		}
		return error;
	}

	/**
	 * save the route ArrayList to private storage so we can bypass the SAX
	 * parser when restoring route or changing TrackPoint density. Saves some
	 * time when using a big Trackpoint file
	 *
	 * @param fileName the route to save
	 **/
	private void saveRouteFile(String fileName) {
		if (debugOldTCXFile) { Log.i(logtag, "saveRouteFile()"); }
		// add prefix denoting track point density and removing path characters
		fileName = adjustFileName(fileName);
		// see if the file already exists
		String[] routeFiles = fileList();
		boolean fileAlreadyExists = false;
		for (String file : routeFiles) {
			if (file.equals(fileName)) {
				fileAlreadyExists = true;
				break;
			}
		}
		// if the file doesn't exist, write it; otherwise don't write it
		if (!fileAlreadyExists) {
			try {
				FileOutputStream fos = openFileOutput(fileName, Context.MODE_PRIVATE);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(myNavRoute.mergedRoute);
				oos.close();
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private String adjustFileName(String fileName) {
		// delete path prefix; private storage names can't have path symbols
		// filename returned from ShowFileList has path characters in it
		if (fileName != null) {
			int start = fileName.lastIndexOf("/") + 1;
			int end = fileName.length();
			if ((end - start) <= 0) {
				fileName = "";
			} else {
				fileName = fileName.substring(start, end);
			}
		}
		// add prefix denoting track point density
		SharedPreferences defaultSettings = PreferenceManager.getDefaultSharedPreferences(context);
		String defTrackDensity = defaultSettings.getString(getResources().getString(R.string.pref_trackpoint_density_key), "0");
		return TP_DENSITY + defTrackDensity + fileName + TMP_CB_ROUTE;
	}

	/**
	 * Whenever we close the fit file, the FileEncoder writes
	 * the fit file in a background task. When finished, set a flag that allows
	 * us to accept new data.
	 */
    private class CloseFitFileBackground extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			myBikeStat.tcxLog.closeTCXLogFile();
			myBikeStat.fitLog.closeFitFile(myBikeStat.tcxLog.outFileName);
			return params[0];
		}

		@Override
		protected void onPostExecute(String sharingFileName) {
            if (debugAppState) {
                Log.i(logtag, "CloseFitFileBackground(), onPostExecute()");
            }
			super.onPostExecute(sharingFileName);
			if (!("").equals(sharingFileName)) {
				uploadFileSend(sharingFileName);
			}
		}
	}

    private Runnable closeFitFileRunnable = new Runnable() {
        @Override
        public void run() {
            if (debugAppState){Log.w(logtag, "closeFitFileRunnable");}
            myBikeStat.tcxLog.closeTCXLogFile();
            myBikeStat.fitLog.closeFitFile(myBikeStat.tcxLog.outFileName);
        }
    };

    private Runnable uploadFileSendRunnable = new Runnable() {
        @Override
        public void run() {
            viewToast("uploadFileSendRunnable", 40, BLE_TOAST_GRAVITY, toastAnchor, textColorWhite);
            Log.w(logtag, "uploadFileSendRunnable");
            uploadFileSend(getSharedPreferences(PREFS_NAME, 0).getString(SHARING_FILENAME, ""));
        }
    };

    private Runnable openNewFitFileBackgroundRunnable = new Runnable() {
        @Override
        public void run() {
            String error = myBikeStat.fitLog.openNewFIT(myBikeStat);
        }
    };

    private Runnable reopenFitFileBackgroundRunnable = new Runnable() {
        @Override
        public void run() {
            String error = myBikeStat.fitLog.reOpenFitFile(myBikeStat.tcxLog.outFileName);
        }
    };

 	/**
	 * Use an asynchronous, background thread to load the file with a progress
	 * bar in case it's a big file
	 */
    private class LoadData extends AsyncTask<Void, String, Void> {
		ProgressDialog progressDialog;

		@Override
		protected void onPreExecute() {
			myNavRoute.setError("");
			// display the progress dialog
            if (!chosenGPXFile.equals("")) {
                progressDialog = ProgressDialog.show(MainActivity.this,
                        LOADING_FILE, LOOKING_FOR_ROUTE_DATA, false);
                progressDialog.setCancelable(true);
                progressDialog.setOnCancelListener(new OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        progressDialog.dismiss();
                        myNavRoute.setError(USER_CANCELED);
                    }
                });
            }
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			if (progressDialog != null){
                progressDialog.setMessage(values[0]);
            }
		}

		@Override
		protected Void doInBackground(Void... voids) {
			// See if we have this file cached; if so, load from cache

			if (!("").equals(restoreRouteFile(chosenGPXFile))) {
				// couldn't find the file in cache, so use the SAX parser via .loadNavRoute
				if (!chosenGPXFile.equals("")) {
					myNavRoute.loadNavRoute(getApplicationContext());
					if ((myNavRoute.handler.handlersGPXRoute.size() == 0)
							&& (myNavRoute.handler.handlersTrackPtRoute.size() == 0)) {
						if (myNavRoute.getError().equals("")) {
							// don't obscure another error
							myNavRoute.setError(NO_ROUTE_DATA_IN_FILE);
						}
					}
				}
				// if there was no SAX error, lat/long error, etc and there is
				// route data, initialize the route
				if (myNavRoute.getError().equals("") && (!chosenGPXFile.equals(""))) {
					if (progressDialog != null){
                        progressDialog.setCancelable(false);
                        publishProgress(INITIALIZING_ROUTE);
                    }
					myNavRoute.prepareRoute(getApplicationContext());
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			// all UI altering tasks have to go in the post-execute method
			if (progressDialog != null) {
                progressDialog.dismiss();
            }
			if (!chosenGPXFile.equals("")) {
				if (!myNavRoute.getError().equals("")) {
					revertChosenFile();
					// either "no route data...", or invalid lat/lon data, or user-canceled
					Toast.makeText(getApplicationContext(),
							myNavRoute.getError(), Toast.LENGTH_LONG).show();
					myNavRoute.setError("");
				} else {
					dealWithGoodData();
				}
			} else {// there was no filename specified
				myNavRoute.mergedRoute.clear();
				initHashMap();
			}
		}

		/**
		 * the file selected in the Chooser isn't valid, return the last good
		 * file to Shared Preferences and NavRoute.chosenFile
		 */
		private void revertChosenFile() {
            if (debugOldTCXFile) {
                Log.i(logtag, "revertChosenFile() - gpxfilename: " + chosenGPXFile
                +  " - prevChosenFile: " + prevChosenFile);
            }
			myNavRoute.mChosenFile = new File(prevChosenFile);
			chosenGPXFile = prevChosenFile;
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(KEY_CHOSEN_GPXFILE, prevChosenFile);
			editor.apply();
            new ThreadPerTaskExecutor().execute(restoreRouteFileRunnable);
			createTitle(prevChosenFile);
		}

		private void dealWithGoodData() {
			if (debugOldTCXFile) { Log.i(logtag, "dealWithGoodData() - gpxfilename: " + chosenGPXFile); }
			myNavRoute.firstListElem = 0;
			myNavRoute.currWP = 0;
			myNavRoute.refreshRouteWayPoints(myPlace, myBikeStat.getGPSTripDistance());
			myNavRoute.setProximate(false);
			createTitle(chosenGPXFile);
			initializeMergedRouteTurnList();// set-up the HashMap for street turns
            // Getting adapter by passing files data ArrayList
            turnByturnAdapter = new TurnByTurnListAdapter(MainActivity.this, routeHashMap);
            turnByturnAdapter.notifyDataSetChanged();
            turnByturnList.setAdapter(turnByturnAdapter);
            turnByturnList.setSelectionFromTop(myNavRoute.firstListElem, 0);
			forceNewTCX_FIT = false;
			if (!resumingRoute) { // loading a new route file
				// if tcx file isn't old, ask to
				// open a new tcx file for the new route and zero-out data
				boolean old = myBikeStat.tcxLog.readTCXFileLastModTime(
						myBikeStat.tcxLog.outFileName, Utilities.getTCXFileAutoReset(getApplicationContext()));
				if (!old) {
					doAskResetPermission();
				}
			} else {// we are resuming route via menu item or autoResuming where tcx file is not old
				// in which case we should open the current tcx file
				// LoadData sets firstListElem = 0; must restore this value
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				myNavRoute.currWP = settings.getInt(CURR_WP, WILDCARD);
				myNavRoute.firstListElem = settings.getInt(FIRST_LIST_ELEM, WILDCARD);
				turnByturnList.setSelectionFromTop(myNavRoute.firstListElem, 0);
			}
			// save the route here in a background task
            new ThreadPerTaskExecutor().execute(saveRouteFileRunnable);
        }// Deal with good data
	}// LoadData class

	private void doAskResetPermission() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String buttonTextID[] = {getString(R.string.ok), getString(R.string.no)};
		builder.setPositiveButton(buttonTextID[0],
				new OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// user pressed the okay button
						resetData();
						gpsFirstLocation = true;
						forceNewTCX_FIT = true;
						if (debugOldTCXFile) {
                            Log.i(logtag, "doAskResetPermission() Okay button");
                            Log.i(logtag, "tcx footerLength: "
									+ myBikeStat.tcxLog.outFileFooterLength);
                            Log.i(logtag, "tcx filename: " + myBikeStat.tcxLog.outFileName);
                            Log.i(logtag, "forceNewTCX: " + (forceNewTCX_FIT ? TRUE : FALSE));
						}
					}
				});
		builder.setNegativeButton(buttonTextID[1],
				new OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// User pressed the no button
						forceNewTCX_FIT = false;
						if (debugOldTCXFile) {
                            Log.i(logtag, "doAskResetPermission() No button");
                            Log.i(logtag, "tcx footerLength: "
									+ myBikeStat.tcxLog.outFileFooterLength);
                            Log.i(logtag, "tcx filename: " + myBikeStat.tcxLog.outFileName);
                            Log.i(logtag, "forceNewTCX: " + (forceNewTCX_FIT ? TRUE : FALSE));
						}
					}
				});
		// Set other dialog properties
		builder.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				forceNewTCX_FIT = false;
			}
		});
		builder.setMessage(getString(R.string.okay_to_reset_trip_data_))
				.setTitle(getString(R.string.reset_data)).show();
	}

	/**
	 * Include the route name in the window title. Called from reset() and load
	 * route options menu items; and from initializeScreen(). When called from
	 * location spoofer, use scrolling title bar
	 */
	@SuppressLint("InflateParams")
	private void createTitle(String chosenFile) {
        android.support.v7.app.ActionBar ab = getSupportActionBar();
		if (trainerMode) {
			if (ab != null) {
				ab.setDisplayShowCustomEnabled(true);
				ab.setDisplayShowTitleEnabled(false);
			}
			LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = inflator != null ? inflator.inflate(R.layout.marquee_action_bar, null) : null;
            TextView actionbar_title_text = null;
            if (v != null) {
                actionbar_title_text = v.findViewById(R.id.actionbar_title);
            }
            if (chosenFile.equals("")) {
                if (actionbar_title_text != null) {
                    actionbar_title_text.setText(logtag);
                }
            } else {
                if (actionbar_title_text != null) {
                    actionbar_title_text.setText(chosenFile);
                }
            }
			// even tho' we've set this in the XML file, have to set it again???
            if (actionbar_title_text != null) {
                actionbar_title_text.setHorizontallyScrolling(true);
                // this is the key to actually having the title scroll hahaha
                actionbar_title_text.setSelected(true);
            }
			// assign the view to the actionbar
			if (ab != null) ab.setCustomView(v);
		} else {
			if (!chosenFile.equals("")) {
				int start = chosenFile.lastIndexOf("/") + 1;
				int end = chosenFile.length();
				if (chosenFile.endsWith(GPX)) {
					end = chosenFile.lastIndexOf(GPX);
				} else if (chosenFile.endsWith(TCX)) {
					end = chosenFile.lastIndexOf(TCX);
				} else if (chosenFile.endsWith(XML)) {
					end = chosenFile.lastIndexOf(XML);
				}
				String title = chosenFile.substring(start, end);
				if (ab != null) {
					ab.setDisplayShowCustomEnabled(false);
					ab.setDisplayShowTitleEnabled(true);
					ab.setTitle(title);
				}
			} else {
				if (ab != null) {
					ab.setDisplayShowCustomEnabled(false);
					ab.setDisplayShowTitleEnabled(true);
					ab.setTitle(APP_NAME);
				}
			}
		}
	}

	/**
	 * When long-clicking a way point in the list, make sure it's close enough
	 * check myPlace.distanceTo(Way point at pos-position in list) < nearEnough
	 * Set message either now navigating from..., or not close enough
	 *
	 * @param pos is the item number in the mergedRouteHashmap
	 * @return true if we are close enough to the waypoint
	 */
	private boolean checkNearEnough(int pos) {
		if (pos >= myNavRoute.mergedRoute_HashMap.size()) {
			return false;
		}
		GPXRoutePoint tempRP;
		tempRP = myNavRoute.mergedRoute_HashMap.get(pos);
		Location loc = new Location(myPlace);
		loc.setLatitude(tempRP.lat);
		loc.setLongitude(tempRP.lon);
		double dist = myPlace.distanceTo(loc);
		String streetString = tempRP.getStreetName();
		String str = getString(R.string.now_navigating_from_) + streetString;
		boolean near = (dist < nearEnough);
		if (near) {
			Toast nearToast = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
			TextView v = nearToast.getView().findViewById(android.R.id.message);
			v.setTextColor(ContextCompat.getColor(context, R.color.gpsgreen));
			v.setTextSize(16);
			nearToast.show();
		} else {
			str = streetString + getString(R.string._is_not_close_enough);
			Toast nearToast = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
			TextView v = nearToast.getView().findViewById(android.R.id.message);
			v.setTextColor(ContextCompat.getColor(context, R.color.gpsred));
			v.setTextSize(16);
			nearToast.show();
		}
		return near;
	}

	void doCalibratePower(String theCalibrationAddress) {
        BLEDeviceData thePowerDevice = powerDeviceData;
        if ((theCalibrationAddress).equals(oppositePowerDeviceData.getAddress())){
            thePowerDevice = oppositePowerDeviceData;
        }
		if (debugAppState) {Log.i(logtag, "doCalibratePower() address " + thePowerDevice.getAddress()
                + " " + thePowerDevice.getDeviceType().name()); }
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String message = getString(R.string.follow_power_meter_instructions);
		CharSequence titleString = getString(R.string.calibrate_power_meter);
		builder.setMessage(message).setTitle(titleString).setCancelable(true);
		builder.setNegativeButton(getString(R.string.cancel),
				new OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				});
		builder.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
			}
		});
        BLEDeviceData finalThePowerDevice = thePowerDevice;
        builder.setPositiveButton(getString(R.string.calibrate),
				new OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// send manual calibration message
						boolean submitted;
                        if (debugAppState) {Log.i(logtag, "requesting manual calibration");}
                        submitted = sendPowerCalibrationCommand(finalThePowerDevice);
						if (!submitted) {
							if (debugAppState) {Log.i(logtag, getString(R.string.calibration_request_could_not_be_made));}
							viewToast(getString(R.string.calibration_request_could_not_be_made),
									40, BLE_TOAST_GRAVITY, toastAnchor, textColorWhite);
						} else {
							if (debugAppState) {Log.i(logtag, getString(R.string.calibration_requested));}
							viewToast(getString(R.string.calibration_requested),
									40, BLE_TOAST_GRAVITY, toastAnchor, textColorWhite);
						}// calibration result receiver will display confirmation message
					}

                    private boolean sendPowerCalibrationCommand(BLEDeviceData thePowerDevice) {
                        if (debugAppState) {Log.wtf(logtag, "Sending calibration command to " + thePowerDevice.getAddress());}
                        boolean submitted;
                        int offset = 0;
                        BluetoothGattCharacteristic pcpCharacteristic = thePowerDevice.getPowerControlPtCharacteristic();
                        submitted = pcpCharacteristic.setValue(CALIBRATE_POWER_OPCODE, FORMAT_UINT8, offset);
                        // distinguish between PowerService and OppositePowerService depending on which pedal we're calibrating
                        // write the descriptor to power meter; we'll write characteristic when onWriteDescriptor called
                        if (thePowerDevice.getAddress().equals(powerDeviceData.getAddress())) {
                            if (debugAppState) {Log.wtf(logtag, "Sending calibration command to PowerService");}
                            mBLEPowerService.setCharacteristicIndication(pcpCharacteristic);
                        } else {
                            if (debugAppState) {Log.wtf(logtag, "Sending calibration command to OppositePowerService");}
                            mBLEOppositePowerService.setCharacteristicIndication(pcpCharacteristic);
                        }
                        return submitted;
                    }
                });// positive button
		builder.show();
	}

	private void doSearchPair(final BLEDeviceType deviceType) {
        searchPairDeviceType = deviceType;
        switch (searchPairDeviceType){
            case BIKE_POWER_DEVICE:
                searchPairNamesList = powerDeviceSPList;
                break;
            case BIKE_CADENCE_DEVICE:
                searchPairNamesList = cadenceDeviceSPList;
                break;
            case HEARTRATE_DEVICE:
                searchPairNamesList = hrmDeviceSPList;
                break;
            case BIKE_SPD_DEVICE:
                searchPairNamesList = speedDeviceSPList;
                break;
            case BIKE_SPDCAD_DEVICE:
                searchPairNamesList = spdcadDeviceSPList;
                break;
            default:
                break;
        }
        mSPAdapter = new SearchPairAdapter(MainActivity.this, searchPairNamesList);
        spListView.setAdapter(mSPAdapter);
        mSPAdapter.notifyDataSetChanged();
		if (debugLEScan) { Log.i(logtag, DO_PAIR_DEVICE + deviceType.name());}
        showActiveDeviceDialog();
	}// do SearchPair

    private void showActiveDeviceDialog() {
        if (debugBLESearchPair) {Log.i(logtag, "showActiveDeviceDialog");}
        searchPairLayout.setVisibility(View.VISIBLE);
        // dismiss the search-pair layout when cancel pressed
        Button cancelSearch = findViewById(R.id.cancelSearchButton);
        cancelSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (debugBLESearchPair) {Log.i(logtag, "showActiveDeviceDialog - cancel clicked");}
                searchPairLayout.setVisibility(View.GONE);
            }
        });
        spListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (debugBLESearchPair) {Log.i(logtag, "showActiveDeviceDialog - onItemClick position " + position);}
                searchPairLayout.setVisibility(View.GONE);
                HashMap<String, String> theData;
                if (searchPairNamesList.size() > position) {
                    theData = searchPairNamesList.get(position);
                    String theDeviceAddress = theData.get(DB_KEY_DEV_ADDRESS);
                    String theDeviceName = theData.get(KEY_SP_DEVICE_NAME);
                    if (theDeviceName == null || theDeviceName.equals("")) {
                        theDeviceName = "<" + theDeviceAddress + ">";
                    }
                    BLEDeviceType theDeviceType = mBLEDeviceManager.getDevAddressType(theDeviceAddress);
                    if (debugBLESearchPair) {
                        Log.i(logtag, "showActiveDeviceDialog - onItemClick address" + theDeviceAddress
                                + " name: " + theDeviceName + " deviceType: " + theDeviceType.name());
                    }
                    ContentValues dataContent = new ContentValues();
                    //set Content to add device to dataBase; need name, address and type
                    dataContent.put(DB_KEY_SEARCH_PRIORITY, 1);
                    dataContent.put(DB_KEY_DEV_ADDRESS, theDeviceAddress);
                    dataContent.put(DB_KEY_DEV_NAME, theDeviceName);
                    dataContent.put(DB_KEY_DEV_TYPE, Integer.toString(theDeviceType.intValue()));
                    //if device is not in data base, add it. This is the only way a device gets into the database!
                    if (!dataBaseAdapter.isDeviceInDataBase(theDeviceAddress)) {
                        if (debugBLESearchPair) {Log.i(logtag, "showActiveDeviceDialog - device not in DB dev address: " + theDeviceAddress);}
                        dataBaseAdapter.addDeviceToDB(dataContent);
                    }
                    mBLEDeviceManager.updateActiveBikeDeviceData(theDeviceAddress, dataContent);
                    // now connect to the selected device.
                    // Use deviceType as reported in the ActiveBikeList, not what we were searching for.
                    doCleanUp(theDeviceAddress, theDeviceType);
                }
            }
        });// OnItemClick
    }

    private void doCleanUp(final String deviceAddress, final BLEDeviceType deviceType) {
        // connect to device selected
        if (debugBLESearchPair) {Log.i(logtag, "doCleanUp()");}

        switch (deviceType) {
            case BIKE_CADENCE_DEVICE:
                cadDeviceData.setAddress(deviceAddress);
                if (mBLECadenceService.isServiceConnected()){
                    mBLECadenceService.disconnect();
                }
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        boolean result = mBLECadenceService.connect(deviceAddress);
                        if (debugBLESearchPair) {Log.i(logtag, "Cadence "+ PAIRING_REQUEST_RESULT + (result?INITIATED:FAILED));}
                    }
                }, 500);
                break;
            case BIKE_SPD_DEVICE:
                speedDeviceData.setAddress(deviceAddress);
                if (mBLESpeedService.isServiceConnected()) {
                    mBLESpeedService.disconnect();
                }
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        boolean result = mBLESpeedService.connect(deviceAddress);
                        if (debugBLESearchPair) {Log.i(logtag, "Speed "+ PAIRING_REQUEST_RESULT + (result?INITIATED:FAILED));}
                    }
                }, 500);
                break;
            case BIKE_POWER_DEVICE:
                powerDeviceData.setAddress(deviceAddress);
                if (mBLEPowerService.isServiceConnected()) {
                    mBLEPowerService.disconnect();
                }
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        boolean result = mBLEPowerService.connect(deviceAddress);
                        if (debugBLESearchPair) {Log.i(logtag, "Power "+ PAIRING_REQUEST_RESULT + (result?INITIATED:FAILED));}
                    }
                }, 500);
                break;
            case HEARTRATE_DEVICE:
                hrmDeviceData.setAddress(deviceAddress);
                if (mBLEHRMService.isServiceConnected()) {
                    mBLEHRMService.disconnect();
                }
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        boolean result = mBLEHRMService.connect(deviceAddress);
                        if (debugBLESearchPair) {Log.i(logtag, "HRM "+ PAIRING_REQUEST_RESULT + (result?INITIATED:FAILED));}
                    }
                }, 500);
                break;
            case BIKE_SPDCAD_DEVICE:
                // disconnect any cadence device
                if (mBLECadenceService.isServiceConnected()){
                    mBLECadenceService.disconnect();
                }
                mBLEDeviceManager.pedalCadenceCnts.initialized = false;
                mBLEDeviceManager.wheelCnts.initialized = false;
                speedDeviceData.setAddress(deviceAddress);
                if (mBLESpeedService.isServiceConnected()) {
                    mBLESpeedService.disconnect();
                }
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        boolean result = mBLESpeedService.connect(deviceAddress);
                        if (debugBLESearchPair) {Log.i(logtag, "Speed "+ PAIRING_REQUEST_RESULT + (result?INITIATED:FAILED));}
                    }
                }, 500);
                cadDeviceData.setAddress(deviceAddress);
                break;
            default:
                break;
        }
    }

    private void showDialog(String deviceAddress) {
	    if (deviceAddress == null){return;}
        updateDBData(" - from showDialog()");
	    Bundle dialogBundle = new Bundle();
        dialogBundle.putBoolean(DDF_KEY_DEVICE_ACTIVE, true);
        int devType = mBLEDeviceManager.getDevAddressType(deviceAddress).intValue();
        String title = composeDeviceDialogTitle(this, BLEDeviceType.valueOf(devType));
        dialogBundle.putCharSequence(DDF_KEY_TITLE, title);
        dialogBundle.putInt(DDF_KEY_DEVICE_TYPE, devType);
        dialogBundle.putCharSequence(DDF_KEY_ADDRESS, deviceAddress);
	    dialogBundle.putCharSequence(DDF_KEY_MESSAGE, composeDeviceDialogMessage(this, deviceAddress, dataBaseAdapter));
        MADeviceDialogFragment newFragment = MADeviceDialogFragment.newInstance(dialogBundle);
        newFragment.show(getFragmentManager(), "Main");
    }

    private void showGPSDialog() {
        Bundle dialogBundle = new Bundle();
        String title = composeGPSDialogTitle(this);
        dialogBundle.putCharSequence(DDF_KEY_TITLE, title);
        dialogBundle.putCharSequence(DDF_KEY_MESSAGE, composeGPSDialogMessage(this, myBikeStat));
        MADeviceDialogFragment newFragment = MADeviceDialogFragment.newInstance(dialogBundle);
        newFragment.show(getFragmentManager(), "Main-GPS");
    }

    public void doForgetClick(String devAddress, int devType) {
        stopLEScanner();
        if (dataBaseAdapter != null) {
            dataBaseAdapter.doForget(devAddress);
        }
        BLEDeviceType bikeDeviceType = BLEDeviceType.valueOf(devType);
        mBLEDeviceManager.forgetDevice(devAddress);
        // disconnect device because were tracking it.
        switch (bikeDeviceType){
            case BIKE_CADENCE_DEVICE:
                mBLECadenceService.disconnect();
                break;
            case BIKE_SPDCAD_DEVICE:
                mBLESpeedService.disconnect();
                mBLECadenceService.disconnect();
                break;
            case BIKE_SPD_DEVICE:
                mBLESpeedService.disconnect();
                break;
            case BIKE_POWER_DEVICE:
            case BIKE_RIGHT_POWER_DEVICE:
            case BIKE_LEFT_POWER_DEVICE:
                mBLEPowerService.disconnect();
                mBLEOppositePowerService.disconnect();
                break;
            case HEARTRATE_DEVICE:
                mBLEHRMService.disconnect();
                break;
        }
        refreshDeviceLists();
    }

    public void doCancelClick() {
        // Do stuff here.
    }
	private void openReopenTCX_FIT() {
		if (debugAppState) Log.i(logtag, "openReopenTCX_FIT()");
		//LocationSpoofer on first Location, called from onLocationChanged, when firstLocation is true, and in writeTrackRecord() if an error occurred
        // and if we were sharing the current activity file
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		myBikeStat.fitLog.purgeSmallActivityFiles(myBikeStat, settings.getString(KEY_CHOSEN_TCXFILE, ""));
		boolean old = myBikeStat.tcxLog.readTCXFileLastModTime(myBikeStat.tcxLog.outFileName,
                Utilities.getTCXFileAutoReset(getApplicationContext()));
		myBikeStat.tcxLog.outFileFooterLength = settings.getInt(TCX_LOG_FILE_FOOTER_LENGTH, 1);
		// open a new tcx file if the previous one is old, we force a new one
		// thru reset, or loading a new route and clearing data, or the file was
		// not found when testing last modified date
		if (old || forceNewTCX_FIT || !myBikeStat.tcxLog.getError().equals("")) {
            if (debugAppState) { Log.i(logtag, "openReopenTCX_FIT() - file old or forceNew"); }
            if (debugOldTCXFile) {
                Log.i(logtag, "openReopenTCX_FIT() old? " + (old ? " yes" : "no")
                        + " forceNew? " + (forceNewTCX_FIT ? " yes" : " no"
                        + " tcxLogError? " + (!myBikeStat.tcxLog.getError().equals("") ? " yes" : " no")));
            }
            // Compose filename using current date-time
			// Need to do this before calling .fitLog.openNewFIT, because the .fit file has the same name
			resetData();
			forceNewTCX_FIT = false;
			myBikeStat.tcxLog.outFileName = myBikeStat.tcxLog.composeTCXFileName();
			myBikeStat.tcxLog.openNewTCX(myBikeStat, myNavRoute);
            new ThreadPerTaskExecutor().execute(openNewFitFileBackgroundRunnable);
		} else {
			if (debugAppState) Log.i(logtag, "openReopenTCX_FIT() - file not old & not forceNew");
			// not old and not forceNewTCX, so re-open tcx & fit
			// restore outfilefooterlength before re-opening
			myBikeStat.tcxLog.reopenTCX(myBikeStat, myNavRoute);
			// re-open the fit file
            new ThreadPerTaskExecutor().execute(reopenFitFileBackgroundRunnable);
		}
		editor.putString(TCX_LOG_FILE_NAME, myBikeStat.tcxLog.outFileName);
		editor.putInt(TCX_LOG_FILE_FOOTER_LENGTH, myBikeStat.tcxLog.outFileFooterLength).apply();
	}

	/**
	 * When tracking a new device, update the data base about the new device
	 * status and also change the search priority of the other devices of the
	 * same type.
	 *
	 * @param content is data about the device
	 */
    private void updateDBTracking(final ContentValues content) {
        if (debugLEScan) {Log.i(logtag, UPDATE_DBTRACKING);}
        // update data and reset search priority in database
        dataBaseAdapter.updateDeviceRecord(content.getAsString(DB_KEY_DEV_ADDRESS), content);
    }

    private void trackingPower(BLEDeviceData thisDeviceData) {
        if (debugLEScan) {Log.i(logtag, TRACKING_POWER);}
        if (thisDeviceData != null) {
            ContentValues content = new ContentValues();
            content.put(DB_KEY_DEV_ADDRESS, thisDeviceData.getAddress());
            content.put(DB_KEY_SEARCH_PRIORITY, 1);
            content.put(DB_KEY_ACTIVE, 1);
            updateDBTracking(content);
            mBLEDeviceManager.updateActiveBikeDeviceData(thisDeviceData.getAddress(), content);
            mBLEDeviceManager.resetSearchPriority(thisDeviceData.getAddress(), BIKE_POWER_DEVICE);
        }

        if (!mBLEDeviceManager.powerWheelCnts.isCalibrated && !trainerMode) {
            mBLEDeviceManager.restartPowerWheelCal(myBikeStat.getWheelTripDistance());
        } else {// already calibrated, so enable .hasCalSpeedSensor
            myBikeStat.hasCalPowerSpeedSensor = true;
        }
        myBikeStat.hasPower = true;
        // time-tag sensor start for ride-time calculation
        mBLEDeviceManager.powerWheelCnts.prevTime = SystemClock.elapsedRealtime();
        myBikeStat.hasCalPowerSpeedSensor = mBLEDeviceManager.powerWheelCnts.isCalibrated;
    }

    private void notTrackingPower(BLEDeviceData thisDeviceData) {
		if (debugLEScan) {Log.i(logtag, NOT_TRACKING_POWER);}
		if (!myBikeStat.hasLeftPower && !myBikeStat.hasRightPower){
            myBikeStat.hasPower = false;
            myBikeStat.hasPowerCadence = false;
            myBikeStat.setPowerCadence(0);
            myBikeStat.setPower(0);
        }
		myBikeStat.hasCalPowerSpeedSensor = false;
		myBikeStat.hasPowerSpeedSensor = false;
        if (thisDeviceData != null){
            if (thisDeviceData.getDeviceType() == BIKE_LEFT_POWER_DEVICE) {
                myBikeStat.hasLeftPower = false;
            } else {
                myBikeStat.hasRightPower = false;
            }
            thisDeviceData.status = SEARCHING;
        }
        myBikeStat.setPowerSpeed(0.);
        refreshSpeed();
		refreshPower();
        refreshCadence();
        // remove all BikePower devices from active list so we won't try to autoConnect() to them: very bad
        mBLEDeviceManager.resetActiveStatusByType(BLEDeviceType.BIKE_POWER_DEVICE);
	}

	private void trackingSpeed() {
		if (debugLEScan) {Log.i(logtag, TRACKING_SPEED);}
		if (speedDeviceData != null) {
            speedDeviceData.status = TRACKING;
			ContentValues content = new ContentValues();
            // this could be a SPD_DEVICE or a SPDCAD_DEVICE
			int deviceType = speedDeviceData.getDeviceType().intValue();
			content.put(DB_KEY_DEV_ADDRESS, speedDeviceData.getAddress());
			content.put(DB_KEY_SEARCH_PRIORITY, 1);
			content.put(DB_KEY_ACTIVE, 1);
			content.put(DB_KEY_DEV_TYPE, deviceType);
            updateDBTracking(content);
			mBLEDeviceManager.updateActiveBikeDeviceData(speedDeviceData.getAddress(), content);
			mBLEDeviceManager.resetSearchPriority(speedDeviceData.getAddress(), speedDeviceData.getDeviceType());
		}
        if (!mBLEDeviceManager.wheelCnts.isCalibrated && !trainerMode) {
            mBLEDeviceManager.restartWheelCal(myBikeStat.getWheelTripDistance());
        } else {// already calibrated, so enable .hasCalSpeedSensor
            myBikeStat.hasCalSpeedSensor = true;
        }
		// time-tag sensor start for ride-time calculation
		mBLEDeviceManager.wheelCnts.prevTime = SystemClock.elapsedRealtime();
		// we don't claim .hasCalSpeedSensor until the wheel is calibrated
		myBikeStat.hasCalSpeedSensor = mBLEDeviceManager.wheelCnts.isCalibrated;
		// wait until we get speed data before claiming we have the speed sensor
	}

	private void notTrackingSpeed() {
		if (debugLEScan) {Log.i(logtag, NOT_TRACKING_SPEED);}
		myBikeStat.hasSpeedSensor = false;
		myBikeStat.hasCalSpeedSensor = false;
        if (speedDeviceData != null){
            speedDeviceData.status = SEARCHING;
        }
        myBikeStat.setSensorSpeed(0.);
        refreshSpeed();
        // remove all BikeSPD devices from active list so we won't try to autoConnect() to them: very bad
        mBLEDeviceManager.resetActiveStatusByType(BLEDeviceType.BIKE_SPD_DEVICE);
	}

	private void trackingCad() {
		if (debugLEScan) {Log.i(logtag, TRACKING_CAD);}
		ContentValues content = new ContentValues();
		myBikeStat.hasCadence = true;
		if (cadDeviceData != null) {
            cadDeviceData.status = TRACKING;
            // this could be a CAD_DEVICE or a SPDCAD_DEVICE
            int deviceType = cadDeviceData.getDeviceType().intValue();
            content.put(DB_KEY_DEV_ADDRESS, cadDeviceData.getAddress());
			content.put(DB_KEY_SEARCH_PRIORITY, 1);
			content.put(DB_KEY_ACTIVE, 1);
			content.put(DB_KEY_DEV_TYPE, deviceType);
            updateDBTracking(content);
            mBLEDeviceManager.updateActiveBikeDeviceData(cadDeviceData.getAddress(), content);
			mBLEDeviceManager.resetSearchPriority(cadDeviceData.getAddress(), cadDeviceData.getDeviceType());
		}
	}

	private void notTrackingCad() {
		if (debugLEScan) {Log.i(logtag, NOT_TRACKING_CAD);}
		myBikeStat.hasCadence = false;
		myBikeStat.setCadence(0);
        if (cadDeviceData != null){
            cadDeviceData.status = SEARCHING;
        }
		refreshCadence();
        // remove all BikeCadence devices from active list so we won't try to autoConnect() to them: very bad
        mBLEDeviceManager.resetActiveStatusByType(BLEDeviceType.BIKE_CADENCE_DEVICE);
	}

	private void trackingHRM() {
		if (debugLEScan) {Log.i(logtag, TRACKING_HRM);}
		if (hrmDeviceData != null) {
            hrmDeviceData.status = TRACKING;
			ContentValues content = new ContentValues();
			content.put(DB_KEY_DEV_ADDRESS, hrmDeviceData.getAddress());
			content.put(DB_KEY_DEV_TYPE, HEARTRATE_DEVICE.intValue());
			content.put(DB_KEY_SEARCH_PRIORITY, 1);
			content.put(DB_KEY_ACTIVE, 1);
            updateDBTracking(content);
			mBLEDeviceManager.updateActiveBikeDeviceData(hrmDeviceData.getAddress(), content);
			mBLEDeviceManager.resetSearchPriority(hrmDeviceData.getAddress(), HEARTRATE_DEVICE);
		}
		myBikeStat.hasHR = true;
	}

	private void notTrackingHRM() {
		if (debugLEScan) {Log.i(logtag, NOT_TRACKING_HRM);}
		myBikeStat.hasHR = false;
        if (hrmDeviceData != null){
            hrmDeviceData.status = SEARCHING;
        }
		refreshHR();
        // remove all HEARTRATE devices from active list so we won't try to autoConnect() to them: very bad
        mBLEDeviceManager.resetActiveStatusByType(BLEDeviceType.HEARTRATE_DEVICE);
	}

	private void viewToast(final String toastText, final int yOffset,
			final int gravity, final View view, final int color) {
		if (MainActivity.this.isFinishing()) { return; }
		view.post(new Runnable() {
			@Override
			public void run() {
				int loc[] = new int[2];
				view.getLocationOnScreen(loc);
				Toast toast = Toast.makeText(toastAnchor.getContext(), toastText, Toast.LENGTH_SHORT);
				toast.setGravity(gravity, 0, loc[1] + yOffset);
				toast.getView().setBackgroundColor(ContextCompat.getColor(context, R.color.bkgnd_black));
				TextView v = toast.getView().findViewById(android.R.id.message);
				v.setTextColor(color);
				toast.show();
			}
		});
	}

	/**
	 * For each device in the bleActiveBikeDeviceList, get the content and update the
	 * database. mBLEDeviceManager doesn't know about the database, so we'll get
	 * content from mBLEDeviceManager and pass it to BLEDBAdapter
	 * Only call this  in saveState()
	 *
	 * @param string just an indication for debugging as to where we called this method
	 */
	private void updateDBData(String string) {

		int activeListSize = mBLEDeviceManager.bleActiveBikeDeviceList.size();
		if (activeListSize == 0) {
			return;
		}
		if (debugLEScan) Log.i(logtag, UPDATE_DB + string + ACTIVE_LIST_SIZE + activeListSize);
		for (int index = 0; index < activeListSize; index++) {
			BLEDeviceData deviceData = mBLEDeviceManager.getActiveBikeDeviceData(index);
			if (debugLEScan) Log.i(logtag, DEVICE_DATA_DEV_ADDRESS + deviceData.getAddress());
			dataBaseAdapter.updateDeviceRecord(deviceData.getAddress(), deviceData.getData());
		}
	}

    private TimerTask autoConnectBLE;
    private final Timer autoConnectBLETimer = new Timer();

    /**
     * A watchdog timer to connect Bluetooth devices. Start and stop the LEScan, then
     * try to connect to devices found. Timer runs every 30 seconds from OnCreate until onDestroy.
     * for three minutes after resuming motion from a Pause
     */
    private void startAutoConnectBLE() {
        autoConnectBLE = new TimerTask() {

            @Override
            public void run() {
                // If pairing we don't want to scanForLEDevices here; we call for a seperate LEScan.
                // Don't connect or search if not using BLE data.
                if (!useBLEData) {return;}
                // clear Active status in the ActiveDBDeviceList
                mBLEDeviceManager.resetActiveStatusAll();
                spListView.post(new Runnable() {
                    @Override
                    public void run() {
                        mSPAdapter = new SearchPairAdapter(MainActivity.this, searchPairNamesList);
                        spListView.setAdapter(mSPAdapter);
                        mSPAdapter.notifyDataSetChanged();
                    }
                });
                scanForBLEDevices(false);
                 sensorWatchdogHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        scanForBLEDevices(allowBLEScanner);
                    }
                });
                // can't autoConnect to devices while LEScan is running, wait a couple seconds for LEScan to stop
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        autoConnectSpeedCad(mBLEDeviceManager.getActiveDevInfoByType(BIKE_SPDCAD_DEVICE));
                    }
                }, SCAN_PERIOD + ONE_SEC);
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        autoConnectHRM(mBLEDeviceManager.getActiveDevInfoByType(HEARTRATE_DEVICE));
                    }
                }, SCAN_PERIOD + 2 * ONE_SEC);
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // connect to BIKE_LEFT or BIKE_POWER
                        autoConnectPower(mBLEDeviceManager.getActiveDevInfoByType(BIKE_POWER_DEVICE));
                    }
                }, SCAN_PERIOD + 3 * ONE_SEC);
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // connect to BIKE_RIGHT
                        autoConnectOppositePower(mBLEDeviceManager.getActiveDevInfoByType(BIKE_RIGHT_POWER_DEVICE));
                    }
                }, SCAN_PERIOD + 4 * ONE_SEC);
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        autoConnectCadence(mBLEDeviceManager.getActiveDevInfoByType(BIKE_CADENCE_DEVICE));
                    }
                }, SCAN_PERIOD + 5 * ONE_SEC);
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        autoConnectSpeed(mBLEDeviceManager.getActiveDevInfoByType(BIKE_SPD_DEVICE));
                    }
                }, SCAN_PERIOD + 6 * ONE_SEC);
            }
        };
        autoConnectBLETimer.schedule(autoConnectBLE, ONE_SEC, THIRTY_SEC);
    }

    private void scanForBLEDevices(boolean enable) {
        TextView btStatus = findViewById(R.id.ble_status_button);
        if (mBluetoothAdapter == null) {
            btStatus.setTextColor(ContextCompat.getColor(context, R.color.gpsred));
            if (debugLEScan) { Log.e(logtag, SCANLE_DEVICES + "Bluetooth Adapter is null");}
            return;
        }
        if (enable) {
            // clear the search-pair device lists; we'll repopulate them when a new device is discovered in LEScan callback
            clearActiveDeviceLists();
            // set cadDeviceData, speedDeviceData, etc device status to active since we can't scan bonded devices
            if (cadDeviceData != null && cadDeviceData.status == TRACKING) {
                mBLEDeviceManager.setDevAddressActiveStatus(cadDeviceData.getAddress(), 1);
            }
            if (speedDeviceData != null && speedDeviceData.status == TRACKING) {
                mBLEDeviceManager.setDevAddressActiveStatus(speedDeviceData.getAddress(), 1);
            }
            if (hrmDeviceData != null && hrmDeviceData.status == TRACKING) {
                mBLEDeviceManager.setDevAddressActiveStatus(hrmDeviceData.getAddress(), 1);
            }
            if (powerDeviceData != null && powerDeviceData.status == TRACKING) {
                mBLEDeviceManager.setDevAddressActiveStatus(powerDeviceData.getAddress(), 1);
            }
            if (oppositePowerDeviceData != null && oppositePowerDeviceData.status == TRACKING) {
                mBLEDeviceManager.setDevAddressActiveStatus(oppositePowerDeviceData.getAddress(), 1);
            }
            boolean canceledDiscovery = cancelBTDiscovery();
            if (canceledDiscovery && debugLEScan){ Log.w(logtag, SCANLE_DEVICES + "had to cancel Bluetooth Discovery"); }
                // Stops scanning after a pre-defined scan period.
                sensorWatchdogHandler.postDelayed(this::stopLEScanner, SCAN_PERIOD);
                if (debugLEScan) {Log.wtf(logtag, SCANLE_DEVICES + ".startLEScan()-LEDiscoveryScan status" + getBTAdapterStatusString(mBluetoothAdapter));}
                mBluetoothAdapter.stopLeScan(mLeDiscoveryScanCallback);
                boolean startedScan = mBluetoothAdapter.startLeScan(mLeDiscoveryScanCallback);
                boolean hasDevices = mBLEDeviceManager.isAnyBikeDeviceActive() || mBLEDeviceManager.bleOtherDeviceList.size() > 0;
                if (!startedScan) {
                    mBLEDeviceManager.setBLEScannerStatus(BLEScannerStatus.DEAD);
                    updateBTScannerStatusDisplay(BLEScannerStatus.DEAD);
                    Log.wtf(logtag, "couldn't start Discovery LeScan" + getBTAdapterStatusString(mBluetoothAdapter));
                } else {
                    BLEScannerStatus mStatus = hasDevices?BLEScannerStatus.OKAY:BLEScannerStatus.NO_DEVICES;
                    mBLEDeviceManager.setBLEScannerStatus(mStatus);
                    updateBTScannerStatusDisplay(mStatus);
                }
        } else {// disable
            try {
                stopLEScanner();
            } catch (Exception ignore){}
        }
    }

    private void stopLEScanner() {
        if (debugLEScan) { Log.i(logtag, SCANLE_DEVICES + "stopping scan");}
        mBLEDeviceManager.setBLEScannerStatus(BLEScannerStatus.STOPPED);
        updateBTScannerStatusDisplay(BLEScannerStatus.STOPPED);
        mBluetoothAdapter.stopLeScan(mLeDiscoveryScanCallback);
    }

    private void autoConnectHRM(final String mDeviceAddress) {
        if (hrmDeviceData.status == TRACKING) {
            if (debugBLEService) {Log.i(logtag, "autoConnectHRM: TRACKING");}
            return;
        }
        if (mDeviceAddress == null) {
            if (debugBLEService) { Log.i(logtag, "autoConnectHRM: no device address");}
            return;
        }
        if (mBLEHRMService == null) {
            boolean connecting = bindService(new Intent(this, BLEHRMService.class), mHRMServiceConnection, BIND_AUTO_CREATE);
            if (debugBLEService) {Log.w(logtag, "autoConnectHRM: no HRMService, re-binding Service (success? )" + (connecting?"yes":"no"));}
            return;
        }
        hrmDeviceData.setAddress(mDeviceAddress);
        if (mBLEHRMService.isServiceConnected()) {
            if (debugBLEService) {Log.w(logtag, "autoConnectingHRM, Service already connected. Disconnecting");}
            mBLEHRMService.disconnect();
        }
        sensorWatchdogHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean result = mBLEHRMService.connect(mDeviceAddress);
                if (debugBLEService) {Log.i(logtag, "HRM connect request result = " + (result ? INITIATED : FAILED));}
            }
        }, 500);
    }
    private void autoConnectOppositePower(final String mDeviceAddress) {
        if (mBLEOppositePowerService == null) {
            boolean connecting = bindService(new Intent(this, BLEOppositePowerService.class), mOppositePowerServiceConnection, BIND_AUTO_CREATE);
            if (debugBLEService) {Log.w(logtag, "autoConnectOppositePower: no PowerService, re-binding Service (success? )" + (connecting?"yes":"no"));}
            return;
        }
        if (oppositePowerDeviceData.status == TRACKING) {
            if (debugBLEService) {Log.i(logtag, "autoConnectOppositePower: TRACKING");}
            return;
        }
        if (mDeviceAddress == null) {
            if (debugBLEService) {Log.i(logtag, "autoConnectOppositePower: no device address");}
            return;
        }
        oppositePowerDeviceData.setAddress(mDeviceAddress);
        if (mBLEOppositePowerService.isServiceConnected()) {
            if (debugBLEService) {Log.w(logtag, "autoConnectOppositePower, Service already connected. Disconnecting");}
            mBLEOppositePowerService.disconnect();
        }
        sensorWatchdogHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean result = mBLEOppositePowerService.connect(mDeviceAddress);
                if (debugBLEService) {Log.i(logtag, "Opposite Power connect request result = " + (result ? INITIATED : FAILED));}
            }
        }, 500);
    }
    private void autoConnectPower(final String mDeviceAddress) {
        if (mBLEPowerService == null) {
            boolean connecting = bindService(new Intent(this, BLEPowerService.class), mPowerServiceConnection, BIND_AUTO_CREATE);
            if (debugBLEService) {Log.w(logtag, "autoConnectPower: no PowerService, re-binding Service (success? )" + (connecting?"yes":"no"));}
            return;
        }
        if (powerDeviceData.status == TRACKING) {
            if (debugBLEService) {Log.i(logtag, "autoConnectPower: TRACKING");}
            return;
        }
        if (mDeviceAddress == null) {
            if (debugBLEService) { Log.i(logtag, "autoConnectPower: no device address");}
            return;
        }
        powerDeviceData.setAddress(mDeviceAddress);
        if (mBLEPowerService.isServiceConnected()) {
            if (debugBLEService) {Log.w(logtag, "autoConnectPower, Service already connected. Disconnecting");}
            mBLEPowerService.disconnect();
        }
        sensorWatchdogHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean result = mBLEPowerService.connect(mDeviceAddress);
                if (debugBLEService) {Log.i(logtag, "Power connect request result = " + (result ? INITIATED : FAILED));}
            }
        }, 500);
    }

    private void autoConnectSpeed(final String mDeviceAddress) {
        if (mBLESpeedService == null) {
            boolean connecting = bindService(new Intent(this, BLESpeedService.class), mSpeedServiceConnection, BIND_AUTO_CREATE);
            if (debugBLEService) {Log.w(logtag, "autoConnectSpeed: no SpeedService, re-binding Service (success? )" + (connecting?"yes":"no"));}
            return;
        }
        if (mDeviceAddress == null) {
            if (debugBLEService) { Log.i(logtag, "autoConnectSpeed: no device address");}
            return;
        }
        if (trackingSpeedCad()){
            if (debugBLEService) {Log.i(logtag, "autoConnectSpeed: TRACKING SpeedCad");}
            return;
        }
        String spdcadAddress = mBLEDeviceManager.getActiveDevInfoByType(BLEDeviceType.BIKE_SPDCAD_DEVICE);
        if (spdcadAddress != null && !(spdcadAddress).equals(mDeviceAddress)) {
            if (debugBLEService) {Log.i(logtag, "autoConnectSpeed: have a SPDCAD device, disconnecting Spd Device"
                    + " spdcad address: " + spdcadAddress + " mDeviceAddress: " + mDeviceAddress);}
            return;
        }
        if (speedDeviceData.status == TRACKING) {
            if (debugBLEService) {Log.i(logtag, "autoConnectSpeed: TRACKING");}
            return;
        }
        speedDeviceData.setAddress(mDeviceAddress);
        // we're looking for a Speed device, not a SPD_Cad device, so set Type
        BLEDeviceType theSpeedDeviceType = mBLEDeviceManager.getDevAddressType(mDeviceAddress);
        if (debugBLEService) { Log.i(logtag, "autoConnectSpeed - theSpeedDeviceType: " + theSpeedDeviceType.name());}
        speedDeviceData.setDeviceType(theSpeedDeviceType);
        if (mBLESpeedService.isServiceConnected()) {
            if (debugBLEService) {Log.w(logtag, "autoConnectSpeed, Service already connected. Disconnecting");}
            mBLESpeedService.disconnect();
        }
        sensorWatchdogHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean result = mBLESpeedService.connect(mDeviceAddress);
                if (debugBLEService) {Log.i(logtag, "Speed connect request result = " + (result ? INITIATED : FAILED));}
            }
        }, 500);
    }

    private void autoConnectSpeedCad(String mDeviceAddress) {
        if (mBLESpeedService == null) {
            boolean connecting = bindService(new Intent(this, BLESpeedService.class), mSpeedServiceConnection, BIND_AUTO_CREATE);
            if (debugBLEService) {Log.w(logtag, "autoConnectSpeedCad: no SpeedService, re-binding Service (success? )"
                    + (connecting?"yes":"no"));}
            return;
        }
        if (trackingSpeedCad()){
            if (debugBLEService) {Log.w(logtag, "autoConnectSpeedCad: TRACKING SpeedCad");}
            return;
        }
        if (mDeviceAddress == null) {
            if (debugBLEService) { Log.i(logtag, "autoConnectSpeedCad: no device address");}
            return;
        }
        if (mBLECadenceService.isServiceConnected()){
            if (debugBLEService){Log.w(logtag, "autoConnectSpeedCad, cadence Service already connected. Disconnecting");}
            mBLECadenceService.disconnect();
        }
        mBLEDeviceManager.pedalCadenceCnts.initialized = false;
        mBLEDeviceManager.wheelCnts.initialized = false;
        autoConnectSpeed(mDeviceAddress);
        cadDeviceData.setAddress(mDeviceAddress);
        //we're looking for SPD_CAD device, so set Device Type
        BLEDeviceType theDeviceType = mBLEDeviceManager.getDevAddressType(mDeviceAddress);
        if (debugBLEService) { Log.i(logtag, "autoConnectSpeedCad - theDeviceType: " + theDeviceType.name());}
        cadDeviceData.setDeviceType(theDeviceType);
        // also set cadDevice.status to TRACKING so we won't connect to another cadence device
        cadDeviceData.status = BLEDeviceStatus.TRACKING;
        BLEDeviceType theSpeedDeviceType = mBLEDeviceManager.getDevAddressType(mDeviceAddress);
        if (debugBLEService) { Log.i(logtag, "autoConnectSpeedCad - theSpeedDeviceType: " + theSpeedDeviceType.name());}
        speedDeviceData.setDeviceType(theSpeedDeviceType);
    }

    private boolean trackingSpeedCad() {
        if (cadDeviceData == null || speedDeviceData == null) {
            return false;
        }
        String cadAddress = cadDeviceData.getAddress();
        String speedAddress = speedDeviceData.getAddress();
        return !(cadAddress == null || speedAddress == null)
                && cadDeviceData.getAddress().equals(speedDeviceData.getAddress())
                && cadDeviceData.status == BLEDeviceStatus.TRACKING
                && speedDeviceData.status == BLEDeviceStatus.TRACKING;
    }

    private void autoConnectCadence(final String mDeviceAddress) {
        if (cadDeviceData.status == TRACKING) {
            if (debugBLEService) {Log.i(logtag, "autoConnectCadence: TRACKING");}
            return;
        }
        if (mDeviceAddress == null) {
            if (debugBLEService) { Log.i(logtag, "autoConnectCadence: no device address");}
            return;
        }
        if (mBLECadenceService == null) {
            boolean connecting = bindService(new Intent(this, BLECadenceService.class), mCadenceServiceConnection, BIND_AUTO_CREATE);
            if (debugBLEService) {Log.w(logtag, "autoConnectCadence: no CadenceService, re-binding Service (success? )"+ (connecting?"yes":"no"));}
            return;
        }
        cadDeviceData.setAddress(mDeviceAddress);
        // we're looking for a Cadence Device, so set Device Type
        BLEDeviceType theDeviceType = mBLEDeviceManager.getDevAddressType(mDeviceAddress);
        if (debugBLEService) { Log.i(logtag, "autoConnectCadence - theDeviceType: " + theDeviceType.name());}
        cadDeviceData.setDeviceType(theDeviceType);
        if (mBLECadenceService.isServiceConnected()) {
            if (debugBLEService) {Log.w(logtag, "autoConnectCadence, Service already connected. Disconnecting");}
            mBLECadenceService.disconnect();
        }
        sensorWatchdogHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean result = mBLECadenceService.connect(mDeviceAddress);
                if (debugBLEService) {Log.i(logtag, "Cadence connect request result = " + (result ? INITIATED : FAILED));}
            }
        }, 5);
    }

    private void stopAutoConnectBLE() {
        if (debugBLEData) { Log.i(logtag, "stopAutoConnectBle()");}
        scanForBLEDevices(false);
        if (autoConnectBLE != null) {
            autoConnectBLE.cancel();
        }
    }

    private void updateBTScannerStatusDisplay(BLEScannerStatus status) {
        int bleStatusColor;
        switch (status) {
            case OKAY:
                bleStatusColor = ContextCompat.getColor(context, R.color.gpsgreen);
                break;
            case STOPPED:
                bleStatusColor = ContextCompat.getColor(context, R.color.texthiviz);
                break;
            case NO_DEVICES:
                bleStatusColor = ContextCompat.getColor(context, R.color.status_orange);
                break;
            case DEAD:
            default:
                bleStatusColor = ContextCompat.getColor(context, R.color.gpsred);
                break;
        }
        TextView bleStatusButton = findViewById(R.id.ble_status_button);
        bleStatusButton.setTextColor(bleStatusColor);
    }

    private boolean cancelBTDiscovery() {
        // can't scan for Bluetooth LE devices if Classic Bluetooth scan is active
        // BT Adapter also has to be ON before we can cancel Discovery
        boolean didCancelDiscovery = false;
        if (mBluetoothAdapter.isEnabled() && mBluetoothAdapter.isDiscovering()){
            didCancelDiscovery = mBluetoothAdapter.cancelDiscovery();
        }
        return  didCancelDiscovery;
    }

    private void initializeLEScanCallback() {
            // Device scan callback for older API than Lollipop
            mLeDiscoveryScanCallback = new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    final String deviceAddress = device.getAddress();
                    final BLEDeviceData parsedDeviceData = parseAdvertisedData(scanRecord, deviceAddress);
                    dealWithFoundDevice(device, deviceAddress, parsedDeviceData);
                }
            };

    }

    private void dealWithFoundDevice(BluetoothDevice device, String deviceAddress, BLEDeviceData parsedDeviceData) {
        String deviceName = device.getName();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String parsedName = deviceName;
                if (parsedName == null) {
                    parsedName = parsedDeviceData.getData().getAsString(DB_KEY_DEV_NAME);
                }
                BLEDeviceType theDeviceType = parsedDeviceData.getDeviceType();
                boolean deviceNotDiscovered = !discoveredDevicesList.contains(device);
                boolean deviceInBikeActiveList = mBLEDeviceManager.isDeviceInActiveBikeList(deviceAddress);
                boolean deviceInOtherActiveList = mBLEDeviceManager.isDeviceInOtherActiveList(deviceAddress);
                boolean deviceAlreadyActive = mBLEDeviceManager.getDevAddressActiveStatus(deviceAddress) == 1;
                boolean deviceInDataBase = dataBaseAdapter.isDeviceInDataBase(deviceAddress);
                // if parseScanRecord returns UNKNOWN_DEVICE, add it to bleOtherDeviceList
                // if device is in bleActiveBikeDeviceList and name field is null or "" update parsed name
                // and update active status and refresh search-pair device lists if it isn't already active
                // if the device is not in discoveredDevicesList and DeviceType is BIKE_SPDCAD_DEVICE_OTHER
                // repeat Discovery
                // else if DeviceType is Bike Power or Heartrate, add it to DBActiveList with priority
                // else if DeviceType is BIKE_SPDCAD_DEVICE_OTHER and not already discovered,
                // do DeviceDiscovery to learn DeviceType
                if (theDeviceType == UNKNOWN_DEVICE && !deviceInOtherActiveList) {
                    mBLEDeviceManager.addToBLEOtherDeviceList(parsedDeviceData);
                    if (debugLEScan) { mBLEDeviceManager.logActiveOtherDeviceListData("other device found in LEScan"); }
                } else if (deviceInBikeActiveList) {
                    String nameField = mBLEDeviceManager.getDevAddressName(deviceAddress);
                    boolean nameFieldIsNull = (nameField == null || nameField.equals(""));
                    if (nameFieldIsNull) {
                        ContentValues newContent = new ContentValues();
                        newContent.put(DB_KEY_ACTIVE, 1);
                        newContent.put(DB_KEY_DEV_NAME, parsedName);
                        mBLEDeviceManager.updateActiveBikeDeviceData(deviceAddress, newContent);
                    }
                    if (!deviceAlreadyActive) {
                        mBLEDeviceManager.setDevAddressActiveStatus(deviceAddress, 1);
                        refreshDeviceLists();
                        if (debugBLEService) {
                            Log.wtf(logtag, "mLEScanCallback found device: " + deviceAddress
                                    + " " + parsedName + " deviceType: " + parsedDeviceData.getDeviceType().name());
                        }
                    }
                    // haven't seen this device yet
                } else if ((theDeviceType == HEARTRATE_DEVICE || theDeviceType == BIKE_POWER_DEVICE)) {
                    if (debugBLEService) {
                        Log.wtf(logtag, "mLEScanCallback found device: " + deviceAddress
                                + " " + parsedName + " deviceType: " + parsedDeviceData.getDeviceType().name());
                    }
                    ContentValues newContent = new ContentValues();
                    newContent.put(DB_KEY_ACTIVE, 1);
                    newContent.put(DB_KEY_DEV_TYPE, theDeviceType.intValue());
                    // this device wasn't in the bleActiveBikeDeviceList yet.
                    // Set Search priority to -1 unless it's in the database
                    int priority = (deviceInDataBase ?
                            dataBaseAdapter.fetchDeviceData(deviceAddress).getColumnIndexOrThrow(DB_KEY_SEARCH_PRIORITY) : -1);
                    newContent.put(DB_KEY_SEARCH_PRIORITY, priority);
                    BLEDeviceData theDeviceData = new BLEDeviceData(deviceAddress, theDeviceType, parsedName);
                    theDeviceData.setData(newContent);
                    theDeviceData.setDeviceType(theDeviceType);
                    mBLEDeviceManager.addToBLEBikeDeviceList(theDeviceData);
                    // we only do this branch the first time a device is found so do refreshDeviceLists()
                    // after this we'll catch the device in the deviceInBikeActiveList branch
                    refreshDeviceLists();
                } else if (theDeviceType == BIKE_SPDCAD_DEVICE_OTHER) {
                    // here's where we test CSC device for characteristic uuids before adding them to mLEDevices
                    if (deviceNotDiscovered) {
                        // this List keeps track of devices being Discovered so we won't repeat Discovery
                        Runnable deviceDiscoveryRunnable = new Runnable() {

                            @Override
                            public void run() {
                                deviceDiscovered = false;
                                if (debugLEScan){Log.i(logtag, "deviceDiscoveryRunnable for device: " + device.getAddress() + " , name: " + device.getName());}
                                if (mBLEDiscoveryService != null) {
                                    if (mBLEDiscoveryService.isServiceConnected()) {
                                        if (debugLEScan){Log.w(logtag, "deviceDiscoveryRunnable, Service already connected. Disconnecting.");}
                                        mBLEDiscoveryService.disconnect();
                                    }
                                    sensorWatchdogHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            boolean result = mBLEDiscoveryService.connect(device.getAddress());
                                            if (debugLEScan){Log.i(logtag, "deviceDiscoveryRunnable connect request result = " + (result ? INITIATED : FAILED));}
                                        }
                                    }, 50);
                                }
                                boolean timedOut = waitUntilDeviceDiscovered();
                                // do we have to account for situation where LEScan found a device, but it's not active when we get around to discovering services?
                                // If we put it in the BLEOtherDeviceList we'll never test it again.
                                // it's okay, because we'll always add devices we've found thru search/pair to our database and these will get added to ActiveDBList.
                                // If we've timed-out because we couldn't connect to device, add it to the bleOtherDeviceList
                                // If it's not in DBActiveList; no other data is needed
                                if (timedOut && !mBLEDeviceManager.isDeviceInActiveBikeList(device.getAddress())){
                                    mBLEDeviceManager.addToBLEOtherDeviceList(new BLEDeviceData(device.getAddress(), UNKNOWN_DEVICE));
                                    mBLEDeviceManager.logActiveOtherDeviceListData("timed-out in Device Discovery");
                                }
                            }

                            private boolean waitUntilDeviceDiscovered() {
                                long startTime = System.currentTimeMillis();
                                long elapsedTime;
                                // deviceDiscovered will be set true in GATTDiscoveryReceiver: ACTION_GATT_SERVICES_DISCOVERED or ACTION_DATA_AVAILABLE
                                do {
                                    elapsedTime = System.currentTimeMillis() - startTime;
                                } while (!deviceDiscovered && elapsedTime < DEVICE_DISCOVERY_TIME);
                                if (debugLEScan) {
                                    Log.w(logtag, "waitUntilDeviceDiscovered: timeout? " + (elapsedTime >= DEVICE_DISCOVERY_TIME ? "yes" : "no")
                                            + " deviceDiscovered? " + (deviceDiscovered ? "yes" : "no"));
                                }
                                // now remove device from discoveredDevicesList
                                discoveredDevicesList.clear();
                                // now disconnect from GATT device, unless we've already left MainActivity for another task and Service has been disconnected
                                if (mBLEDiscoveryService != null) {
                                    mBLEDiscoveryService.disconnect();
                                }
                                return elapsedTime >= DEVICE_DISCOVERY_TIME;
                            }
                        };
                        discoveredDevicesList.add(device);
                        new ThreadPerTaskExecutor().execute(deviceDiscoveryRunnable);
                    }
                }
            }
        });
    }

    private void refreshDeviceLists() {
        if (debugBLESearchPair) Log.i(logtag, "refreshDeviceLists()");
        if (mBLEDeviceManager.bleActiveBikeDeviceList.size() < 1){return;}
        for (BLEDeviceData deviceData : mBLEDeviceManager.bleActiveBikeDeviceList) {
            //imageLevel refers to a green or red dB icon (IS_INDB:NOT_INDB)
            //in image_icon.xml level-list; also use this in the file chooser
            String imageLevel = ((deviceData.getData().getAsInteger(DB_KEY_SEARCH_PRIORITY) > 0)?IS_INDB:NOT_INDB);
            String deviceAddress = deviceData.getAddress();
            String deviceName = deviceData.getData().getAsString(DB_KEY_DEV_NAME);
            if (deviceName == null || ("").equals(deviceName)) {
                deviceName = "<" + deviceAddress + ">";
            }
            HashMap<String, String> map = new HashMap<>();
            map.put(KEY_SP_DEVICE_NAME, deviceName);
            map.put(KEY_SP_INDB_ICON, imageLevel);
            map.put(DB_KEY_DEV_ADDRESS, deviceAddress);
            map.put(DB_KEY_DEV_TYPE, String.valueOf(deviceData.getDeviceType().intValue()));
            //all devices in the bleActiveBikeDeviceList may not be active
            boolean deviceIsActive = deviceData.getData().getAsInteger(DB_KEY_ACTIVE) == 1;
            BLEDeviceType deviceType = deviceData.getDeviceType();
            switch (deviceType){
                case BIKE_POWER_DEVICE:
                    if (!isDeviceInSPList(map, powerDeviceSPList) && deviceIsActive) {
                        powerDeviceSPList.add(map);
                    }
                    break;
                case BIKE_CADENCE_DEVICE:
                    if (!isDeviceInSPList(map, cadenceDeviceSPList) && deviceIsActive) {
                        cadenceDeviceSPList.add(map);
                    }
                    break;
                case HEARTRATE_DEVICE:
                    if (!isDeviceInSPList(map, hrmDeviceSPList) && deviceIsActive) {
                        hrmDeviceSPList.add(map);
                    }
                    break;
                case BIKE_SPD_DEVICE:
                    if (!isDeviceInSPList(map, speedDeviceSPList) && deviceIsActive) {
                        speedDeviceSPList.add(map);
                    }
                    break;
                case BIKE_SPDCAD_DEVICE:
                    if (!isDeviceInSPList(map, spdcadDeviceSPList) && deviceIsActive) {
                        spdcadDeviceSPList.add(map);
                    }
                    if (!isDeviceInSPList(map, cadenceDeviceSPList) && deviceIsActive) {
                        cadenceDeviceSPList.add(map);
                    }
                    if (!isDeviceInSPList(map, speedDeviceSPList) && deviceIsActive) {
                        speedDeviceSPList.add(map);
                    }
                    break;
                default:
            }
        }
        // now have to update the searchPairNamesList adapter with new information
        switch (searchPairDeviceType){
            case BIKE_POWER_DEVICE:
                searchPairNamesList = powerDeviceSPList;
                break;
            case BIKE_CADENCE_DEVICE:
                searchPairNamesList = cadenceDeviceSPList;
                break;
            case HEARTRATE_DEVICE:
                searchPairNamesList = hrmDeviceSPList;
                break;
            case BIKE_SPD_DEVICE:
                searchPairNamesList = speedDeviceSPList;
                break;
            case BIKE_SPDCAD_DEVICE:
                searchPairNamesList = spdcadDeviceSPList;
                break;
            default:
                break;

        }
        if (debugBLESearchPair) Log.v(logtag, "refreshDeviceLists() - searchPairNamesList.size() " + searchPairNamesList.size());
        mSPAdapter = new SearchPairAdapter(MainActivity.this, searchPairNamesList);
        spListView.setAdapter(mSPAdapter);
        if (debugBLESearchPair) logDeviceLists();
        mSPAdapter.notifyDataSetChanged();
    }

    private void logDeviceLists() {
        mBLEDeviceManager.logActiveBikeDeviceListData("after refreshDeviceLists() in LEScan callback");
        Log.i(logtag, "active Power Devices:");
        for (HashMap hmap:powerDeviceSPList){
            Log.i(logtag, "active Power Device: " + hmap.get(KEY_SP_DEVICE_NAME));
        }
        Log.i(logtag, "active HRM Devices:");
        for (HashMap hmap:hrmDeviceSPList){
            Log.i(logtag, "active HRM Device: " + hmap.get(KEY_SP_DEVICE_NAME));
        }
        Log.i(logtag, "active cad Devices:");
        for (HashMap hmap:cadenceDeviceSPList){
            Log.i(logtag, "active cadence Device: " + hmap.get(KEY_SP_DEVICE_NAME));
        }
        Log.i(logtag, "active Speed Devices:");
        for (HashMap hmap:speedDeviceSPList){
            Log.i(logtag, "active Speed Device: " + hmap.get(KEY_SP_DEVICE_NAME));
        }
        Log.i(logtag, "active spdcad Devices:");
        for (HashMap hmap:spdcadDeviceSPList){
            Log.i(logtag, "active spdcad Device: " + hmap.get(KEY_SP_DEVICE_NAME));
        }
        Log.v(logtag, "Devices in searchPairNamesList:");
        for (HashMap hmap:searchPairNamesList){
            Log.i(logtag, "active Device: " + hmap.get(KEY_SP_DEVICE_NAME));
        }
    }

     @TargetApi(Build.VERSION_CODES.M)
    public void doShowBatterySettings() {
        Intent myIntent = new Intent();
        myIntent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        startActivity(myIntent);
    }

	private TimerTask testSensorData;
	private final Handler sensorWatchdogHandler = new Handler();
	private final Timer sensorWatchdogTimer = new Timer();

    /**
     * A watchdog timer to check if sensor data is current; also detect faulty PowerTap & speed sensor
     * Test crank length and find distributed power sensors. Check location is current and check permissions.
     */
    private void startSensorWatchdog() {
        testSensorData = new TimerTask() {
            @Override
            public void run() {

                sensorWatchdogHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        checkBTLEScanning();
                        // check if Bluetooth is on and show snackbar that can't connect to sensors without Bluetooth,
                        if (!isBluetoothOn(getApplicationContext()) && !mResolvingBluetoothMode) {
                            requestBluetoothModeOn();
                        }
                        checkFusedLocationService();
                        checkDataIsCurrent();
                        checkCrankLength();
                        // if PowerTap battery weak, data still reported, but values are zero
                        detectFaultyPowerTap();
                        // detect odd SpeedSensor values; Garmin accelerometer sometimes reports low values
                        detectFaultyAccelSpeedSensor();
                        // if no GPS locations, can check if we're paused using wheel and power wheel sensors
                        testZeroPaused();
                        // have to test zero paused first before deciding to enable or disable BLE scanner
                        enableDisableBTLEScanner();
                        // allow screen to turn-off if paused; do this after testing Paused state.
                        setScreenDim();
                        checkPermissions();
                        checkLocCurrent();
                        writeAppMessage("", textColorWhite);
                        // check if speed sensor is calibrated
                        calWheel();
                        calPowerWheel();
                    }

                });// sensorWatchdog Runnable
            }

            private void checkFusedLocationService() {
                if (askLocationPermission()) {
                    if (!Utilities.requestingLocationUpdates(context) && mLocationServiceBound) {
                        mLocationService.requestLocationUpdates();
                    }
                    if (debugLocation) {
                        Log.i(logtag, "checkFusedLocationService()"
                                + (Utilities.requestingLocationUpdates(context) ? " are already requesting" : " not requesting"));
                    }
                }
            }

            private void enableDisableBTLEScanner(){
                // we only want to scan for three minutes after starting up from a Pause
                allowBLEScanner = SystemClock.elapsedRealtime() - myBikeStat.getPausedClock() < THREE_MINUTES;
                //if (debugLEScan) {Log.w(logtag, "enableDisableBTLEScanner-allowBLEScanner: " + (allowBLEScanner?"yes":"no"));}
            }
            private void checkBTLEScanning() {
                // if we've already checked, or we haven't waited 3 minutes since we've first acquired a Location, return
                // if we have discovered any BT LE device, return
                boolean hadLocationsFor3Minutes = System.currentTimeMillis() - myBikeStat.getFirstLocSysTimeStamp() >= THREE_MINUTES;
                if (!Utilities.hasBLE(context) || alreadyCheckedBTLEScanning || !hadLocationsFor3Minutes) {
                    return;
                }
                alreadyCheckedBTLEScanning = true;
                boolean haveOtherBTLEDevices = mBLEDeviceManager.bleOtherDeviceList.size() > 0;
                boolean haveBikeBTLEDevices = mBLEDeviceManager.isAnyBikeDeviceActive();
                if (debugLEScan) {Log.w(logtag, "haveOtherBTLEDevices: " + (haveOtherBTLEDevices?"yes":"no")
                        + " haveBikeBTLEDevices: " + (haveBikeBTLEDevices?"yes":"no"));}
                if (haveOtherBTLEDevices || haveBikeBTLEDevices) {
                    // finding BTLE devices, scanning is okay
                    return;
                }
                // try to fix scanner by turning off, then turning on Bluetooth, re-initializing BT Adapter, re-initializing Scanner
                if (mBluetoothAdapter != null) {
                    mBluetoothAdapter.disable();
                    mBluetoothAdapter = null;
                    // we only enable BT by asking permission during sensor Watchdog Timer
                }
                // BT Adapter was null, try to get it again
                final BluetoothManager bluetoothManager =
                        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                if (bluetoothManager != null) {
                    mBluetoothAdapter = bluetoothManager.getAdapter();
                }
                loadBLEConfiguration();
                initializeLEScanCallback();
            }

            private void checkCrankLength() {
                // If we are able to read crankLength and should read or write - from each side of a distributed system
                // canRead is set in mGattPowerReceiver when we receive power data, meaning that POWER_CONTROL_POINT is accessible
                // shouldRead is set true initially, and set false once we've read
                // shouldWrite is tested when we've read in mGatPowerReceiver and when prefsChanged
                if (debugBLEPowerCrank) {Log.i(logtag, "canReadCrankLength: " + (powerDeviceData.canReadCrankLength?"yes":"no")
                         + " shouldReadCrankLength: " + (powerDeviceData.shouldReadCrankLength?"yes":"no")
                        + " shouldWriteCrankLength: " + (powerDeviceData.shouldWriteCrankLength?"yes":"no"));}
                if (debugBLEPowerCrank) {Log.i(logtag, "canReadOppositeCrankLength: " + (oppositePowerDeviceData.canReadCrankLength?"yes":"no")
                        + " shouldReadOppositeCrankLength: " + (oppositePowerDeviceData.shouldReadCrankLength?"yes":"no")
                        + " shouldWriteOppositeCrankLength: " + (oppositePowerDeviceData.shouldWriteCrankLength?"yes":"no"));}
                if (powerDeviceData.canReadCrankLength
                        && powerDeviceData.shouldReadCrankLength
                        && powerDeviceData.status == BLEDeviceStatus.TRACKING) {
                    boolean submitted = readCrankLength(powerDeviceData);
                    if (debugBLEPowerCrank) {Log.wtf(logtag, "requested crank length: " + (submitted?"yes":"no"));}
                }
                if (powerDeviceData.canReadCrankLength
                        && powerDeviceData.shouldWriteCrankLength
                        && powerDeviceData.status == BLEDeviceStatus.TRACKING) {
                    boolean submitted = writeCrankLength(powerDeviceData);
                    if (debugBLEPowerCrank) {Log.i(logtag, "write crank length: " + (submitted?"yes":"no"));}
                }
                if (oppositePowerDeviceData.canReadCrankLength
                        && oppositePowerDeviceData.shouldReadCrankLength
                        && oppositePowerDeviceData.status == BLEDeviceStatus.TRACKING) {
                    boolean submitted = readOppositeCrankLength(oppositePowerDeviceData);
                    if (debugBLEPowerCrank) {Log.wtf(logtag, "requested opposite crank length: " + (submitted?"yes":"no"));}
                }
                if (oppositePowerDeviceData.canReadCrankLength
                        && oppositePowerDeviceData.shouldWriteCrankLength
                        && oppositePowerDeviceData.status == BLEDeviceStatus.TRACKING) {
                    boolean submitted = writeOppositeCrankLength(oppositePowerDeviceData);
                    if (debugBLEPowerCrank) {Log.i(logtag, "write opposite crank length: " + (submitted?"yes":"no"));}
                }
            }

            private boolean writeOppositeCrankLength(BLEDeviceData powerDeviceData) {
                boolean submitted;
                int offset = 0;
                BluetoothGattCharacteristic pcpCharacteristic = powerDeviceData.getPowerControlPtCharacteristic();
                pcpCharacteristic.setValue(WRITE_CRANK_LENGTH_OPCODE, FORMAT_UINT8, offset);
                offset = 1;
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                double mCrankLength = Double.valueOf(settings.getString(CRANK_LENGTH, String.valueOf(DEFAULT_CRANK_LENGTH)));
                int newCrankLength = (int) (2 * mCrankLength);
                submitted = pcpCharacteristic.setValue(newCrankLength, FORMAT_UINT16, offset);
                //write the descriptor to power meter; we'll write characteristic when onWriteDescriptor called
                mBLEOppositePowerService.setCharacteristicIndication(pcpCharacteristic);
                return submitted;
            }

            private boolean readOppositeCrankLength(BLEDeviceData powerDeviceData) {
                //send BIKE_POWER_CONTROL_POINT message through BLEOppositePowerService
                boolean submitted;
                int offset = 0;
                BluetoothGattCharacteristic pcpCharacteristic = powerDeviceData.getPowerControlPtCharacteristic();
                submitted = pcpCharacteristic.setValue(REQUEST_CRANK_LENGTH_OPCODE, FORMAT_UINT8, offset);
                // write the descriptor to power sensor; we'll write characteristic when onWriteDescriptor called
                mBLEOppositePowerService.setCharacteristicIndication(pcpCharacteristic);
                return submitted;
            }

            private boolean writeCrankLength(BLEDeviceData powerDeviceData) {
                boolean submitted;
                int offset = 0;
                BluetoothGattCharacteristic pcpCharacteristic = powerDeviceData.getPowerControlPtCharacteristic();
                pcpCharacteristic.setValue(WRITE_CRANK_LENGTH_OPCODE, FORMAT_UINT8, offset);
                offset = 1;
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                double mCrankLength = Double.valueOf(settings.getString(CRANK_LENGTH, String.valueOf(DEFAULT_CRANK_LENGTH)));
                int newCrankLength = (int) (2 * mCrankLength);
                submitted = pcpCharacteristic.setValue(newCrankLength, FORMAT_UINT16, offset);
                // write the descriptor to power sensor; we'll write characteristic when onWriteDescriptor called
                mBLEPowerService.setCharacteristicIndication(pcpCharacteristic);
                return submitted;
            }

            private boolean readCrankLength(BLEDeviceData powerDeviceData) {
                //send BIKE_POWER_CONTROL_POINT message through BLEPowerService
                boolean submitted;
                int offset = 0;
                BluetoothGattCharacteristic pcpCharacteristic = powerDeviceData.getPowerControlPtCharacteristic();
                submitted = pcpCharacteristic.setValue(REQUEST_CRANK_LENGTH_OPCODE, FORMAT_UINT8, offset);
                //write the descriptor to power sensor; we'll write characteristic when onWriteDescriptor called
                mBLEPowerService.setCharacteristicIndication(pcpCharacteristic);
                return submitted;
            }

             private void showPairingSnackbar() {
                if (mResolvingPowerPairing){
                    return;
                }
                mResolvingPowerPairing = true;
                mRequestPowerPairingSnackBar = Snackbar.make(
                        myCoordinatorLayout,
                        getString(R.string.reqPowerPairing),
                        Snackbar.LENGTH_LONG);
                mRequestPowerPairingSnackBar.setAction(R.string.ok, new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                    }
                }).show();
            }

            private void showDistributedPowerInactiveSnackbar() {
                if (mWarnedPowerInactive){
                    return;
                }
                mWarnedPowerInactive = true;
                mWarnPowerInactiveSnackBar= Snackbar.make(
                        myCoordinatorLayout,
                        getString(R.string.warnPowerInactive),
                        Snackbar.LENGTH_LONG);
                mWarnPowerInactiveSnackBar.setAction(R.string.ok, new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                    }
                }).show();
            }

            private void checkPermissions() {
                // do we have Location Permission? If not, ask for location permission
                // if so, and location is current, and we don't have write permission, ask for Write permission
                boolean isLocationCurrent = isFusedLocationCurrent() || isGPSLocationCurrent();
                if (askLocationPermission() && isLocationCurrent) {
                    askWritePermission();
                }
            }

            private void askWritePermission() {
                if (Utilities.hasStoragePermission(getApplicationContext())) { return; }
                // Should we show an explanation? Check box "don't show again" override.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE);
                }
                writeAppMessage(getString(R.string.write_permission_denied),
                        ContextCompat.getColor(context, R.color.gpsred));
            }

            private void checkLocCurrent() {
                // used to indicate loss of GPS location data in
                // the display speed will read XX.x, distance to way points will show ??
                if (trainerMode) {
                    return;
                }
                if (debugLocation){Log.i(logtag, "System time: " + System.currentTimeMillis()
                        + " BikeStat.newGPSSysTimeStamp: " + myBikeStat.newGPSLocSysTimeStamp
                        + " Sensor Watchdog - GPS location Current?: " + (isGPSLocationCurrent()?"yes":"no"));
                    Log.i(logtag, "System time: " + System.currentTimeMillis()
                            + " BikeStat.newFusedSysTimeStamp: " + myBikeStat.newFusedLocSysTimeStamp
                            + " Sensor Watchdog - Fused location Current?: " + (isFusedLocationCurrent()?"yes":"no"));}
                if (!isGPSLocationCurrent()) {
                    int event = isGPSLocationCurrent()
                            || isFusedLocationCurrent()?GpsStatus.GPS_EVENT_STARTED:GpsStatus.GPS_EVENT_STOPPED;
                    updateGPSStatusDisplay(event, false, context, findViewById(R.id.gps_status_button));
                    myBikeStat.gpsSpeedCurrent = false;
                    refreshScreen();
                    refreshSpeed();
                    // if location not current some time during wheel cal, start calibration over again
                    // so set startDist to current trip distance, zero-out wheel total counts
                    if (!mBLEDeviceManager.wheelCnts.isCalibrated) {
                        mBLEDeviceManager.restartWheelCal(myBikeStat.getWheelTripDistance());
                    }
                    if (!mBLEDeviceManager.powerWheelCnts.isCalibrated) {
                        mBLEDeviceManager.restartPowerWheelCal(myBikeStat.getWheelTripDistance());
                    }
                    if (!isGPSLocationEnabled(getApplicationContext())
                            && !trainerMode
                            && !mLocationSettingsSnackBar.isShown()){
                        mLocationSettingsSnackBar.setAction(getString(R.string.enable), new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                Intent viewIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivityForResult(viewIntent, REQUEST_CHANGE_LOCATION_SETTINGS);
                            }
                        }).show();
                    }
                    // Location Settings may have been disabled, then user fixed after showing SnackBar
                } else if (mLocationSettingsSnackBar.isShown()){
                    mLocationSettingsSnackBar.dismiss();
                }
            }// checkLocCurrent()

            void calWheel() {
                if (mBLEDeviceManager.wheelCnts.isCalibrated
                        || speedDeviceData == null
                        || (speedDeviceData.status != BLEDeviceStatus.TRACKING)
                        || !mBLEDeviceManager.wheelCnts.isDataCurrent
                        || (mBLEDeviceManager.wheelCnts.calTotalCount == 0)
                        || (myBikeStat.getGPSTripDistance() - mBLEDeviceManager.wheelCnts.calGPSStartDist) < MIN_CAL_DIST
                        || trainerMode) {
                    return;
                }
                // the startDist accounts for non-zero distance
                // when the Speed channel is opened and accumulating wheel counts
                double wheelCircum = (myBikeStat
                        .getGPSTripDistance() - mBLEDeviceManager.wheelCnts.calGPSStartDist)
                        / mBLEDeviceManager.wheelCnts.calTotalCount;
                // handle wheelCircum out of limits = calib. failure
                // should be 2.140 for 25 mm x 700c wheels
                // start over
                if ((wheelCircum > UPPER_WHEEL_CIRCUM) || (wheelCircum < LOWER_WHEEL_CIRCUM)) {
                    mBLEDeviceManager.restartWheelCal(myBikeStat.getWheelTripDistance());
                    return;
                }
                mBLEDeviceManager.wheelCnts.wheelCircumference = wheelCircum;
                mBLEDeviceManager.wheelCnts.isCalibrated = true;
                myBikeStat.hasCalSpeedSensor = true;
                // force refreshTitles() to show "cal"
                refreshTitles();
            }// calWheel()

            void calPowerWheel() {
                if (mBLEDeviceManager.powerWheelCnts.isCalibrated
                        || powerDeviceData == null
                        || (powerDeviceData.status != BLEDeviceStatus.TRACKING)
                        || !mBLEDeviceManager.powerWheelCnts.isDataCurrent
                        || (mBLEDeviceManager.powerWheelCnts.calTotalCount == 0)
                        || (myBikeStat.getGPSTripDistance() - mBLEDeviceManager.powerWheelCnts.calGPSStartDist) < MIN_CAL_DIST
                        || trainerMode) {
                    return;
                }
                // the startDist accounts for non-zero distance
                // when the power channel is opened and accumulating wheel counts
                double wheelCircum = (myBikeStat.getGPSTripDistance()
                        - mBLEDeviceManager.powerWheelCnts.calGPSStartDist)
                        / mBLEDeviceManager.powerWheelCnts.calTotalCount;
                // handle wheelCircum out of limits = calib. failure
                // should be 2.160 for 25 mm x 700c wheels start over
                if ((wheelCircum > UPPER_WHEEL_CIRCUM) || (wheelCircum < LOWER_WHEEL_CIRCUM)) {
                    mBLEDeviceManager.restartPowerWheelCal(myBikeStat.getWheelTripDistance());
                    return;
                }
                mBLEDeviceManager.powerWheelCnts.wheelCircumference = wheelCircum;
                mBLEDeviceManager.powerWheelCnts.isCalibrated = true;
                myBikeStat.hasCalPowerSpeedSensor = true;
                // force refreshTitles() to show "cal"
                refreshTitles();
            }// calPowerWheel()

            /**
             * If Garmin speed sensor battery weak it sends out faulty "data" where speed is too low.
             * This can cause a 'paused' condition, which requires a calibrated speed sensor.
             * Check for low speed values against GPS; if different, set speed sensor to "uncalibrated"
             */
            private void detectFaultyAccelSpeedSensor() {
                // do nothing if there is no calibrated speed sensor
                if (!myBikeStat.hasCalSpeedSensor) { return; }
                // current, gps speed > 5mph and current calibrated speed sensor < .5
                boolean faultCondition = isGPSLocationCurrent()
                        && (myBikeStat.getGpsSpeed() > faultSpeed)
                        && mBLEDeviceManager.wheelCnts.isDataCurrent
                        && (myBikeStat.getSensorSpeed() < .5);
                if (faultCondition) {
                    myBikeStat.hasCalSpeedSensor = false;
                    mBLEDeviceManager.wheelCnts.isCalibrated = false;
                }
            }

            /**
             * If PowerTap battery weak it sends out faulty "data" where speed and power are zero
             * or speed is much different than gpsSpeed.
             * This can cause a 'paused' condition if the PowerTap is calibrated.
             * The paused condition requires a calibrated PowerTap. Check for zero PowerTap values
             * against GPS or SpeedSensor values; if different, set PowerTap to "uncalibrated"
             */
            private void detectFaultyPowerTap() {
                if (!myBikeStat.hasPowerSpeedSensor) { return; }
                boolean powerSpeedDifferent = Math.abs((myBikeStat.getGpsSpeed() - myBikeStat.getPowerSpeed())
                        / myBikeStat.getGpsSpeed()) > 0.5;
                // current GPS location, speed > 5mph, and current powerSpeed < .1
                boolean faultCondition = isGPSLocationCurrent()
                        && (myBikeStat.getGpsSpeed() > faultSpeed)
                        && powerSpeedDifferent
                        && mBLEDeviceManager.powerWheelCnts.isDataCurrent;
                if (faultCondition) {
                    myBikeStat.hasCalPowerSpeedSensor = false;
                    mBLEDeviceManager.powerWheelCnts.isCalibrated = false;
                }
            }

            private void checkDataIsCurrent() {
                // two functions 1) some sensors send the last data value if no new data is available
                // - set cadence, speed, power to zero if no new data for three seconds
                // 2) Bluetooth sensors stop sending data after two minutes of inactivity, but Service does not disconnect automatically.
                // Want to wait until a device has been connected, thus the .initialized test
                long currentTime = SystemClock.elapsedRealtime();
                // ert put in .currTime in RawSpeed, calculatedSpeed, calcPower, rawPower, and HR
                mBLEDeviceManager.hrData.isDataCurrent = ((currentTime - mBLEDeviceManager.hrData.ertTimeStamp) < THREE_SEC);
                if (!mBLEDeviceManager.hrData.isDataCurrent) {
                    refreshHR();
                }// HR !current
                // many Bluetooth sensors stop transmitting after 2-3 minutes; need to disconnect the Service
                if (currentTime - mBLEDeviceManager.hrData.ertTimeStamp > THREE_MINUTES
                        && mBLEDeviceManager.hrData.initialized){
                    //reset the ertTimeStamp so we don't keep disconnecting Service
                    mBLEDeviceManager.hrData.ertTimeStamp = currentTime;
                    if (debugBLEData){Log.w(logtag, "disconnecting HRM Service, no data for three minutes");}
                    mBLEHRMService.disconnect();
                }
                mBLEDeviceManager.wheelCnts.isDataCurrent = (currentTime - mBLEDeviceManager.wheelCnts.ertTimeStamp) < THREE_SEC;
                myBikeStat.setSensorSpeedCurrent(mBLEDeviceManager.wheelCnts.isDataCurrent);
                mBLEDeviceManager.wheelCnts.isDataNew = (currentTime - mBLEDeviceManager.wheelCnts.ertTimeStampNewData) < THREE_SEC;
                if (!mBLEDeviceManager.wheelCnts.isDataNew){
                    myBikeStat.setSensorSpeed(0.);
                    refreshSpeed();
                }

                if (currentTime - mBLEDeviceManager.wheelCnts.ertTimeStamp > THREE_MINUTES
                        && mBLEDeviceManager.wheelCnts.initialized){
                    //reset the ertTimeStamp so we don't keep disconnecting Service
                    mBLEDeviceManager.wheelCnts.ertTimeStamp = currentTime;
                    if (debugBLEData){Log.w(logtag, "disconnecting Speed Service, no data for three minutes");}
                    mBLESpeedService.disconnect();
                }

                mBLEDeviceManager.pedalCadenceCnts.isDataCurrent = ((currentTime - mBLEDeviceManager.pedalCadenceCnts.ertTimeStamp) < THREE_SEC);
                mBLEDeviceManager.pedalCadenceCnts.isDataNew = ((currentTime - mBLEDeviceManager.pedalCadenceCnts.ertTimeStampNewData) < THREE_SEC);
                // set cadence display to 0 if data not new; that is we didn't receive any new revolutions
                if (!mBLEDeviceManager.pedalCadenceCnts.isDataNew) {
                    myBikeStat.setPedalCadence(0);
                    refreshCadence();
                }// cadence !current
                if (currentTime - mBLEDeviceManager.pedalCadenceCnts.ertTimeStamp > THREE_MINUTES
                        && mBLEDeviceManager.pedalCadenceCnts.initialized){
                    //reset the ertTimeStamp so we don't keep disconnecting Service
                    mBLEDeviceManager.pedalCadenceCnts.ertTimeStamp = currentTime;
                    if (debugBLEData){Log.w(logtag, "disconnecting Cadence Service, no data for three minutes");}
                    mBLECadenceService.disconnect();
                }

                mBLEDeviceManager.calcPowerData.isDataNew =
                        ((currentTime - mBLEDeviceManager.calcPowerData.ertTimeStampNewData) < THREE_SEC);
                if (!mBLEDeviceManager.calcPowerData.isDataNew) {
                    // Stages keeps sending the last power value when coasting; Set power to 0
                    // if last value is > 3 seconds old.
                    myBikeStat.setPower(0);
                    myBikeStat.setPrevPower(0);
                    refreshPower();
                }// calcPowerData !current
                if (currentTime - mBLEDeviceManager.calcPowerData.ertTimeStamp > THREE_MINUTES
                        && mBLEDeviceManager.calcPowerData.initialized){
                    //reset the ertTimeStamp so we don't keep disconnecting Service
                    mBLEDeviceManager.calcPowerData.ertTimeStamp = currentTime;
                    if (debugBLEData){Log.w(logtag, "disconnecting Power Service, no data for three minutes");}
                    mBLEPowerService.disconnect();
                }

                mBLEDeviceManager.crankCadenceCnts.isDataNew =
                        ((currentTime - mBLEDeviceManager.crankCadenceCnts.ertTimeStampNewData) < THREE_SEC);
                // Stages keeps sending the last cadence value when coasting;
                // Set cadence to 0 if last value is > 3 seconds old
                if (!mBLEDeviceManager.crankCadenceCnts.isDataNew && myBikeStat.hasPowerCadence) {
                    myBikeStat.setPowerCadence(0);
                    refreshCadence();
                }// powerCadence !current
                //ertTimeStamp is set when we receive any data from the sensor
                mBLEDeviceManager.powerWheelCnts.isDataCurrent = (currentTime - mBLEDeviceManager.powerWheelCnts.ertTimeStamp) < THREE_SEC;
                myBikeStat.setPowerSpeedCurrent(mBLEDeviceManager.powerWheelCnts.isDataCurrent);
                mBLEDeviceManager.powerWheelCnts.isDataNew = (currentTime - mBLEDeviceManager.powerWheelCnts.ertTimeStampNewData) < THREE_SEC;
                // alert BikeStatRow if data not new; this is used only to set speed to 0 since we are not getting any revolutions
                if (!mBLEDeviceManager.powerWheelCnts.isDataNew) {
                    // Power wheel stopped turning, set power sensor speed to 0.
                    // When gps decides we've paused, the speed display will go to zero
                    myBikeStat.setPowerSpeed(0.);
                    // this is okay, only called rarely
                    refreshSpeed();
                }// powerWheel speed !current
            }
        };// TimerTask()
		sensorWatchdogTimer.schedule(testSensorData, ONE_SEC, THREE_SEC);
	}// startSensorWatchdog()

    private boolean isGPSLocationCurrent() {
        return (System.currentTimeMillis() - myBikeStat.newGPSLocSysTimeStamp) < TEN_SEC;
    }

    private boolean isFusedLocationCurrent() {
        return (System.currentTimeMillis() - myBikeStat.newFusedLocSysTimeStamp) < TEN_SEC;
    }

    private void testBatteryOptimization() {
        // test for battery saver and show dialog to turn off
        if (Utilities.isBatterySaverActive(getApplicationContext()) && !testedBatterySaver){
            testedBatterySaver = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                writeAppMessage("testing battery saver", textColorWhite);
                Bundle dialogBundle = new Bundle();
                dialogBundle.putCharSequence(DDF_KEY_TITLE, getString(R.string.bat_optimization_title));
                dialogBundle.putCharSequence(DDF_KEY_MESSAGE, getString(R.string.bat_optimization));
                MADeviceDialogFragment newFragment = MADeviceDialogFragment.newInstance(dialogBundle);
                newFragment.show(getFragmentManager(), "MainbatteryOptimizationSettings");
            }
        }
    }

    private void stopSensorWatchdog() {
        if (debugAppState) Log.i(logtag, "stopping sensor Watchdog");
		sensorWatchdogHandler.removeCallbacksAndMessages(null);
		if (testSensorData != null) {
			testSensorData.cancel();
		}
	}

    private boolean askLocationPermission() {
        if (Utilities.hasFineLocationPermission(getApplicationContext())) {
            if (debugAppState) Log.i(logtag, "ask Location permission: has location permission");
            return true;
        } else {
            // Request permission
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
            writeAppMessage(getString(R.string.loc_permission_denied),
                    ContextCompat.getColor(context, R.color.gpsred));
            return false;
        }
    }

	@Override
	public void onRequestPermissionsResult(int requestCode,
			@NonNull String[] permissions,
			@NonNull int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_LOCATION: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    writeAppMessage("", textColorWhite);
                    mLocationService.requestLocationUpdates();
				} else {
                    // we'll ask again in Location Watchdog
					writeAppMessage(getString(R.string.loc_permission_denied), ContextCompat.getColor(context, R.color.gpsred));
				}
                break;
			}
			case MY_PERMISSIONS_REQUEST_WRITE: {
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    writeAppMessage("", textColorWhite);
                } else {
					writeAppMessage(getString(R.string.write_permission_denied), ContextCompat.getColor(context, R.color.gpsred));
				}
				break;
			}
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
				// other 'case' lines to check for other permissions this app might request
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        prefChanged = true;
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if (requestCode == RC_SHOW_FILE_LIST) {
            if (resultCode == RESULT_OK) {
                int chooserCode = 0;
                Bundle chooserCodeBundle = intent.getExtras();
                if (chooserCodeBundle != null) {
                    chooserCode = chooserCodeBundle.getInt(KEY_CHOOSER_CODE);
                }
                switch (chooserCode) {
                        case CHOOSER_TYPE_GPX_DIRECTORY:
                        // intent -> start chooser activity
                        Intent loadFileIntent = new Intent(this, ShowFileList.class);
                        //indicate the chooser type is choosing gpx file
                        loadFileIntent.putExtra(CHOOSER_TYPE, ROUTE_FILE_TYPE);
                        loadFileIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivityForResult(loadFileIntent, RC_SHOW_FILE_LIST);
                        break;
                    case CHOOSER_TYPE_GPX_FILE:
                        prevChosenFile = chosenGPXFile;
                        chosenGPXFile = settings.getString(KEY_CHOSEN_GPXFILE, "");
                        if (!chosenGPXFile.equals("")) {
                            myNavRoute.mChosenFile = new File(chosenGPXFile);
                            //refresh the screen to indicate we've moved out of Chooser
                            refreshScreen();
                            //we're not trying to restore the route and force a new tcx file
                            resumingRoute = false;
                            //load file in async task with progress bar
                            new LoadData().execute();
                        }
                        break;
                    case CHOOSER_TYPE_TCX_DIRECTORY:
                        // We don't actually let user choose a different directory when searching for an activity file.
                        // If we did, this is how we would go back to the chooser
                        Intent loadFileIntent1 = new Intent(this, ShowFileList.class);
                        //indicate the chooser type is choosing tcx file
                        loadFileIntent1.putExtra(CHOOSER_TYPE, ACTIVITY_FILE_TYPE);
                        loadFileIntent1.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivityForResult(loadFileIntent1, RC_SHOW_FILE_LIST);
                        break;
                    case CHOOSER_TYPE_TCX_FILE:
                        String mChosenTCXFile = settings.getString(KEY_CHOSEN_TCXFILE, "");
                        String sharingFileName = mChosenTCXFile;
                        if (readActivityFileType() == Integer.valueOf(FIT_ACTIVITY_TYPE)) {
                            // Replace the suffix to indicate a fit file instead of a tcx file
                            sharingFileName = myBikeStat.fitLog.delTCXFITSuffix(mChosenTCXFile) + ".fit";
                        }
                        // save the sharingFileName in SharedPrefs so we can access it in the ThhreadPerTask executor
                        settings.edit().putString(SHARING_FILENAME, sharingFileName).apply();
                        String sharingName_noPath = myBikeStat.fitLog.delTCXFITSuffix(myBikeStat.fitLog.stripFilePath(sharingFileName));
                        if (mustCloseFit(myBikeStat.tcxLog.outFileName, sharingName_noPath)) {
                            // Sharing the current log files, close the activity files before uploading them
                            // Give CFFB the sharing filename so it can pass it on to UploadFileSend when finished closing
                            new CloseFitFileBackground().execute(sharingFileName);
                            //SerialExecutor closeFitExecutor = new SerialExecutor();
                            //closeFitExecutor.execute(closeFitFileRunnable);
                            //closeFitExecutor.execute(uploadFileSendRunnable);
                        } else {
                            //new ThreadPerTaskExecutor().execute(uploadFileSendRunnable);
                            uploadFileSend(sharingFileName);
                        }
                        break;
                    default:
                        break;
                }//returned from ShowFileList Activity
            }
        } else if (requestCode == REQUEST_CHECK_SETTINGS) {
            //mLocationHelper.mResolvingLocationConnectError = false;

            switch (resultCode) {
                case Activity.RESULT_OK:
                    // All required changes were successfully made user changed location settings
                    break;
                case Activity.RESULT_CANCELED:
                    // The user was asked to change Location settings, but chose not to
                    break;
                default:
                    break;
            }
        } else if (requestCode == UPLOAD_FILE_SEND_REQUEST_CODE) {
			// This hasn't really finished the sharing operation. The Intent returns after user selects an app to use for sharing
			// We've closed the activity files while sharing. Now re-open them. Now we can re-write track intent to the files
			// If we've come back from turning-on WiFi when authorizing StravaShare, go back
			String sharingFileName = settings.getString(KEY_CHOSEN_TCXFILE, "");
			try {
				if ((intent != null) && intent.hasExtra(AUTH_NO_NETWORK_INTENT_RC)
						&& (resultCode != RESULT_CANCELED)) {
					goBackToStravaShare(resultCode, intent, sharingFileName);
				}
			} catch (Exception e) {
			    e.printStackTrace();
				// Dropbox throws a "ClassNotFoundException" here. Just catch it
			}
			sharingFileName = myBikeStat.fitLog.delTCXFITSuffix(myBikeStat.fitLog.stripFilePath(sharingFileName));
			String tcxLogFileName = myBikeStat.fitLog.delTCXFITSuffix(myBikeStat.fitLog.stripFilePath(myBikeStat.tcxLog.outFileName));
			// Can't re-open the current outFile if we're trying to share it, so we have to reset
			// If we're sharing an old file there is no need to reset the current intent
			forceNewTCX_FIT = tcxLogFileName.contains(sharingFileName);
			openReopenTCX_FIT();
		} else if (requestCode == RC_BLE_SETTINGS) {
            clearActiveDeviceLists();
			switch (resultCode) {
                case RESULT_OK:
                    int bleSettingsCode = 0;
                    Bundle bleSettingsBundle = intent.getExtras();
                    if (bleSettingsBundle != null) {
                        bleSettingsCode = bleSettingsBundle.getInt(KEY_CHOOSER_CODE);
                    }
                    if (debugBLEPowerCrank || debugBLEPowerCal) {Log.w(logtag, "onActivityResult RC_BLE_SETTINGS chooserCode: "
                            + intent.getExtras().getInt(KEY_CHOOSER_CODE));}
                    switch (bleSettingsCode) {
                        case BLESETTINGS_TYPE_CAL:
                            if (debugBLEPowerCrank || debugBLEPowerCal) {Log.w(logtag, "calAddress: " + intent.getStringExtra(KEY_CAL_CHANNEL)
                                    + " powerDeviceDataAddress: " + powerDeviceData.getAddress()
                                    + " oppositePowerDeviceDataAddress: " + oppositePowerDeviceData.getAddress());}
                            // put power sensor address in Intent Extras, find which powerDeviceData or oppositePowerDeviceData has that address
                            String theCalibrationAddress = intent.getStringExtra(KEY_CAL_CHANNEL);
                            doCalibratePower(theCalibrationAddress);
                            break;
                        case BLESETTINGS_TYPE_SEARCH_PAIR:
                            int deviceType = intent.getExtras().getInt(KEY_PAIR_CHANNEL, WILDCARD);
                            //Log.i(logtag, "doSearchPair() - devType: " + deviceType);
                            doSearchPair(BLEDeviceType.valueOf(deviceType));
                            break;
                        default:
                            //  make sure we only write crank length when it's different from pedal crank length
                            // test crank length
                            int myLeftCrankLength = myBikeStat.getLeftCrankLength();
                            int myRightCrankLength = myBikeStat.getRightCrankLength();
                            double desiredCrankLength = Double.parseDouble(settings.getString(CRANK_LENGTH, String.valueOf(DEFAULT_CRANK_LENGTH)));
                            String warningText = "";
                            if (desiredCrankLength > UPPER_CRANK_LENGTH) {
                                warningText = getResources().getString(R.string.crank_too_long);
                            }
                            if (desiredCrankLength < LOWER_CRANK_LENGTH) {
                                warningText = getResources().getString(R.string.crank_too_short);
                            }
                            if (!("").equals(warningText)) {
                                int toastTextColor = ContextCompat.getColor(context, R.color.gpsred);
                                viewToast(warningText, 40, BLE_TOAST_GRAVITY, toastAnchor, toastTextColor);
                            } else {// crank length within range
                                // have to specify CrankLength > 0 in case we don't have a power sensor at all
                                // only obey new crank length if it's in range; just warn if not
                                // see if user has changed crank length
                                if (debugBLEPowerCrank) {Log.i(logtag, "SharedPref desiredCrankLength: " + desiredCrankLength);}
                                // crankLength read from power sensor is twice physical length in mm because precision is 0.5 mm
                                // BikeStat crankLength > 0 means we've read the crank length from the power sensor
                                if (powerDeviceData != null) {
                                    int deviceCrankLength = (powerDeviceData.getDeviceType() == BIKE_LEFT_POWER_DEVICE)?
                                            myLeftCrankLength:myRightCrankLength;
                                    if (debugBLEPowerCrank) {Log.i(logtag, "power deviceCrankLength: " + deviceCrankLength);}
                                    powerDeviceData.shouldWriteCrankLength
                                            = (Math.abs((desiredCrankLength - deviceCrankLength)) >= .5) && deviceCrankLength > 0;
                                }
                                if (oppositePowerDeviceData != null) {
                                    int deviceCrankLength = (oppositePowerDeviceData.getDeviceType() == BIKE_LEFT_POWER_DEVICE)?
                                            myLeftCrankLength:myRightCrankLength;
                                    if (debugBLEPowerCrank) {Log.i(logtag, "opposite power deviceCrankLength: " + deviceCrankLength);}
                                    oppositePowerDeviceData.shouldWriteCrankLength
                                            = (Math.abs((desiredCrankLength - deviceCrankLength)) >= .5) && deviceCrankLength > 0;
                                }
                            }// default
                            break;
                    }//switch on BLESettings request type
                    break;//RESULT_OK:
                case RESULT_CANCELED:
                    break;
                default:
					break;
			}//switch on result code
        }// returned from BLESettings Activity
        else if (requestCode == REQUEST_CHANGE_LOCATION_SETTINGS) {

        } else if (requestCode == RC_BLUETOOTH_MODE_SETTINGS) {
            mResolvingBluetoothMode = false;
        }
        prefChanged = true;
        refreshScreen();
    }

    private void clearActiveDeviceLists() {
        hrmDeviceSPList.clear();
        cadenceDeviceSPList.clear();
        speedDeviceSPList.clear();
        powerDeviceSPList.clear();
        searchPairNamesList.clear();
        spdcadDeviceSPList.clear();
    }

    /**
     * Test if we have to close the FileEncoder. Only have to close before sharing
     * if we're sharing the current log file and we're sharing a fit file
     *
     * @param tcxLogFileName  current log file
     * @param sharingFileName file we're sharing
     * @return true if we have to close FileEncoder
     */
    private boolean mustCloseFit(String tcxLogFileName, String sharingFileName) {
        if (debugOldTCXFile) {
            Log.i(logtag, "tcxLog: " + tcxLogFileName + "  sharingName: " + sharingFileName);
            Log.i(logtag, " activityType: " + readActivityFileType());
            Log.i(logtag, "mustCloseFit() : "
                    + ((readActivityFileType() == Integer.valueOf(FIT_ACTIVITY_TYPE)
                    && tcxLogFileName.contains(sharingFileName)) ? "true" : "false"));
        }
        return tcxLogFileName.contains(sharingFileName);
    }

	/**
	 * If we needed to Authorize user Strava account, and we didn't have WiFi, user had to go to Settings
	 * Settings intent would return here, so send user back to try Authorize again
	 *
	 * @param resultCode      Intent result: OKAY or CANCELLED
	 * @param data            extras to let us know we were asking for WiFi
	 * @param sharingFileName file to upload to Strava
	 */
	private void goBackToStravaShare(int resultCode, Intent data, String sharingFileName) {
		int extras = 0;
		if ((data != null) && data.hasExtra(AUTH_NO_NETWORK_INTENT_RC)) {
			extras = data.getExtras().getInt(AUTH_NO_NETWORK_INTENT_RC);
		}
		// Log.i(logtag, "extras: " + extras + " resultCode: " + resultCode);
		if (resultCode == Activity.RESULT_OK && extras == 1) {
			Intent stravaUploadIntent = new Intent(this, StravaShareCBBLE.class);
			stravaUploadIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			stravaUploadIntent.putExtra(UPLOAD_FILENAME, sharingFileName);
			startActivityForResult(stravaUploadIntent, UPLOAD_FILE_SEND_REQUEST_CODE);
		}
	}

	/**
	 * Read user preference for type of activity file
	 *
	 * @return an integer indicating activity file type 0 = .tcx file, 1 = .fit file
	 */
	private int readActivityFileType() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String theString = sharedPref.getString(getResources().getString(R.string.pref_activity_file_key), TCX_ACTIVITY_TYPE);
		return Integer.parseInt(theString);
	}

	/**
	 * intermediate step to alert user if activity file will be closed
	 * If file to share is not the current output file, just proceed to sharingIntent
	 *
	 * @param uploadFilename user choice of file to share
	 */
	private void uploadFileSend(String uploadFilename) {
		//Log.i(logtag, "uploadFileSend()");
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		boolean showSharingAlert = settings.getBoolean(SHOW_SHARING, true);
		String sharingFileName = myBikeStat.fitLog.delTCXFITSuffix(myBikeStat.fitLog.stripFilePath(uploadFilename));
		String tcxLogFileName = myBikeStat.fitLog.delTCXFITSuffix(myBikeStat.fitLog.stripFilePath(myBikeStat.tcxLog.outFileName));
		if (debugOldTCXFile) {
            Log.i(logtag, "upload onActivityResult() - sharingFileName: " + sharingFileName);
            Log.i(logtag, "upload onActivityResult() - tcxLogFileName: " + tcxLogFileName);
        }
		if (tcxLogFileName.contains(sharingFileName) && showSharingAlert) {
			// warn user that a new activity will start
			doShowSharingAlert(uploadFilename);
		} else {
			doUploadIntent(uploadFilename);
		}
	}

    /**
	 * Now that we've closed the FileEncoder if we're sharing the current log file
	 * and the user has agreed to restart (if sharing current activity) let user choose how to share the file.
	 * We've made intent filters for RWGPS and Strava that we can intercept, or let user attach file to e-mail
	 * or upload to DropBox, Drive, etc. Those implicit actions are handled by those apps
	 *
	 * @param uploadFilename file to share
	 */
    private void doUploadIntent(String uploadFilename) {
		// Depending on where we're sending the file either use OAuth, an e-mail intent, etc
		//Log.w(logtag, "now doing upload Intent");
        Uri fileUri = FileProvider.getUriForFile(
                context,
                context.getApplicationContext()
                        .getPackageName() + ".provider", new File(uploadFilename));
		// Log.i(logtag, fileUri.toString());
		String bodyText = "Uploading new file";
        String subjectText = myBikeStat.fitLog.stripFilePath(uploadFilename);
        Intent uploadFileIntent;
		uploadFileIntent = new Intent(Intent.ACTION_SEND);
		uploadFileIntent.setDataAndType(fileUri, "text/cbsmarttype");
		uploadFileIntent.putExtra(UPLOAD_FILENAME, uploadFilename);
        uploadFileIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        uploadFileIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		uploadFileIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		uploadFileIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		uploadFileIntent.putExtra(Intent.EXTRA_EMAIL, RWGPS_EMAIL);
		uploadFileIntent.putExtra(Intent.EXTRA_SUBJECT, subjectText);
		//uploadFileIntent.putExtra(Intent.EXTRA_TEXT, bodyText);
		startActivityForResult(Intent.createChooser(uploadFileIntent, getString(R.string.upload_file)), UPLOAD_FILE_SEND_REQUEST_CODE);
	}

    /**
     * Give user a chance to cancel sharing because sharing current output file will close that file
     *
     * @param uploadFilename activity file to share
     */
    private void doShowSharingAlert(final String uploadFilename) {
        View checkBoxView = View.inflate(this, R.layout.sharing_checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.checkbox);
        checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean(SHOW_SHARING, !isChecked).apply();
            }
        });
        checkBox.setText(R.string.dont_remind);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Set the dialog title
        builder.setTitle(R.string.sharing_alert)
                .setMessage(R.string.sharing_text)
                .setView(checkBoxView)
                // Set the action buttons
                .setPositiveButton(R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so save the mSelectedItems results somewhere
                        // or return them to the component that opened the dialog
                        doUploadIntent(uploadFilename);
                    }
                })
                .setNegativeButton(R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create().show();
    }

	private void exitTrainerMode() {
		trainerMode = false;
		gpsFirstLocation = true;
		stopSpoofingLocations();
        // new CloseFitFileBackground().execute("");
        new ThreadPerTaskExecutor().execute(closeFitFileRunnable);
        forceNewTCX_FIT = true;
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
        editor.putString(KEY_CHOSEN_GPXFILE, "");
		editor.putBoolean(KEY_TRAINER_MODE, false);
		editor.putBoolean(KEY_FORCE_NEW_TCX, true);
		editor.apply();
		sensorWatchdogHandler.post(new Runnable() {
			@Override

			public void run() {
				resetData();
				createTitle("");
				prefChanged = true;
				refreshScreen();
			}
		});// post(Runnable)
    }

    // Monitors the state of the connection to the service.
    private final ServiceConnection mLocationServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mLocationService = binder.getService();
            if (debugLocation){Log.e(logtag, "onLocationServiceConnected()");}
            mLocationServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLocationService = null;
            mLocationServiceBound = false;
            Utilities.setRequestingLocationUpdates(context, false);
            if (debugLocation){Log.e(logtag, "onLocationServiceDisconnected()");}
        }
    };

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEDiscoveryService = ((BLEDiscoveryService.LocalBinder) service).getService();
            if (!mBLEDiscoveryService.initialize()) {Log.e(logtag, "Unable to initialize Bluetooth");}
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBLEDiscoveryService = null;
            if (debugBLEService){Log.e(logtag, "onDiscoveryServiceDisconnected()");}
        }
    };

    // Code to manage Service lifecycle.
    private final ServiceConnection mHRMServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEHRMService = ((BLEHRMService.LocalBinder) service).getService();
            if (!mBLEHRMService.initialize()) {Log.e(logtag, "Unable to initialize Bluetooth");}
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBLEHRMService = null;
            hrmDeviceData.status = DEAD;
            if (debugBLEService){Log.e(logtag, "onHRMServiceDisconnected()");}
        }
    };

    // Code to manage Service lifecycle.
    private final ServiceConnection mPowerServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEPowerService = ((BLEPowerService.LocalBinder) service).getService();
            if (!mBLEPowerService.initialize()) {
                Log.e(logtag, "Unable to initialize Bluetooth");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBLEPowerService = null;
            powerDeviceData.status = DEAD;
            if (debugBLEService){Log.e(logtag, "onPowerServiceDisconnected()");}
        }
    };

    // Code to manage Service lifecycle.
    private final ServiceConnection mOppositePowerServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEOppositePowerService = ((BLEOppositePowerService.LocalBinder) service).getService();
            if (!mBLEOppositePowerService.initialize()) {
                Log.e(logtag, "Unable to initialize Bluetooth");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBLEOppositePowerService = null;
            oppositePowerDeviceData.status = DEAD;
            if (debugBLEService){Log.e(logtag, "onOppositePowerServiceDisconnected()");}
        }
    };
    // Code to manage Service lifecycle.
    private final ServiceConnection mSpeedServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLESpeedService = ((BLESpeedService.LocalBinder) service).getService();
            if (!mBLESpeedService.initialize()) {
                Log.e(logtag, "Unable to initialize Bluetooth");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBLESpeedService = null;
            speedDeviceData.status = DEAD;
            if (debugBLEService){Log.e(logtag, "onSpeedServiceDisconnected()");}
        }
    };
    private final ServiceConnection mCadenceServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLECadenceService = ((BLECadenceService.LocalBinder) service).getService();
            if (!mBLECadenceService.initialize()) {
                Log.e(logtag, "Unable to initialize Bluetooth");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBLECadenceService = null;
            cadDeviceData.status = DEAD;
            if (debugBLEService){Log.e(logtag, "onCadenceServiceDisconnected()");}
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read or notification operations.
    private final BroadcastReceiver mGattDiscoveryReceiver = new BroadcastReceiver() {

        private void enableDiscoveryCharacteristic(BluetoothGattCharacteristic gattCharacteristic) {
            if (gattCharacteristic == null) { return; }
            if (debugBLEService) { logCharacteristicDescriptors(gattCharacteristic); }
            Log.wtf(logtag, "characteristicProp: " + gattCharacteristic.getProperties());
            if (isCharacteristicReadable(gattCharacteristic)) {
                // If there is an active notification on a characteristic, clear it first.
                Log.i(logtag, "enableDiscoveryCharacteristic... PROPERTY_READ");
                mBLEDiscoveryService.setCharacteristicNotification(gattCharacteristic, false);
                mBLEDiscoveryService.readCharacteristic(gattCharacteristic);
            }
            if (isCharacterisiticNotifiable(gattCharacteristic)) {
                Log.wtf(logtag, "enableDiscoveryCharacteristic... PROPERTY_NOTIFY");
                mBLEDiscoveryService.setCharacteristicNotification(gattCharacteristic, true);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            // need address to save device in bleActiveBikeDeviceList
            String mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            if (ACTION_GATT_CONNECTED.equals(action)) {
                if (debugBLEService){Log.i(logtag, "ACTION_GATT_CONNECTED");}
            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {
                if (debugBLEService){Log.i(logtag, "ACTION_GATT_DISCONNECTED");}
            } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                if (debugBLEService){Log.i(logtag, "ACTION_GATT_SERVICES_DISCOVERED");}
                // now wait a bit and then ask what Services were found
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        List<BluetoothGattService> theServices = mBLEDiscoveryService.getSupportedGattServices();
                        BluetoothGattCharacteristic gattCharacteristic =
                                    new BLEDeviceData(mDeviceAddress, BLEDeviceType.BIKE_SPDCAD_DEVICE_OTHER).hasSpdCadFeature(theServices);
                        enableDiscoveryCharacteristic(gattCharacteristic);
                    }
                }, 5);
            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                // in Discovery, we'll only receive this if we had to query CSC_FEATURE
                deviceDiscovered = true;
                if (debugBLEService) { Log.i(logtag, "ACTION_DATA_AVAILABLE - " + "data received: "
                            + intent.getStringExtra(EXTRA_DATA)); }
                BLEDeviceType identBLEDeviceType = BLEDeviceType.valueOf(Integer.parseInt(intent.getStringExtra(EXTRA_DATA)));
                BLEDeviceData newBLEDevice = new BLEDeviceData(mDeviceAddress, identBLEDeviceType);
                ContentValues newContent = new ContentValues();
                newContent.put(DB_KEY_ACTIVE, 1);
                // sometimes we access getDeviceType, sometimes we use .getData
                newContent.put(DB_KEY_DEV_TYPE, identBLEDeviceType.intValue());
                boolean deviceInDataBase = dataBaseAdapter.isDeviceInDataBase(mDeviceAddress);
                // this device wasn't in the bleActiveBikeDeviceList yet.
                // Set Search priority to -1 unless it's in the database
                int priority = (deviceInDataBase ?
                        dataBaseAdapter.fetchDeviceData(mDeviceAddress).getColumnIndexOrThrow(DB_KEY_SEARCH_PRIORITY) : -1);
                newContent.put(DB_KEY_SEARCH_PRIORITY, priority);
                newBLEDevice.setData(newContent);
                // sometimes we access getDeviceType, sometimes we use .getData
                newBLEDevice.setDeviceType(identBLEDeviceType);
                if (identBLEDeviceType != UNKNOWN_DEVICE) {
                    // this will break us out of the while loop in DeviceDiscoveryAsync
                    if (debugBLEService) { Log.i(logtag, "adding DeviceType - " + identBLEDeviceType.toString()
                                + " to BLEBikeDeviceList - address: " + mDeviceAddress); }
                    mBLEDeviceManager.addToBLEBikeDeviceList(newBLEDevice);
                    refreshDeviceLists();
                }
            }
        }
    };//mGattDiscoveryReceiver

    private final BroadcastReceiver mGattSpeedMeasurementReceiver = new BroadcastReceiver() {

        /**
         * When first connecting to a GATT device have to step thru characteristics disabling the current one and enabling the next.
         * Until we get to the end of the list, when we want to keep the measurement characteristic running.
         * We've set-up the characteristics from the list returned during GATT Services discovered.
         *
         * @param mDeviceData the DeviceData structure to modify
         */
        private void enableNextCharacteristic(BLEDeviceData mDeviceData) {
            BluetoothGattCharacteristic gattCharacteristic = mDeviceData.getNextCharacteristic();
            // get next characteristic on the list, increment characteristic index, disable previous characteristic, enable this one
            if (gattCharacteristic != null) {
                if (debugBLEService){
                    logCharacteristicDescriptors(gattCharacteristic);}
                if (isCharacteristicReadable(gattCharacteristic)) {
                    // If there is an active notification on a characteristic, clear it first.
                    if (speedDeviceData.getNotifyCharacteristic() != null) {
                        mBLESpeedService.setCharacteristicNotification(speedDeviceData.getNotifyCharacteristic(), false);
                        speedDeviceData.setNotifyCharacteristic(null);
                    }
                    mBLESpeedService.readCharacteristic(gattCharacteristic);
                }
                if (isCharacterisiticNotifiable(gattCharacteristic)
                        && gattCharacteristic == speedDeviceData.getMeasurementCharacteristic()) {
                    speedDeviceData.setNotifyCharacteristic(gattCharacteristic);
                    mBLESpeedService.setCharacteristicNotification(gattCharacteristic, true);
                }
            }
        }

        /**
         *
         * @param context The Context in which the receiver is running.
         * @param intent  The Intent being received.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            ContentValues newContent = new ContentValues();
            if (ACTION_SPEED_GATT_CONNECTED.equals(action)) {
                if (debugBLEService){Log.i(logtag, "ACTION_SPEED_GATT_CONNECTED");}

            } else if (ACTION_SPEED_GATT_DISCONNECTED.equals(action)) {
                if (debugBLEService){Log.w(logtag, "ACTION_SPEED_GATT_DISCONNECTED");}
                notTrackingSpeed();
                if (speedDeviceData.getDeviceType() == BLEDeviceType.BIKE_SPDCAD_DEVICE){
                    notTrackingCad();
                }
            } else if (ACTION_SPEED_GATT_SERVICES_DISCOVERED.equals(action)) {
                if (debugBLEService){Log.i(logtag, "ACTION_SPEED_GATT_SERVICES_DISCOVERED");}
                // now wait a bit and then ask what Services were found
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        List<BluetoothGattService> theServices = mBLESpeedService.getSupportedGattServices();
                        if (theServices != null && theServices.size() > 0) {
                            trackingSpeed();
                            speedDeviceData.updateDeviceDataGattServices(theServices);
                            // enable first characteristic to start the loop
                            enableNextCharacteristic(speedDeviceData);
                        } else {
                            Log.e(logtag, "No SPEED_GATT_SERVICES_DISCOVERED");
                        }
                    }
                }, 100);

            } else if (ACTION_SPEED_DATA_AVAILABLE.equals(action)) {
                newContent.put(DB_KEY_ACTIVE, 1);
                BLEDataType type = BLEDataType.valueOf(intent.getIntExtra(EXTRA_CSC_DATA_TYPE, BLEDataType.UNKNOWN.intValue()));
                if (type == CSC_MEAS) {
                    // device may have been Discovered as a cadence device initially, then speed data delivered. Change deviceType when data received.
                    BLEDeviceType devType = BLEDeviceType.valueOf(intent.getIntExtra(EXTRA_CSC_DEVICE_TYPE, BIKE_SPDCAD_DEVICE_OTHER.intValue()));
                    if (debugBLEData) { Log.i(logtag, "Speed GATT receiver CSC_MEAS - devType " + devType.toString());}
                    speedDeviceData.setDeviceType(devType);
                    mBLEDeviceManager.setDevAddressType(speedDeviceData.getAddress(), devType);
                    handleSpeedData(intent);
                    // if this is a spdcad device we'll also have cadence data. We're only using the SpeedService to handle both
                    if (speedDeviceData.getDeviceType() == BIKE_SPDCAD_DEVICE) {
                        cadDeviceData.setDeviceType(devType);
                        handleCadenceData(intent);
                    }
                } else {
                    if (debugBLEData) { Log.i(logtag, "ACTION_SPEED_DATA_AVAILABLE - " + "data received: "
                                + intent.getStringExtra(EXTRA_SPEED_DATA)); }
                    handleBLEDeviceInformation(intent, newContent, type, EXTRA_SPEED_DATA);
                    mBLEDeviceManager.updateActiveBikeDeviceData(speedDeviceData.getAddress(), newContent);
                    // also update connected BLEDeviceData; hrmDeviceData, powerDeviceData, speedDeviceData, cadDeviceData
                    speedDeviceData.setData(newContent);
                    // can only enable one Characteristic at a time
                    enableNextCharacteristic(speedDeviceData);
                }
            }
        }
    };

    private void handleSpeedData(Intent intent) {
        myBikeStat.hasSpeedSensor = true;
        speedDeviceData.status = TRACKING;
        // this just says we are still getting data from the Sensor, not that it is new data
        mBLEDeviceManager.wheelCnts.ertTimeStamp = SystemClock.elapsedRealtime();
        mBLEDeviceManager.wheelCnts.isDataCurrent = true;
        // Wahoo speed sensor shuts down if no motion for 2 minutes
        mBLEDeviceManager.wheelCnts.prevCount = mBLEDeviceManager.wheelCnts.currCount;
        mBLEDeviceManager.wheelCnts.prevTime = mBLEDeviceManager.wheelCnts.currTime;
        mBLEDeviceManager.wheelCnts.currCount = intent.getIntExtra(EXTRA_WHEEL_REVS, 0);
        mBLEDeviceManager.wheelCnts.currTime = intent.getIntExtra(EXTRA_WHEEL_REV_TIME, 0);
        // assume we're going the same speed in case we don't get another rev. Sensor will report the same rev#
        double wheelSpeed = myBikeStat.getSensorSpeed();
        // don't calculate speed if this is the first value. deltaWheelRevs could be anything
        if (mBLEDeviceManager.wheelCnts.initialized) {
            long deltaWheelRevs = mBLEDeviceManager.wheelCnts.currCount - mBLEDeviceManager.wheelCnts.prevCount;
            //wheel rev data rolls over at 4294967295 (32 bits)
            if (deltaWheelRevs < 0) {
                deltaWheelRevs += 0xffffffff;
            }
            long deltaWheelTime = (mBLEDeviceManager.wheelCnts.currTime - mBLEDeviceManager.wheelCnts.prevTime);
            //wheel time data rolls over at 65535 (16 bits)
            if (deltaWheelTime < 0) {
                deltaWheelTime += 0xffff;
            }
            double wheelDeltaTime = deltaWheelTime / WHEEL_TIME_RESOLUTION;
            // don't calculate speed or ride-time if this is the same data, or we were paused for a while
            if (wheelDeltaTime > 0 && wheelDeltaTime <= 3) {
                wheelSpeed = DEFAULT_WHEEL_CIRCUM * deltaWheelRevs / wheelDeltaTime;
                // wait until we have a new wheel rev before saying data is new.
                // this allows Sensor Watchdog to set speed to 0 if data not new
                mBLEDeviceManager.wheelCnts.ertTimeStampNewData = SystemClock.elapsedRealtime();
                mBLEDeviceManager.wheelCnts.isDataNew = true;
                myBikeStat.setSensorSpeed(wheelSpeed);
                refreshSpeed();
                if (debugBLEData) { Log.i(logtag, "received wheelSpeed (mph): " + String.format(FORMAT_4_3F, wheelSpeed * mph_per_mps)); }
                if (!myBikeStat.isPaused() && !writingTrackRecord) {
                    myBikeStat.setWheelRideTime(myBikeStat.getWheelRideTime() + wheelDeltaTime);
                }
            }
            long accumulatedWheelRevs = mBLEDeviceManager.wheelCnts.cumulativeRevolutions + deltaWheelRevs;
            mBLEDeviceManager.wheelCnts.cumulativeRevolutions += deltaWheelRevs;
            // .cumulativeRevsAtCalStart is set in restartWheelCal()
            mBLEDeviceManager.wheelCnts.calTotalCount = accumulatedWheelRevs
                    - mBLEDeviceManager.wheelCnts.cumulativeRevsAtCalStart;
            mBLEDeviceManager.wheelCnts.cumulativeRevolutions = accumulatedWheelRevs;
            // If deltaCount < 0 it may be that the speed sensor stopped and restarted
            // or there was an overflow in cumulativeRevolutions. If wheel is not calibrated
            // and delta is < 0 restart wheel cal. Can't have cumRevsatCalStart be greater than cumRevs!
            double deltaDistance = deltaWheelRevs * mBLEDeviceManager.wheelCnts.wheelCircumference;
            // now if maxEstimatedSpeed MIN_SPEED, because we haven't gotten a revolution in a while, set calculatedSpeed to 0.
            double distance = myBikeStat.getWheelTripDistance();
            boolean tooFast = wheelSpeed > MAXIMUM_SPEED;
            if (!tooFast) {
                if (deltaDistance < 0){
                    deltaDistance = 0;
                }
                distance += deltaDistance;
            }

            // If gps location is not current, use calibrated speed sensor
            // to measure distance and ride time. We only use speed sensor in trainer mode.
            // Copy values to GPS Trip distance and ride time.
            if ((!isGPSLocationCurrent() && myBikeStat.hasCalSpeedSensor) || trainerMode) {
                myBikeStat.setWheelTripDistance(distance);
                myBikeStat.setSpoofWheelTripDistance(distance);
                myBikeStat.setGPSTripDistance(myBikeStat.getWheelTripDistance());
                myBikeStat.setGPSTripTime(myBikeStat.getWheelRideTime());
                // use speed sensor in preference over PowerTap speed sensor
                myBikeStat.setPowerWheelTripDistance(myBikeStat.getWheelTripDistance());
                myBikeStat.setPowerWheelRideTime(myBikeStat.getWheelRideTime());
            }
            if (!trainerMode) {
                // time & distance are handled differently in TrainerMode
                refreshTimeDistance();
            }

        } else {
            // we weren't initialized, but having received data, now we are
            mBLEDeviceManager.wheelCnts.prevTime = mBLEDeviceManager.wheelCnts.currTime;
            mBLEDeviceManager.wheelCnts.initialized = true;
            mBLEDeviceManager.wheelCnts.ertTimeStampNewData = SystemClock.elapsedRealtime();
        }
    }

    private final BroadcastReceiver mGattCadenceMeasurementReceiver = new BroadcastReceiver() {

        private void enableMeasurementCharacteristic(BluetoothGattCharacteristic gattCharacteristic) {
            if (debugBLEService){logCharacteristicDescriptors(gattCharacteristic);}

            if (isCharacteristicReadable(gattCharacteristic)) {
                // If there is an active notification on a characteristic, clear it first.
                if (cadDeviceData.getNotifyCharacteristic() != null) {
                    mBLECadenceService.setCharacteristicNotification(cadDeviceData.getNotifyCharacteristic(), false);
                    cadDeviceData.setNotifyCharacteristic(null);
                }
                mBLECadenceService.readCharacteristic(gattCharacteristic);
            }
            if (isCharacterisiticNotifiable(gattCharacteristic)
                    && gattCharacteristic == cadDeviceData.getMeasurementCharacteristic()) {
                cadDeviceData.setNotifyCharacteristic(gattCharacteristic);
                mBLECadenceService.setCharacteristicNotification(gattCharacteristic, true);
            }
        }

        /**
         * When first connecting to a GATT device have to step thru characteristics disabling the current one and enabling the next.
         * Until we get to the end of the list, when we want to keep the measurement characteristic running.
         * We've set-up the characteristics from the list returned during GATT Services discovered.
         *
         * @param mDeviceData the DeviceData structure to modify
         */
        private void enableNextCharacteristic(BLEDeviceData mDeviceData) {
            BluetoothGattCharacteristic nextCharacteristic = mDeviceData.getNextCharacteristic();
            // get next characteristic on the list, increment characteristic index, disable previous characteristic, enable this one
            if (nextCharacteristic != null) {
                enableMeasurementCharacteristic(nextCharacteristic);
            }
        }

        /**
         *
         * @param context The Context in which the receiver is running.
         * @param intent  The Intent being received.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            ContentValues newContent = new ContentValues();
            if (ACTION_CAD_GATT_CONNECTED.equals(action)) {
                if (debugBLEService){Log.i(logtag, "ACTION_CAD_GATT_CONNECTED");}

            } else if (ACTION_CAD_GATT_DISCONNECTED.equals(action)) {
                if (debugBLEService){Log.w(logtag, "ACTION_CAD_GATT_DISCONNECTED");}
                cadDeviceData.status = SEARCHING;
                notTrackingCad();
            } else if (ACTION_CAD_GATT_SERVICES_DISCOVERED.equals(action)) {
                if (debugBLEService){Log.i(logtag, "ACTION_CAD_GATT_SERVICES_DISCOVERED");}
                // now wait a bit and then ask what Services were found
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        List<BluetoothGattService> theServices = mBLECadenceService.getSupportedGattServices();
                        if (theServices != null && theServices.size() > 0) {
                            trackingCad();
                            cadDeviceData.updateDeviceDataGattServices(theServices);
                            // enable first characteristic to start the loop
                            enableNextCharacteristic(cadDeviceData);
                        } else { Log.e(logtag, "No CAD_GATT_SERVICES_DISCOVERED"); }
                    }
                }, 100);

            } else if (ACTION_CAD_DATA_AVAILABLE.equals(action)) {
                newContent.put(DB_KEY_ACTIVE, 1);
                BLEDataType type = BLEDataType.valueOf(intent.getIntExtra(EXTRA_CSC_DATA_TYPE, BLEDataType.UNKNOWN.intValue()));
                if (type == CSC_MEAS) {
                    // device may have been Discovered as a speed device initially, then cad data delivered.
                    // Change deviceType when data received.
                    BLEDeviceType devType = BLEDeviceType.valueOf(intent.getIntExtra(EXTRA_CSC_DEVICE_TYPE, BIKE_SPDCAD_DEVICE_OTHER.intValue()));
                    if (debugBLEData) { Log.i(logtag, "Cadence GATT receiver CSC_MEAS - devType " + devType.toString());}
                    cadDeviceData.setDeviceType(devType);
                    mBLEDeviceManager.setDevAddressType(cadDeviceData.getAddress(), devType);
                    handleCadenceData(intent);
                    if (cadDeviceData.getDeviceType() == BIKE_SPDCAD_DEVICE){
                        speedDeviceData.setDeviceType(devType);
                        handleSpeedData(intent);
                    }
                } else {
                    if (debugBLEData) { Log.i(logtag, "ACTION_CADENCE_DATA_AVAILABLE - " + "data received: "
                                + intent.getStringExtra(EXTRA_CAD_DATA)); }
                    handleBLEDeviceInformation(intent, newContent, type, EXTRA_CAD_DATA);
                    mBLEDeviceManager.updateActiveBikeDeviceData(cadDeviceData.getAddress(), newContent);
                    // also update connected BLEDeviceData; hrmDeviceData, powerDeviceData, speedDeviceData, cadDeviceData
                    cadDeviceData.setData(newContent);
                    // can only enable one Characteristic at a time
                    enableNextCharacteristic(cadDeviceData);
                }
            }
        }
    };

    private void handleCadenceData(Intent intent) {
        if (debugBLEData) { Log.i(logtag, "ACTION_CADENCE_DATA_AVAILABLE - "
                    + "data received: rev - " + intent.getIntExtra(EXTRA_REV_DATA, 0)
                    + " data received: time - " + intent.getIntExtra(EXTRA_REV_TIME_DATA, 0)); }
        myBikeStat.hasCadence = true;
        cadDeviceData.status = TRACKING;
        // this just says we are still getting data from the Sensor, not that it is new data
        mBLEDeviceManager.pedalCadenceCnts.ertTimeStamp = SystemClock.elapsedRealtime();
        mBLEDeviceManager.pedalCadenceCnts.isDataCurrent = true;
        // Wahoo speed sensor shuts down if no motion for 2 minutes
        mBLEDeviceManager.pedalCadenceCnts.prevCount = mBLEDeviceManager.pedalCadenceCnts.currCount;
        mBLEDeviceManager.pedalCadenceCnts.prevTime = mBLEDeviceManager.pedalCadenceCnts.currTime;
        mBLEDeviceManager.pedalCadenceCnts.currCount = intent.getIntExtra(EXTRA_REV_DATA, 0);
        mBLEDeviceManager.pedalCadenceCnts.currTime = intent.getIntExtra(EXTRA_REV_TIME_DATA, 0);
        // Wahoo cadence sensor shuts down if no motion for 2 minutes
        // don't calculate cadence if this is the first value. deltaCadenceCnts could be anything
        if (mBLEDeviceManager.pedalCadenceCnts.initialized) {
            long deltaCadenceCnts = mBLEDeviceManager.pedalCadenceCnts.currCount - mBLEDeviceManager.pedalCadenceCnts.prevCount;
            // Cadence cnt data rolls over at 65535 (16 bits)
            if (deltaCadenceCnts < 0) {
                deltaCadenceCnts += 0xffff;
            }
            long deltaCadenceTime = (mBLEDeviceManager.pedalCadenceCnts.currTime - mBLEDeviceManager.pedalCadenceCnts.prevTime);
            // Cadence time data rolls over at 65535 (16 bits)
            if (deltaCadenceTime < 0) {
                deltaCadenceTime += 0xffff;
            }
            if (debugBLEData) {
                Log.i(logtag, "delta cadence counts: " + deltaCadenceCnts);
                Log.i(logtag, "delta cadence time: " + deltaCadenceTime);
            }
            // cadenceDeltaTime is in seconds, want it in minutes to get rpm
            double cadenceDeltaTime = (deltaCadenceTime / CADENCE_TIME_RESOLUTION) / SECONDS_PER_MINUTE;
            // this will "hold" the previous value until a new cadence count is received
            int cadence = myBikeStat.getPedalCadence();
            if (cadenceDeltaTime > 0) {
                cadence = (int) (deltaCadenceCnts / cadenceDeltaTime);
                if (debugBLEData) {
                    Log.i(logtag, "cadence (rpm): " + cadence);}
                // wait until we have a new wheel rev before saying data is new.
                // this allows Sensor Watchdog to set speed to 0 if data not new
                mBLEDeviceManager.pedalCadenceCnts.ertTimeStampNewData = SystemClock.elapsedRealtime();
                mBLEDeviceManager.pedalCadenceCnts.isDataNew = true;
            }
              if ((cadence >= 0) && !writingTrackRecord
                    && (cadence < 241)) {
                myBikeStat.setPedalCadence(cadence);
                if (cadence > myBikeStat.getMaxCadence()) {
                    myBikeStat.setMaxCadence(cadence);
                }
            }
            if (cadence > 0) {// don't average zeros
                mBLEDeviceManager.addNumPedalCad();
                mBLEDeviceManager.addTotalPedalCad(cadence);
                myBikeStat.setAvgCadence((int) (mBLEDeviceManager.getTotalPedalCad() / mBLEDeviceManager.getNumPedalCad()));
            }
            refreshCadence();
        } else {
            // we weren't initialized, but having received data, now we are
            mBLEDeviceManager.pedalCadenceCnts.initialized = true;
            mBLEDeviceManager.pedalCadenceCnts.ertTimeStampNewData = SystemClock.elapsedRealtime();
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read or notification operations.
    private final BroadcastReceiver mGattHRMMeasurementReceiver = new BroadcastReceiver() {

        private void enableNextCharacteristic(BLEDeviceData mDeviceData) {
            BluetoothGattCharacteristic gattCharacteristic = mDeviceData.getNextCharacteristic();
            // get next characteristic on the list, increment characteristic index, disable previous characteristic, enable this one
            if (gattCharacteristic != null) {
                if (debugBLEService){ logCharacteristicDescriptors(gattCharacteristic);}
                if (isCharacteristicReadable(gattCharacteristic)) {
                    // If there is an active notification on a characteristic, clear it first.
                    if (hrmDeviceData.getNotifyCharacteristic() != null) {
                        mBLEHRMService.setCharacteristicNotification(hrmDeviceData.getNotifyCharacteristic(), false);
                        hrmDeviceData.setNotifyCharacteristic(null);
                    }
                    mBLEHRMService.readCharacteristic(gattCharacteristic);
                }
                if (isCharacterisiticNotifiable(gattCharacteristic)
                        && gattCharacteristic == hrmDeviceData.getMeasurementCharacteristic()) {
                    hrmDeviceData.setNotifyCharacteristic(gattCharacteristic);
                    mBLEHRMService.setCharacteristicNotification(gattCharacteristic, true);
                }
            }
        }

        private void averageCalcHR(long heartRate) {
            if (heartRate > 0) {// don't average zeros
                mBLEDeviceManager.addNumHREvents();
                mBLEDeviceManager.addTotalHRCounts(heartRate);
                myBikeStat.setAvgHeartRate((int) (mBLEDeviceManager.getTotalHRCounts() / mBLEDeviceManager.getNumHREvents()));
            }
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
           if (debugBLEData) { Log.i(logtag, "receiving broadcast from DeviceType: " + HEARTRATE_DEVICE.name() + " int: "
                   + HEARTRATE_DEVICE.intValue()); }
            ContentValues newContent = new ContentValues();
            if (ACTION_HRM_GATT_CONNECTED.equals(action)) {
                if (debugBLEService) {Log.i(logtag, "ACTION_HRM_GATT_CONNECTED");}

            } else if (ACTION_HRM_GATT_DISCONNECTED.equals(action)) {
                if (debugBLEService) {Log.w(logtag, "ACTION_HRM_GATT_DISCONNECTED");}
                hrmDeviceData.status = SEARCHING;
                notTrackingHRM();
            } else if (ACTION_HRM_GATT_SERVICES_DISCOVERED.equals(action)) {
                if (debugBLEService) {Log.i(logtag, "ACTION_HRM_GATT_SERVICES_DISCOVERED");}
                // now wait a bit and then ask what Services were found
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        List<BluetoothGattService> theServices = mBLEHRMService.getSupportedGattServices();
                        if (theServices != null && theServices.size() > 0) {
                            trackingHRM();
                            hrmDeviceData.updateDeviceDataGattServices(theServices);
                            // enable first characteristic to start the loop
                            enableNextCharacteristic(hrmDeviceData);
                        } else { Log.e(logtag, "No HRM_GATT_SERVICES_DISCOVERED"); }
                    }
                }, 2);
            } else if (ACTION_HRM_DATA_AVAILABLE.equals(action)) {
                newContent.put(DB_KEY_ACTIVE, 1);
                if (debugBLEData) Log.i(logtag, "ACTION_HRM_DATA_AVAILABLE - " + "data received: "
                        + intent.getStringExtra(EXTRA_HRM_DATA));
                BLEDataType type = BLEDataType.valueOf(intent.getIntExtra(EXTRA_HRM_DATA_TYPE, BLEDataType.UNKNOWN.intValue()));
                if (type == HEARTRATE_MEAS) {
                    myBikeStat.hasHR = true;
                    hrmDeviceData.status = TRACKING;
                    mBLEDeviceManager.hrData.ertTimeStamp = SystemClock.elapsedRealtime();
                    mBLEDeviceManager.hrData.isDataCurrent = true;
                    final int heartRate = Integer.parseInt(intent.getStringExtra(EXTRA_HRM_DATA));
                    if (debugBLEData) Log.i(logtag, "received heartRate: " + heartRate);
                    // only update display if this is new data
                    averageCalcHR(heartRate);
                    myBikeStat.setHR(heartRate);
                    if (heartRate > myBikeStat.getMaxHeartRate()) {
                        myBikeStat.setMaxHeartRate(heartRate);
                    }
                    refreshHR();
                } else {
                    handleBLEDeviceInformation(intent, newContent, type, EXTRA_HRM_DATA);
                    mBLEDeviceManager.updateActiveBikeDeviceData(hrmDeviceData.getAddress(), newContent);
                    // also update connected BLEDeviceData; hrmDeviceData, powerDeviceData, speedDeviceData, cadDeviceData
                    hrmDeviceData.setData(newContent);
                    // can only enable one Characteristic at a time
                    enableNextCharacteristic(hrmDeviceData);
                }
            }
        }
    };

    private final BroadcastReceiver mGattPowerMeasurementReceiver = new BroadcastReceiver() {
        /**
         * When first connecting to a GATT device have to step thru characteristics disabling the current one and enabling the next.
         * Until we get to the end of the list, when we want to keep the measurement characteristic running. We've stored the
         * characteristics this device knows during ServiceDiscovery.
         *
         * @param mDeviceData the DeviceData structure to modify
         */
        private void enableNextCharacteristic(BLEDeviceData mDeviceData, BLEPowerService thePowerService) {
            BluetoothGattCharacteristic nextCharacteristic = mDeviceData.getNextCharacteristic();
            // get next characteristic on the list, increment characteristic index, disable previous characteristic, enable this one
            if (nextCharacteristic != null) {
                if (debugBLEService) {logCharacteristicDescriptors(nextCharacteristic);}
                if (isCharacteristicReadable(nextCharacteristic)) {
                    // If there is an active notification on a characteristic, clear it first.
                    if (mDeviceData.getNotifyCharacteristic() != null) {
                        thePowerService.setCharacteristicNotification(mDeviceData.getNotifyCharacteristic(), false);
                        mDeviceData.setNotifyCharacteristic(null);
                    }
                    thePowerService.readCharacteristic(nextCharacteristic);
                }
                if (isCharacterisiticNotifiable(nextCharacteristic)) {
                    mDeviceData.setNotifyCharacteristic(nextCharacteristic);
                    thePowerService.setCharacteristicNotification(nextCharacteristic, true);
                }
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            ContentValues newContent = new ContentValues();
            if (ACTION_POWER_GATT_CONNECTED.equals(action)) {
                if (debugBLEService) {Log.i(logtag, "ACTION_POWER_GATT_CONNECTED");}
            } else if (ACTION_POWER_GATT_DISCONNECTED.equals(action)) {
                if (debugBLEService) {Log.w(logtag, "ACTION_POWER_GATT_DISCONNECTED");}
                notTrackingPower(powerDeviceData);
            } else if (ACTION_POWER_GATT_SERVICES_DISCOVERED.equals(action)) {
                trackingPower(powerDeviceData);
                if (debugBLEService) {Log.i(logtag, "ACTION_POWER_GATT_SERVICES_DISCOVERED");}
                // now wait a bit and then ask what Services were found
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        List<BluetoothGattService> theServices = mBLEPowerService.getSupportedGattServices();
                        if (theServices != null && theServices.size() > 0) {
                            powerDeviceData.updateDeviceDataGattServices(theServices);
                            // enable first characteristic to start the loop
                            enableNextCharacteristic(powerDeviceData, mBLEPowerService);
                        } else {Log.e(logtag, "No POWER_GATT_SERVICES_DISCOVERED");}
                    }
                }, 100);
            } else if (ACTION_POWER_DATA_AVAILABLE.equals(action)) {
                newContent.put(DB_KEY_ACTIVE, 1);
                BLEDeviceType thisDeviceType = powerDeviceData.getDeviceType();
                String deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_POWER_ADDRESS);
                if (debugBLEPowerData) {Log.i(logtag, "ACTION_POWER_DATA_AVAILABLE - " + "data received: "
                        + intent.getStringExtra(EXTRA_POWER_DATA));}
                BLEDataType powerDataType = BLEDataType.valueOf(intent.getIntExtra(EXTRA_POWER_DATA_TYPE, BLEDataType.UNKNOWN.intValue()));
                if (powerDataType == BIKE_POWER_MEAS) {
                    // only declare TRACKING if we're receiving instantaneousPower measurements
                    powerDeviceData.status = TRACKING;
                    myBikeStat.hasPower = true;
                    final int instantaneousPower = handlePowerData(intent, mBLEDeviceManager.calcPowerData);
                    boolean hasPowerSpeedSensor = intent.getBooleanExtra(EXTRA_POWER_HAS_SPEED, false);
                    boolean hasPowerCadenceSensor = intent.getBooleanExtra(EXTRA_POWER_HAS_CAD, false);
                    myBikeStat.hasPowerSpeedSensor = hasPowerSpeedSensor;
                    myBikeStat.hasPowerCadence = hasPowerCadenceSensor;
                    if (hasPowerSpeedSensor) {
                        handlePowerSpeedEvent(intent);
                    }
                    if (hasPowerCadenceSensor) {
                        handlePowerCadenceEvent(intent, mBLEDeviceManager.crankCadenceCnts);
                    }
                    if (!writingTrackRecord) {
                        // use prevPower to display average of last two power values
                        myBikeStat.setPrevPower(myBikeStat.getPower());
                        if (thisDeviceType == BIKE_LEFT_POWER_DEVICE) {
                            myBikeStat.setLeftPower(instantaneousPower);
                        } else if (thisDeviceType == BIKE_RIGHT_POWER_DEVICE) {
                            myBikeStat.setRightPower(instantaneousPower);
                        } else {
                            myBikeStat.setPower(instantaneousPower);
                        }
                        if (myBikeStat.getPower() > myBikeStat.getMaxPower()) {
                            myBikeStat.setMaxPower(myBikeStat.getPower());
                        }
                    }
                    // todo if distributed power, instantaneousPower is only half; how does this affect avgPower calc?
                    //todo have to use mBLEDeviceManager.calcOppositePowerData for distributed power sensors
                    averageCalcPower(myBikeStat.getPower(),
                            (SystemClock.elapsedRealtime() - mBLEDeviceManager.calcPowerData.ertOfLastEvent));
                    refreshPower();
                    mBLEDeviceManager.calcPowerData.ertOfLastEvent = SystemClock.elapsedRealtime();
                    if (debugBLEPowerData || debugBLEPowerCal || debugBLEPowerCal) {Log.i(logtag, "Device_power_address: " + deviceAddress);}
                    if (debugBLEPowerData) {Log.i(logtag, "received instantaneousPower: " + instantaneousPower);}
                } else if (powerDataType == BIKE_POWER_CONTROL_PT) {
                    int pcpOpCode = intent.getIntExtra(EXTRA_PCP_OPCODE, 0);
                    if (debugBLEPowerCrank || debugBLEPowerCal) {Log.i(logtag, "pcpOpCode: " + pcpOpCode);}
                    if (pcpOpCode == CALIBRATE_POWER_OPCODE) {
                        handleCalibrationResult(context, intent, deviceAddress);
                    } else if (pcpOpCode == REQUEST_CRANK_LENGTH_OPCODE) {
                        handleReadCrankLength(intent, powerDeviceData);
                    } else if (pcpOpCode == WRITE_CRANK_LENGTH_OPCODE) {
                        handleWriteCrankLength(context, intent, powerDeviceData);
                    }
                } else if (powerDataType == SENSOR_LOCATION) {
                    handleSensorLocation(intent, newContent, powerDeviceData);
                    mBLEDeviceManager.updateActiveBikeDeviceData(deviceAddress, newContent);
                    // also update connected BLEDeviceData; hrmDeviceData, powerDeviceData, speedDeviceData, cadDeviceData
                    powerDeviceData.setData(newContent);
                    enableNextCharacteristic(powerDeviceData, mBLEPowerService);
                }  else if (powerDataType == BIKE_POWER_FEATURE) {
                    handleBikeFeature(intent, newContent, powerDeviceData);
                    mBLEDeviceManager.updateActiveBikeDeviceData(deviceAddress, newContent);
                    // also update connected BLEDeviceData; hrmDeviceData, powerDeviceData, speedDeviceData, cadDeviceData
                    powerDeviceData.setData(newContent);
                    enableNextCharacteristic(powerDeviceData, mBLEPowerService);
                } else {
                    handleBLEDeviceInformation(intent, newContent, powerDataType, EXTRA_POWER_DATA);
                    mBLEDeviceManager.updateActiveBikeDeviceData(deviceAddress, newContent);
                    // also update connected BLEDeviceData; hrmDeviceData, powerDeviceData, speedDeviceData, cadDeviceData
                    powerDeviceData.setData(newContent);
                    // can only enable one Characteristic at a time
                    enableNextCharacteristic(powerDeviceData, mBLEPowerService);
                }
            }
        }

        private void handlePowerSpeedEvent(Intent intent) {
            // ignore this for opposite power
            mBLEDeviceManager.powerWheelCnts.ertTimeStamp = SystemClock.elapsedRealtime();
            mBLEDeviceManager.powerWheelCnts.isDataCurrent = true;
            mBLEDeviceManager.powerWheelCnts.prevCount = mBLEDeviceManager.powerWheelCnts.currCount;
            mBLEDeviceManager.powerWheelCnts.currCount = intent.getIntExtra(EXTRA_POWER_WHEEL_REVS, 0);
            mBLEDeviceManager.powerWheelCnts.prevTime = mBLEDeviceManager.powerWheelCnts.currTime;
            mBLEDeviceManager.powerWheelCnts.currTime = intent.getIntExtra(EXTRA_POWER_WHEEL_REV_TIME, 0);
            double powerWheelSpeed = myBikeStat.getPowerSpeed();
            // don't calculate speed if this is the first value. deltaWheelRevs could be anything
            if (mBLEDeviceManager.powerWheelCnts.initialized) {
                long deltaWheelRevs = mBLEDeviceManager.powerWheelCnts.currCount - mBLEDeviceManager.powerWheelCnts.prevCount;
                //wheel rev data rolls over at 4294967295 (32 bits)
                if (deltaWheelRevs < 0) {
                    deltaWheelRevs += 0xffffffff;
                }
                long deltaWheelTime = (mBLEDeviceManager.powerWheelCnts.currTime - mBLEDeviceManager.powerWheelCnts.prevTime);
                //wheel time data rolls over at 65535 (16 bits)
                if (deltaWheelTime < 0) {
                    deltaWheelTime += 0xffff;
                }
                double wheelDeltaTime = deltaWheelTime / POWER_WHEEL_TIME_RESOLUTION;
                // don't calculate speed or ride-time if this is the same data, or we were paused for a while
                if (wheelDeltaTime > 0 && wheelDeltaTime <= 3) {
                    powerWheelSpeed = DEFAULT_WHEEL_CIRCUM * deltaWheelRevs / wheelDeltaTime;
                    mBLEDeviceManager.powerWheelCnts.ertTimeStampNewData = SystemClock.elapsedRealtime();
                    mBLEDeviceManager.powerWheelCnts.isDataNew = true;
                    if (debugBLEPowerWheel) {Log.i(logtag, "received wheelSpeed (mph): "
                                + String.format(FORMAT_4_3F, powerWheelSpeed * mph_per_mps));}
                }
                if (!myBikeStat.isPaused() && !writingTrackRecord) {
                    myBikeStat.setPowerWheelRideTime(myBikeStat.getPowerWheelRideTime() + wheelDeltaTime);
                }
                long accumulatedWheelRevs = mBLEDeviceManager.powerWheelCnts.cumulativeRevolutions + deltaWheelRevs;
                // .cumulativeRevsAtCalStart is set in restartPowerWheelCal()
                mBLEDeviceManager.powerWheelCnts.calTotalCount = accumulatedWheelRevs
                        - mBLEDeviceManager.powerWheelCnts.cumulativeRevsAtCalStart;
                mBLEDeviceManager.powerWheelCnts.cumulativeRevolutions = accumulatedWheelRevs;
                double deltaDistance = deltaWheelRevs * mBLEDeviceManager.powerWheelCnts.wheelCircumference;
                if (deltaWheelRevs > 0 && !writingTrackRecord) {
                    myBikeStat.setPowerWheelTripDistance(deltaDistance
                            + myBikeStat.getPowerWheelTripDistance());
                    myBikeStat.setSpoofWheelTripDistance(deltaDistance
                            + myBikeStat.getPowerWheelTripDistance());
                }
                final double finalPowerWheelSpeed = powerWheelSpeed;
                sensorWatchdogHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (finalPowerWheelSpeed < MAXIMUM_SPEED) {
                            myBikeStat.setPowerSpeed(finalPowerWheelSpeed);
                            //only refresh speed if no speed sensor. Don't want to call this too often
                            if (!myBikeStat.hasSpeedSensor) {
                                refreshSpeed();
                            }
                        }
                    }
                });

                // If gps location not current and we don't have a speed sensor,
                // or if power wheel is calibrated and we don't have a calibrated speed sensor,
                // use power wheel sensor to measure distance and ride time.
                // Copy values over to GPS Trip distance and ride time
                boolean condition1 = !isGPSLocationCurrent() && !myBikeStat.hasSpeedSensor;
                boolean condition2 = myBikeStat.hasCalPowerSpeedSensor && !myBikeStat.hasCalSpeedSensor;
                if ((condition1 || condition2) && !writingTrackRecord) {
                    myBikeStat.setGPSTripDistance(myBikeStat.getPowerWheelTripDistance());
                    myBikeStat.setGPSTripTime(myBikeStat.getPowerWheelRideTime());
                    myBikeStat.setWheelTripDistance(myBikeStat.getPowerWheelTripDistance());
                    myBikeStat.setWheelRideTime(myBikeStat.getPowerWheelRideTime());
                }
            } else {
                mBLEDeviceManager.powerWheelCnts.prevTime = mBLEDeviceManager.powerWheelCnts.currTime;
                mBLEDeviceManager.powerWheelCnts.initialized = true;
                mBLEDeviceManager.powerWheelCnts.ertTimeStampNewData = SystemClock.elapsedRealtime();
            }
        }// handlePowerSpeedEvent()
    };

    private final BroadcastReceiver mGattOppositePowerMeasurementReceiver = new BroadcastReceiver() {
        /**
         * When first connecting to a GATT device have to step thru characteristics disabling the current one and enabling the next.
         * Until we get to the end of the list, when we want to keep the measurement characteristic running. We've stored the
         * characteristics this device knows during ServiceDiscovery.
         *
         * @param mDeviceData the DeviceData structure to modify
         */
        private void enableNextCharacteristic(BLEDeviceData mDeviceData, BLEOppositePowerService thePowerService) {
            BluetoothGattCharacteristic nextCharacteristic = mDeviceData.getNextCharacteristic();
            // get next characteristic on the list, increment characteristic index, disable previous characteristic, enable this one
            if (nextCharacteristic != null) {
                if (debugBLEService) {logCharacteristicDescriptors(nextCharacteristic);}
                if (isCharacteristicReadable(nextCharacteristic)) {
                    // If there is an active notification on a characteristic, clear it first.
                    if (mDeviceData.getNotifyCharacteristic() != null) {
                        thePowerService.setCharacteristicNotification(mDeviceData.getNotifyCharacteristic(), false);
                        mDeviceData.setNotifyCharacteristic(null);
                    }
                    thePowerService.readCharacteristic(nextCharacteristic);
                }
                if (isCharacterisiticNotifiable(nextCharacteristic)) {
                    mDeviceData.setNotifyCharacteristic(nextCharacteristic);
                    thePowerService.setCharacteristicNotification(nextCharacteristic, true);
                }
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            ContentValues newContent = new ContentValues();
            if (ACTION_OPPOSITE_POWER_GATT_CONNECTED.equals(action)) {
                if (debugBLEService) {Log.i(logtag, "ACTION_OPPOSITE_POWER_GATT_CONNECTED");}
            } else if (ACTION_OPPOSITE_POWER_GATT_DISCONNECTED.equals(action)) {
                if (debugBLEService) {Log.w(logtag, "ACTION_OPPOSITE_POWER_GATT_DISCONNECTED");}
                notTrackingPower(oppositePowerDeviceData);
            } else if (ACTION_OPPOSITE_POWER_GATT_SERVICES_DISCOVERED.equals(action)) {
                trackingPower(oppositePowerDeviceData);
                if (debugBLEService) {Log.i(logtag, "ACTION_OPPOSITE_POWER_GATT_SERVICES_DISCOVERED");}
                // now wait a bit and then ask what Services were found
                sensorWatchdogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        List<BluetoothGattService> theServices = mBLEOppositePowerService.getSupportedGattServices();
                        if (theServices != null && theServices.size() > 0) {
                            oppositePowerDeviceData.updateDeviceDataGattServices(theServices);
                            // enable first characteristic to start the loop
                            enableNextCharacteristic(oppositePowerDeviceData, mBLEOppositePowerService);
                        } else {Log.e(logtag, "No OPPOSITE_POWER_GATT_SERVICES_DISCOVERED");}
                    }
                }, 100);
            } else if (ACTION_OPPOSITE_POWER_DATA_AVAILABLE.equals(action)) {
                newContent.put(DB_KEY_ACTIVE, 1);
                BLEDeviceType thisDeviceType = oppositePowerDeviceData.getDeviceType();
                String deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_OPPOSITE_POWER_ADDRESS);
                if (debugBLEPowerData) {Log.i(logtag, "ACTION_OPPOSITE_POWER_DATA_AVAILABLE - " + "data received: "
                        + intent.getStringExtra(EXTRA_POWER_DATA));}
                BLEDataType powerDataType = BLEDataType.valueOf(intent.getIntExtra(EXTRA_OPPOSITE_POWER_DATA_TYPE, BLEDataType.UNKNOWN.intValue()));
                if (debugBLEPowerData) {Log.i(logtag, "powerDataType: " + powerDataType.toString());}
                if (powerDataType == BIKE_POWER_MEAS) {
                    // use different SpeedCadenceCounts calcPowerData for distributed power sensors
                    // only declare TRACKING if we're receiving instantaneousPower measurements
                    oppositePowerDeviceData.status = TRACKING;
                    myBikeStat.hasPower = true;
                    final int instantaneousPower = handlePowerData(intent, mBLEDeviceManager.oppositeCalcPowerData);
                    boolean hasPowerCadenceSensor = intent.getBooleanExtra(EXTRA_POWER_HAS_CAD, false);
                    myBikeStat.hasPowerCadence = hasPowerCadenceSensor;
                    if (hasPowerCadenceSensor) {
                        handlePowerCadenceEvent(intent, mBLEDeviceManager.oppositeCrankCadenceCnts);
                    }
                    if (!writingTrackRecord) {
                        // use prevPower to display average of last two power values
                        myBikeStat.setPrevPower(myBikeStat.getPower());
                        if (thisDeviceType == BIKE_LEFT_POWER_DEVICE) {
                            myBikeStat.setLeftPower(instantaneousPower);
                        } else if (thisDeviceType == BIKE_RIGHT_POWER_DEVICE) {
                            myBikeStat.setRightPower(instantaneousPower);
                        } else {
                            myBikeStat.setPower(instantaneousPower);
                        }
                        if (myBikeStat.getPower() > myBikeStat.getMaxPower()) {
                            myBikeStat.setMaxPower(myBikeStat.getPower());
                        }
                    }
                    // todo if distributed power, instantaneousPower is only half; how does this affect avgPower calc?
                    averageCalcPower(myBikeStat.getPower(),
                            (SystemClock.elapsedRealtime() - mBLEDeviceManager.oppositeCalcPowerData.ertOfLastEvent));
                    refreshPower();
                    mBLEDeviceManager.oppositeCalcPowerData.ertOfLastEvent = SystemClock.elapsedRealtime();
                    if (debugBLEPowerData || debugBLEPowerCal || debugBLEPowerCal) {Log.i(logtag, "Device_opposite_power_address: "
                            + deviceAddress);}
                    if (debugBLEPowerData) {Log.i(logtag, "received instantaneousPower: " + instantaneousPower);}
                } else if (powerDataType == BIKE_POWER_CONTROL_PT) {
                    int pcpOpCode = intent.getIntExtra(EXTRA_OPPOSITE_PCP_OPCODE, 0);
                    if (debugBLEPowerCrank || debugBLEPowerCal) {Log.i(logtag, "opposite pcpOpCode: " + pcpOpCode);}
                    if (pcpOpCode == CALIBRATE_POWER_OPCODE) {
                        handleCalibrationResult(context, intent, deviceAddress);
                    } else if (pcpOpCode == REQUEST_CRANK_LENGTH_OPCODE){
                        handleReadCrankLength(intent, oppositePowerDeviceData);
                    } else if (pcpOpCode == WRITE_CRANK_LENGTH_OPCODE) {
                        handleWriteCrankLength(context, intent, oppositePowerDeviceData);
                    }
                } else if (powerDataType == SENSOR_LOCATION) {
                    handleSensorLocation(intent, newContent, oppositePowerDeviceData);
                    mBLEDeviceManager.updateActiveBikeDeviceData(deviceAddress, newContent);
                    // also update connected BLEDeviceData; hrmDeviceData, powerDeviceData, speedDeviceData, cadDeviceData
                    oppositePowerDeviceData.setData(newContent);
                    enableNextCharacteristic(oppositePowerDeviceData, mBLEOppositePowerService);
                }  else if (powerDataType == BIKE_POWER_FEATURE) {
                    handleBikeFeature(intent, newContent, oppositePowerDeviceData);
                    mBLEDeviceManager.updateActiveBikeDeviceData(deviceAddress, newContent);
                    // also update connected BLEDeviceData; hrmDeviceData, powerDeviceData, speedDeviceData, cadDeviceData
                    oppositePowerDeviceData.setData(newContent);
                    enableNextCharacteristic(oppositePowerDeviceData, mBLEOppositePowerService);
                } else {
                    handleBLEDeviceInformation(intent, newContent, powerDataType, EXTRA_POWER_DATA);
                    mBLEDeviceManager.updateActiveBikeDeviceData(deviceAddress, newContent);
                    // also update connected BLEDeviceData; hrmDeviceData, powerDeviceData, speedDeviceData, cadDeviceData
                    oppositePowerDeviceData.setData(newContent);
                    // can only enable one Characteristic at a time
                    enableNextCharacteristic(oppositePowerDeviceData, mBLEOppositePowerService);
                }
            }
        }

    };

    private int handlePowerData(Intent intent, SpeedCadenceCounts calcPowerData) {
        final int instantaneousPower = intent.getIntExtra(EXTRA_POWER_IPOWER, 0);
        // ertTimeStamp is used to see if Power Sensor is still sending data. SensorWatchdog waits 3 minutes and disconnects Service.
        calcPowerData.ertTimeStamp = SystemClock.elapsedRealtime();
        calcPowerData.isDataCurrent = true;
        if (!calcPowerData.initialized) {
            // time-tag the latest data for checking sensorDataCurrent
            calcPowerData.ertTimeStampNewData = SystemClock.elapsedRealtime();
            // used to calculate deltaT for average power measurement
            calcPowerData.ertOfLastEvent = SystemClock.elapsedRealtime();
            calcPowerData.initialized = true;
        }
        // test that this power is different than the last data; if so, set .ertNewData to currentTime.
        // Then record prevCount
        if (instantaneousPower != calcPowerData.prevCount) {
            // time-tag the latest data for checking sensorDataCurrent
            calcPowerData.ertTimeStampNewData = SystemClock.elapsedRealtime();
            calcPowerData.prevCount = instantaneousPower;
        }
        return instantaneousPower;
    }

    private void averageCalcCrankCad(int intValue) {
        if (intValue > 0) {// don't average zeros
            mBLEDeviceManager.addNumCalcCrankCad();
            mBLEDeviceManager.addTotalCalcCrankCad(intValue);
            myBikeStat.setAvgCadence((int) (mBLEDeviceManager.getTotalCalcCrankCad() / (mBLEDeviceManager.getNumCalcCrankCad() + 1)));
        }
    }

    private void averageCalcPower(final long currCount, final long deltaT) {
        // Now accumulate energy and pedaling time to calculate average power.
        // PowerTap only sends wheel_torque_data, not Power_only_data. Stages
        // sends crank_torque_data. When coasting, Stages doesn't send zeros; if
        // coasting for longer than MAX_DELTAT we won't keep adding the last
        // power value sent

        if ((currCount > 0.) && (deltaT > MIN_DELTAT) && (deltaT < MAX_DELTAT)) {
            mBLEDeviceManager.addCumEnergy(currCount * deltaT / msecPerSec);
            mBLEDeviceManager.addCumPowerTime(deltaT / msecPerSec);
            if (!writingTrackRecord) {
                myBikeStat.setAvgPower((int) (mBLEDeviceManager.getCumEnergy() / (mBLEDeviceManager.getCumPowerTime() + 1)));
            }
        }
    }

    private void handleCalibrationResult(Context context, Intent intent, String deviceAddress) {
        if (debugBLEPowerCal) {Log.i(logtag, "handleCalibrationResult: " + deviceAddress);}
        int toastTextColor = ContextCompat.getColor(context, R.color.gpsred);
        String toastText = getResources().getString(R.string.power_calibration_failed);
        int responseValue = intent.getIntExtra(EXTRA_RESPONSE_VALUE, RESPONSE_VALUE_FAILURE);
        if (responseValue == RESPONSE_VALUE_SUCCESS) {
            int calData = intent.getIntExtra(EXTRA_POWER_CAL_VALUE, 0);
            toastText = String.format(getResources().getString(R.string.power_calibration_success_value_), calData);
            toastTextColor = ContextCompat.getColor(context, R.color.gpsgreen);
            // save cal data
            ContentValues content = new ContentValues();
            content.put(DB_KEY_POWER_CAL, String.valueOf(calData));
            mBLEDeviceManager.updateActiveBikeDeviceData(deviceAddress, content);
        }
        viewToast(toastText, 40, BLE_TOAST_GRAVITY, toastAnchor, toastTextColor);
    }

    private void handleWriteCrankLength(Context context, Intent intent, BLEDeviceData theDeviceData) {
        if (debugBLEPowerCrank) {Log.i(logtag, "handleWriteCrankLength: " + theDeviceData.getAddress());}
        int responseValue = intent.getIntExtra(EXTRA_RESPONSE_VALUE, RESPONSE_VALUE_FAILURE);
        if (responseValue == RESPONSE_VALUE_SUCCESS) {
            // on success, don't need to write crank length again
            theDeviceData.shouldWriteCrankLength = false;
            // read again, for debugging
            if (debugBLEPowerCrank) {theDeviceData.shouldReadCrankLength = true;}
            // show crank length result to user
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            double mCrankLength = Double.valueOf(settings.getString(CRANK_LENGTH, String.valueOf(DEFAULT_CRANK_LENGTH)));
            String toastText = String.format(getResources().getString(R.string.crank_length_write_value), String.format(FORMAT_4_1F, mCrankLength));
            int toastTextColor = ContextCompat.getColor(context, R.color.gpsgreen);
            viewToast(toastText, 40, BLE_TOAST_GRAVITY, toastAnchor, toastTextColor);
            if (debugBLEPowerCrank) {
                Log.i(logtag, "sharedPref crank length: " + String.format(FORMAT_4_1F, mCrankLength));
            }
        }
    }

    private void handleReadCrankLength(Intent intent, BLEDeviceData theDeviceData) {
        if (debugBLEPowerCrank) {Log.i(logtag, "handleReadCrankLength: " + theDeviceData.getAddress());}
        int responseValue = intent.getIntExtra(EXTRA_RESPONSE_VALUE, RESPONSE_VALUE_FAILURE);
        if (responseValue == RESPONSE_VALUE_SUCCESS) {
            // on success, don't need to read crank length again
            theDeviceData.shouldReadCrankLength = false;
            int crankLength = intent.getIntExtra(EXTRA_POWER_CRANK_LENGTH, 0);
            if (theDeviceData.getDeviceType() == BIKE_LEFT_POWER_DEVICE){
                myBikeStat.setLeftCrankLength(crankLength);
            } else {
                myBikeStat.setRightCrankLength(crankLength);
            }
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            double mCrankLength = Double.valueOf(settings.getString(CRANK_LENGTH, String.valueOf(DEFAULT_CRANK_LENGTH)));
            // crankLength read from power sensor is twice physical length in mm because precision is 0.5 mm
            theDeviceData.shouldWriteCrankLength = Math.abs((mCrankLength - crankLength / 2.)) >= .5;
            if (debugBLEPowerCrank) {
                Log.i(logtag, "sharedPref crank length: " + String.format(FORMAT_4_1F, mCrankLength));
                Log.i(logtag, "received crank length data/2: " + String.format(FORMAT_4_1F, crankLength / 2.));
                Log.i(logtag, "shouldWriteCrankLength: " + (theDeviceData.shouldWriteCrankLength ? "yes" : "no"));
            }
        }
    }

    private void handlePowerCadenceEvent(Intent intent, SpeedCadenceCounts crankCadenceCnts) {
        crankCadenceCnts.ertTimeStamp = SystemClock.elapsedRealtime();
        crankCadenceCnts.isDataCurrent = true;
        crankCadenceCnts.prevCount = crankCadenceCnts.currCount;
        crankCadenceCnts.currCount = intent.getIntExtra(EXTRA_POWER_CRANK_REVS, 0);
        crankCadenceCnts.prevTime = crankCadenceCnts.currTime;
        crankCadenceCnts.currTime = intent.getIntExtra(EXTRA_POWER_CRANK_REV_TIME, 0);
        if (debugBLEPowerCadence) {Log.i(logtag, "ACTION_POWER-CADENCE_DATA_AVAILABLE - "
                + "data received: rev - " + intent.getIntExtra(EXTRA_POWER_CRANK_REVS, 0)
                + " data received: time - " + intent.getIntExtra(EXTRA_POWER_CRANK_REV_TIME, 0));}
        // don't calculate cadence if this is the first value. deltaCrankRevs could be anything
        if (crankCadenceCnts.initialized) {
            long deltaCrankRevs = crankCadenceCnts.currCount - crankCadenceCnts.prevCount;
            //crank rev data rolls over at 65535(16 bits)
            if (deltaCrankRevs < 0) {
                deltaCrankRevs += 0xffff;
            }
            long deltaCrankTime = (crankCadenceCnts.currTime - crankCadenceCnts.prevTime);
            //crank time data rolls over at 65535(16 bits)
            if (deltaCrankTime < 0) {
                deltaCrankTime += 0xffff;
            }
            // crankDeltaTime is in seconds, want it in minutes to get rpm
            double crankDeltaTime = (deltaCrankTime / POWER_CRANK_TIME_RESOLUTION) / SECONDS_PER_MINUTE;
            if (crankDeltaTime > 0) {
                crankCadenceCnts.ertTimeStampNewData = SystemClock.elapsedRealtime();
                crankCadenceCnts.isDataNew = true;
                double powerCrankCadence = deltaCrankRevs / crankDeltaTime;
                myBikeStat.setPowerCadence((int) powerCrankCadence);
                if (debugBLEPowerCadence) {Log.i(logtag, "received crank cadence (rpm): " + powerCrankCadence);}
                refreshCadence();
                averageCalcCrankCad(myBikeStat.getPowerCadence());
            }
        } else {
            crankCadenceCnts.initialized = true;
            crankCadenceCnts.ertTimeStampNewData = SystemClock.elapsedRealtime();
        }
    }
}
