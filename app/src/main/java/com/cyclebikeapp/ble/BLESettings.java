package com.cyclebikeapp.ble;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import static com.cyclebikeapp.ble.Constants.BLESETTINGS_TYPE_CAL;
import static com.cyclebikeapp.ble.Constants.BLESETTINGS_TYPE_SEARCH_PAIR;
import static com.cyclebikeapp.ble.Constants.CRANK_LENGTH;
import static com.cyclebikeapp.ble.Constants.DEFAULT_CRANK_LENGTH;
import static com.cyclebikeapp.ble.Constants.DEFAULT_WHEEL_CIRCUM;
import static com.cyclebikeapp.ble.Constants.FORMAT_4_1F;
import static com.cyclebikeapp.ble.Constants.FORMAT_4_3F;
import static com.cyclebikeapp.ble.Constants.KEY_AUTO_CONNECT_ALL;
import static com.cyclebikeapp.ble.Constants.KEY_CAL_CHANNEL;
import static com.cyclebikeapp.ble.Constants.KEY_CHOOSER_CODE;
import static com.cyclebikeapp.ble.Constants.KEY_PAIR_CHANNEL;
import static com.cyclebikeapp.ble.Constants.LOWER_WHEEL_CIRCUM;
import static com.cyclebikeapp.ble.Constants.POWER_WHEEL_CIRCUM;
import static com.cyclebikeapp.ble.Constants.POWER_WHEEL_IS_CAL;
import static com.cyclebikeapp.ble.Constants.PREFS_NAME;
import static com.cyclebikeapp.ble.Constants.RC_BLE_DEVICE_EDIT;
import static com.cyclebikeapp.ble.Constants.SHOW_BLE;
import static com.cyclebikeapp.ble.Constants.UPPER_WHEEL_CIRCUM;
import static com.cyclebikeapp.ble.Constants.USE_BLE;
import static com.cyclebikeapp.ble.Constants.WHEEL_CIRCUM;
import static com.cyclebikeapp.ble.Constants.WHEEL_IS_CAL;

public class BLESettings extends AppCompatActivity {
	
