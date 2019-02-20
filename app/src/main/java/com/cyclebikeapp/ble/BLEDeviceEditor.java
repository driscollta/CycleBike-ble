package com.cyclebikeapp.ble;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;

import java.util.ArrayList;

import static com.cyclebikeapp.ble.BLEUtilities.composeDeviceDialogMessage;
import static com.cyclebikeapp.ble.BLEUtilities.composeDeviceDialogTitle;
import static com.cyclebikeapp.ble.Constants.BLESETTINGS_TYPE_CAL;
import static com.cyclebikeapp.ble.Constants.BLESETTINGS_TYPE_SEARCH_PAIR;
import static com.cyclebikeapp.ble.Constants.DB_KEY_ACTIVE;
import static com.cyclebikeapp.ble.Constants.DB_KEY_DEV_TYPE;
import static com.cyclebikeapp.ble.Constants.DDF_KEY_ADDRESS;
import static com.cyclebikeapp.ble.Constants.DDF_KEY_DEVICE_ACTIVE;
import static com.cyclebikeapp.ble.Constants.DDF_KEY_DEVICE_TYPE;
import static com.cyclebikeapp.ble.Constants.DDF_KEY_MESSAGE;
import static com.cyclebikeapp.ble.Constants.DDF_KEY_TITLE;
import static com.cyclebikeapp.ble.Constants.KEY_CAL_CHANNEL;
import static com.cyclebikeapp.ble.Constants.KEY_CHOOSER_CODE;
import static com.cyclebikeapp.ble.Constants.KEY_PAIR_CHANNEL;
import static com.cyclebikeapp.ble.Constants.PREFS_NAME;

public class BLEDeviceEditor extends ExpandableListActivity {
	private final ArrayList<String> groupItem = new ArrayList<>();
	private final ArrayList<ArrayList<String>> childItem = new ArrayList<>();
	private static final String KEY_EXPAND_GROUP_0 = "key_expand_group0";
	private static final String KEY_EXPAND_GROUP_1 = "key_expand_group1";
	private static final String KEY_EXPAND_GROUP_2 = "key_expand_group2";
	private static final String KEY_EXPAND_GROUP_3 = "key_expand_group3";
	private static final String KEY_EXPAND_GROUP_4 = "key_expand_group4";
	private static final String[]  KEY_EXPAND_GROUP = {KEY_EXPAND_GROUP_0, KEY_EXPAND_GROUP_1,
		KEY_EXPAND_GROUP_2, KEY_EXPAND_GROUP_3, KEY_EXPAND_GROUP_4};
	private BLEDBAdapter dataBaseAdapter = null;
	private ExpListAdapter mExpAdapter;
	private ExpandableListView expList;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			dataBaseAdapter = new BLEDBAdapter(getApplicationContext());
			dataBaseAdapter.open();
		} catch(SQLiteException e){
			e.printStackTrace();
		}
        getWindow().getDecorView().setBackgroundColor(ContextCompat.getColor(this, R.color.bkgnd_black));
		expList = getExpandableListView();
		expList.setDividerHeight(2);
		expList.setGroupIndicator(null);
		expList.setClickable(true);
		setGroupData();
		setChildGroupData();
	    mExpAdapter = new ExpListAdapter(groupItem, childItem);
		mExpAdapter.setInflater(
						(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE),this);
		expList.setAdapter(mExpAdapter);
		restoreExpListState();
		expList.setOnGroupClickListener(new OnGroupClickListener() {

			@Override
			public boolean onGroupClick(ExpandableListView parent,
					View v, int groupPosition, long id) {
                if (MainActivity.debugAppState) Log.i(this.getClass().getName(), "onGroupClick() group: " + groupPosition);
				// this just expands the list to show device names
				// store this value in SharedPrefs
				saveState();
				return false;
			}
		});
		expList.setOnChildClickListener(new OnChildClickListener() {
			
			@Override
		    public boolean onChildClick(ExpandableListView parent, View v,
		                int groupPosition, int childPosition, long id) {
                if (MainActivity.debugAppState) Log.i(this.getClass().getName(), "onChildClick() child: " + childPosition
                 + " group: " + groupPosition);
				if (childPosition == 0) {
					doSearch(groupPosition);
					// should return to DisplayActivity and show search progress
					//groupPosition 0 is heart_rate_sensor
					// 1 power_sensor
					// 2 speed_sensor
					// 3 cadence_sensor
					// 4 speed_cadence_sensor
				} else {
					String deviceName = childItem.get(groupPosition).get(childPosition);
					if (dataBaseAdapter != null) {
						String deviceAddress = dataBaseAdapter.fetchDeviceAddressByName(deviceName, groupPosition);
						showDialog(deviceAddress, dataBaseAdapter);
					}
				}
				return true;
		    }
		});
	}

	private void restoreExpListState() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		// restore exp list state of groups expanded or collapsed
		int groupNum = 0;
		boolean[] expand = {
		settings.getBoolean(KEY_EXPAND_GROUP_0, false),
		settings.getBoolean(KEY_EXPAND_GROUP_1, false),
		settings.getBoolean(KEY_EXPAND_GROUP_2, false),
		settings.getBoolean(KEY_EXPAND_GROUP_3, false),
		settings.getBoolean(KEY_EXPAND_GROUP_4, false)};		
		for (boolean open:expand) {
			if (open) {
				expList.expandGroup(groupNum);
			} else {
				expList.collapseGroup(groupNum);
			}
			groupNum++;
		}
	}

    public void doForgetClick(String deviceAddress) {
        // Do stuff here.
        doForget(deviceAddress);
        //Log.i("FragAlertDialogDevEd", "Forget click!");
    }

    public void doCancelClick() {
        // Do stuff here.
        //Log.i("FragAlertDialogDevEd", "Cancel click!");
    }
    private void showDialog(String deviceAddress, BLEDBAdapter dataBaseAdapter) {
        if (MainActivity.debugAppState) Log.i(this.getClass().getName(), "showDialog()");

        Bundle dialogBundle = new Bundle();
        int devType = BLEDeviceType.UNKNOWN_DEVICE.intValue();
        Cursor mCursor = null;
        try {
            mCursor = dataBaseAdapter.fetchDeviceData(deviceAddress);
            if (mCursor != null) {
                boolean deviceActive = mCursor.getInt(mCursor.getColumnIndexOrThrow(DB_KEY_ACTIVE)) == 1;
                dialogBundle.putBoolean(DDF_KEY_DEVICE_ACTIVE, deviceActive);
                devType = mCursor.getInt(mCursor.getColumnIndexOrThrow(DB_KEY_DEV_TYPE));
            }
        } catch (IllegalArgumentException e) {
            Log.e(this.getClass().getName(), "IllegalArgumentException - " + e.toString());
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        String title = composeDeviceDialogTitle(this, BLEDeviceType.valueOf(devType));
        dialogBundle.putCharSequence(DDF_KEY_TITLE, title);
        dialogBundle.putCharSequence(DDF_KEY_ADDRESS, deviceAddress);
        dialogBundle.putInt(DDF_KEY_DEVICE_TYPE, devType);
        dialogBundle.putCharSequence(DDF_KEY_MESSAGE, composeDeviceDialogMessage(this, deviceAddress, dataBaseAdapter));
        MADeviceDialogFragment newFragment = MADeviceDialogFragment.newInstance(dialogBundle);
        newFragment.show(getFragmentManager(), "DeviceEditor");
    }

	/**
	 * Populate the groupItem ArrayList with titles of the device types
	 */
	private void setGroupData() {
		groupItem.add(getString(R.string._heart_rate_sensor));
		groupItem.add(getString(R.string._power_sensor));
		groupItem.add(getString(R.string._speed_sensor));
		groupItem.add(getString(R.string._cadence_sensor));
		groupItem.add(getString(R.string._speed_cadence_sensor));
	}

	/**
	 * Populate the childItem ArrayLists with all known device names for each
	 * device type from the database
	 */
	private void setChildGroupData() {
		childItem.clear();
		//Add Data For HRM
		try {
			ArrayList<String> child;
			BLEDeviceType deviceType = BLEDeviceType.HEARTRATE_DEVICE;
			child = dataBaseAdapter.getAllDeviceNames(deviceType);
			childItem.add(child);

			//Add Data For Power
			deviceType = BLEDeviceType.BIKE_POWER_DEVICE;
			child = dataBaseAdapter.getAllDeviceNames(deviceType);
			childItem.add(child);

			//Add Data For Speed
			deviceType = BLEDeviceType.BIKE_SPD_DEVICE;
			child = dataBaseAdapter.getAllDeviceNames(deviceType);
			childItem.add(child);

			//Add Data For Cadence
			deviceType = BLEDeviceType.BIKE_CADENCE_DEVICE;
			child = dataBaseAdapter.getAllDeviceNames(deviceType);
			childItem.add(child);

			//Add Data For Speed&Cadence
			deviceType = BLEDeviceType.BIKE_SPDCAD_DEVICE;
			child = dataBaseAdapter.getAllDeviceNames(deviceType);
			childItem.add(child);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			Log.e(this.getClass().getName(), "IllegalArgumentException in setChildGroupData()");
		}
	}
	
	/**
	 * When user presses the Calibrate button in the device List
	 * @param deviceAddress the device number to calibrate
	 */
    void doCalibrate(final String deviceAddress) {
		if (MainActivity.debugAppState) Log.i(this.getClass().getName(), "doCalibrate()");
		Intent intent = getIntent();
		intent.putExtra(KEY_CHOOSER_CODE, BLESETTINGS_TYPE_CAL);
		intent.putExtra(KEY_CAL_CHANNEL, deviceAddress);
		setResult(RESULT_OK, intent);
		finish();
		//should return to DisplayActivity thru onResume and do Calibrate
	}

	/**
	 * When user presses the "Forget" button on a device page, call the doForget
	 * method and re-build the device list child items
	 * @param deviceAddress the device to forget
	 */
	private void doForget(String deviceAddress) {
		if (dataBaseAdapter != null) {
			dataBaseAdapter.doForget(deviceAddress);
		}
		// re-build childItems
		setChildGroupData();
	    mExpAdapter = new ExpListAdapter(groupItem, childItem);
		mExpAdapter.setInflater(
						(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE),this);
		expList.setAdapter(mExpAdapter);
		restoreExpListState();
	}
	
	/**
	 * When user presses the Search/Pair button in the device List
	 * @param groupPosition the device type to search for
	 */
	private void doSearch(int groupPosition) {
		if (MainActivity.debugAppState) Log.i(this.getClass().getName(), "doSearch()");
		// return to DisplayActivity and Pair the channel
		Intent intent = getIntent();
		intent.putExtra(KEY_CHOOSER_CODE, BLESETTINGS_TYPE_SEARCH_PAIR);
		intent.putExtra(KEY_PAIR_CHANNEL, convertGroupPositionToDeviceType(groupPosition));
		setResult(RESULT_OK, intent);
		saveState();
		finish();
		// should return to DisplayActivity thru onResume and show search progress
	}// do Search

	/**
	 * User clicks on a group position in the expandable list, but we need to
	 * return the DeviceType (integer value) to main activity
	 * @param groupPosition user selection from expandable list
	 * @return corresponding DeviceType (integer value)
	 */
	private int convertGroupPositionToDeviceType(int groupPosition) {
//		static final int HRM_CHANNEL = 0;
//		static final int POWER_CHANNEL = 1;
//		static final int SPEED_CHANNEL = 2;
//		static final int CADENCE_CHANNEL = 3;
//		static final int SPEEDCADENCE_CHANNEL = 4;
		switch (groupPosition) {
		case 0:	
			return BLEDeviceType.HEARTRATE_DEVICE.intValue();
		case 1:	
			return BLEDeviceType.BIKE_POWER_DEVICE.intValue();
		case 2:	
			return BLEDeviceType.BIKE_SPD_DEVICE.intValue();
		case 3:	
			return BLEDeviceType.BIKE_CADENCE_DEVICE.intValue();
		case 4:	
			return BLEDeviceType.BIKE_SPDCAD_DEVICE.intValue();
		default:	
			return -1;
		}
	}

	@Override
	protected void onPause() {
		if (dataBaseAdapter != null) {
			dataBaseAdapter.close();
		}
		super.onPause();
	}
	
	private void saveState() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		for (int group = 0; group < 5; group++){
			editor.putBoolean(KEY_EXPAND_GROUP[group], expList.isGroupExpanded(group));
		}
		editor.apply();
	}
	
	@Override
	protected void onStop() {
		if (MainActivity.debugAppState) Log.i(this.getClass().getName(), "onStop()");
		saveState();
		super.onStop();
		Intent intent = new Intent();
		intent.putExtra(KEY_CHOOSER_CODE, 0);
		setResult(RESULT_OK, intent);
	}

	@Override
	public void onBackPressed() {
		if (MainActivity.debugAppState) Log.i(this.getClass().getName(), "onBackPressed()");
		saveState();
		if (dataBaseAdapter != null) {
			dataBaseAdapter.close();
		}
		super.onStop();
		Intent intent = new Intent();
		intent.putExtra(KEY_CHOOSER_CODE, 0);
		setResult(RESULT_OK, intent);
		finish();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    // Respond to the action bar's Up/Home button
	    case android.R.id.home:
			saveState();
			if (dataBaseAdapter != null) {
				dataBaseAdapter.close();
			}
	        NavUtils.navigateUpFromSameTask(this);
			this.overridePendingTransition(0, 0);
	        return true;
	    }
	    return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