	private EditText wheelEdit;
    private EditText crankEdit;
	private CheckBox useBLECheck;
	private CheckBox showBLECheck;
	private CheckBox autoConnectCheck;
	private TextView autoConnectText;
	private Double wheelCirc;
    private Double crankLength;
	private boolean useBLE;
	private boolean showBLE;
	private BLEDeviceManager mBLEManager;
	private boolean autoConnectBLEAll = true;
    private final Intent bleSettingsIntent = new Intent();
	private static final boolean debugAppState = MainActivity.debugAppState;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (debugAppState) {Log.i(this.getClass().getName(), "onCreate()");}
		super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_settings_view);
        bleSettingsIntent.putExtra(KEY_CHOOSER_CODE, 0);
        setResult(RESULT_OK, bleSettingsIntent);
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
        editor.putInt(KEY_CHOOSER_CODE, 0).apply();
		mBLEManager = new BLEDeviceManager(getApplicationContext());
		setupActionBar();
		getWidgetIDs();
		loadPreferences();
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	private void setupActionBar() {
		android.support.v7.app.ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(getString(R.string.menu_ble));
			actionBar.show();
		}
	}
	private void getWidgetIDs() {
		Button aboutANTButton = findViewById(R.id.ble_help_btn);
		aboutANTButton.setOnClickListener(aboutBLEButtonClickListener);
		Button antManagerButton = findViewById(R.id.ble_manager_btn);
		antManagerButton.setOnClickListener(bleManagerButtonClickListener);
		Button trainerModeButton = findViewById(R.id.trainer_mode_btn);
		trainerModeButton.setOnClickListener(trainerModeButtonClickListener);
        TextView wheelEditTitle = findViewById(R.id.wheel_edit_title);
        if (Utilities.isScreenWidthSmall(getApplicationContext())){
		    wheelEditTitle.setTextSize(14);
        }
        TextView crankEditTitle = findViewById(R.id.crank_edit_title);
        if (Utilities.isScreenWidthSmall(getApplicationContext())){
            crankEditTitle.setTextSize(14);
        }
        wheelEdit = findViewById(R.id.wheel_edit);
        crankEdit = findViewById(R.id.crank_edit);
        if (Utilities.isScreenWidthSmall(getApplicationContext())){
            crankEdit.setTextSize(14);
        }
        if (Utilities.isScreenWidthSmall(getApplicationContext())){
            wheelEdit.setTextSize(14);
        }

		useBLECheck = findViewById(R.id.use_ble_checkbox);
		showBLECheck = findViewById(R.id.show_ble_checkbox);
		autoConnectCheck = findViewById(R.id.autoConnect_ble_checkbox);
		autoConnectCheck.setOnClickListener(autoConnectCheckOnClick);
		autoConnectText = findViewById(R.id.autoconnect_text);
		useBLECheck.setOnClickListener(useBLEOnClick);
		showBLECheck.setOnClickListener(showBLEOnClick);
  	}

	@SuppressLint("DefaultLocale")
	private void loadPreferences() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		useBLE = settings.getBoolean(USE_BLE, true);
		showBLE = settings.getBoolean(SHOW_BLE, true);
        useBLECheck.setChecked(useBLE);
        showBLECheck.setChecked(showBLE);
        autoConnectBLEAll = settings.getBoolean(KEY_AUTO_CONNECT_ALL, false);
		autoConnectText.setText(getAutoConnectCheckText(autoConnectBLEAll));
		wheelCirc = Double.valueOf(settings.getString(WHEEL_CIRCUM, String.valueOf(DEFAULT_WHEEL_CIRCUM)));
		wheelEdit.setText(String.format(FORMAT_4_3F, wheelCirc));
        crankLength = Double.valueOf(settings.getString(CRANK_LENGTH, String.valueOf(DEFAULT_CRANK_LENGTH)));
        crankEdit.setText(String.format(FORMAT_4_1F, crankLength));
		if (!useBLE) {
			showBLECheck.setChecked(false);
            showBLE = useBLE;
            showBLECheck.setEnabled(false);
		}
        autoConnectCheck.setChecked(autoConnectBLEAll);
		loadBLEConfiguration();
		if (mBLEManager.wheelCnts.isCalibrated) {
			wheelCirc = mBLEManager.wheelCnts.wheelCircumference;
		} else if (mBLEManager.powerWheelCnts.isCalibrated) {
			wheelCirc = mBLEManager.powerWheelCnts.wheelCircumference;
		}
	}
	
	private CharSequence getAutoConnectCheckText(boolean autoConnectBLEAll2) {
		String connectAny = getString(R.string.ble_autoconnect_any);
		String connectLast = getString(R.string.ble_autoconnect_last);
		if (autoConnectBLEAll2) {
			return connectAny;
		} else {
			return connectLast;
		}
	}

	private void loadBLEConfiguration() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		mBLEManager.wheelCnts.wheelCircumference = Double.valueOf(settings
				.getString(WHEEL_CIRCUM, String.valueOf(DEFAULT_WHEEL_CIRCUM)));
		mBLEManager.powerWheelCnts.wheelCircumference = Double.valueOf(settings
				.getString(POWER_WHEEL_CIRCUM, String.valueOf(DEFAULT_WHEEL_CIRCUM)));
		mBLEManager.wheelCnts.isCalibrated = settings.getBoolean(WHEEL_IS_CAL, false);
		mBLEManager.powerWheelCnts.isCalibrated = settings.getBoolean(POWER_WHEEL_IS_CAL, false);
	}

	@Override
	protected void onPause() {
		saveState();
		super.onPause();
	}

	private void saveState() {
		//only save the values that might change in this activity
		String wheelCircumference = wheelEdit.getText().toString();
		useBLE = useBLECheck.isChecked();
		showBLE = showBLECheck.isChecked();
        // We need an Editor object to make preference changes.
		// All objects are from android.context.Context
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		double newWheelCirc;
	    try {
			newWheelCirc = Double.valueOf(wheelCircumference.replaceAll(",", "."));
	    } catch (NumberFormatException e) {
	        newWheelCirc = DEFAULT_WHEEL_CIRCUM;
	    }
		if (newWheelCirc > UPPER_WHEEL_CIRCUM) {
			newWheelCirc = UPPER_WHEEL_CIRCUM;
		}
        if (newWheelCirc < LOWER_WHEEL_CIRCUM) {
            newWheelCirc = LOWER_WHEEL_CIRCUM;
        }
		wheelCircumference = String.valueOf(newWheelCirc);
		boolean wheelCircChanged = Math.abs(newWheelCirc - wheelCirc) > .01;
		if (wheelCircChanged) {
			editor.putBoolean(WHEEL_IS_CAL, false);
			editor.putString(WHEEL_CIRCUM, wheelCircumference);
			editor.putBoolean(POWER_WHEEL_IS_CAL, false);
			editor.putString(POWER_WHEEL_CIRCUM, wheelCircumference);
		}
        double newCrankLength;
        String tempCrankLength = crankEdit.getText().toString();
        try {
            newCrankLength = Double.valueOf(tempCrankLength.replaceAll(",", "."));
        } catch (NumberFormatException e) {
            newCrankLength = DEFAULT_CRANK_LENGTH;
        }
        tempCrankLength = String.valueOf(newCrankLength);
        boolean crankLengthChanged = Math.abs(newCrankLength - crankLength) > .5;
        if (crankLengthChanged) {
            editor.putString(CRANK_LENGTH, tempCrankLength);
        }
		editor.putBoolean(USE_BLE, useBLE);
		editor.putBoolean(SHOW_BLE, showBLE);
		editor.putBoolean(KEY_AUTO_CONNECT_ALL, autoConnectBLEAll);
		editor.apply();
	}
	
	private final OnClickListener aboutBLEButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent i11 = new Intent(BLESettings.this, AboutBLEScroller.class);
			startActivity(i11);			
		}
	};
	private final OnClickListener bleManagerButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent i111 = new Intent(BLESettings.this, BLEDeviceEditor.class);
			startActivityForResult(i111, RC_BLE_DEVICE_EDIT);
			// show expanded list of BLE devices with search... button
		}
	};		
	
	private final OnClickListener autoConnectCheckOnClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			autoConnectBLEAll = autoConnectCheck.isChecked();
			autoConnectText.setText(getAutoConnectCheckText(autoConnectBLEAll));
		}
	};		

	private final OnClickListener useBLEOnClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			useBLE = useBLECheck.isChecked();
			if (!useBLE){
				showBLECheck.setChecked(false);
				showBLE = useBLE;
				showBLECheck.setEnabled(false);
			} else {
				showBLECheck.setEnabled(true);
			}
		}
	};		

	private final OnClickListener showBLEOnClick = v -> {
    };

    private final OnClickListener trainerModeButtonClickListener = v -> {
        Intent i111 = new Intent(BLESettings.this, TrainerModeSettings.class);
        startActivity(i111);
        // show trainer mode layout with about textbox, check box, spoof location spinner
    };

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		switch (requestCode) {
		case RC_BLE_DEVICE_EDIT:
			int chooserCode = intent.getIntExtra(KEY_CHOOSER_CODE, 0);
			if ((chooserCode == BLESETTINGS_TYPE_SEARCH_PAIR
					|| chooserCode == BLESETTINGS_TYPE_CAL)
					&& resultCode == RESULT_OK) {
				SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
				editor.putInt(KEY_CHOOSER_CODE, chooserCode);
				editor.putInt(KEY_PAIR_CHANNEL, intent.getIntExtra(KEY_PAIR_CHANNEL, 0));
				editor.putString(KEY_CAL_CHANNEL, intent.getStringExtra(KEY_CAL_CHANNEL)).apply();

				bleSettingsIntent.putExtra(KEY_CHOOSER_CODE, chooserCode);
				bleSettingsIntent.putExtra(KEY_PAIR_CHANNEL, intent.getIntExtra(KEY_PAIR_CHANNEL, 0));
				bleSettingsIntent.putExtra(KEY_CAL_CHANNEL, intent.getStringExtra(KEY_CAL_CHANNEL));
				setResult(RESULT_OK, bleSettingsIntent);
				finish();
			}
			break;
		}
	}

}
