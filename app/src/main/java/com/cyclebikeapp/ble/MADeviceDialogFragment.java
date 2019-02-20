package com.cyclebikeapp.ble;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.location.LocationManager;
import android.os.Bundle;

import static android.content.Context.LOCATION_SERVICE;

/**
 * Created by TommyD on 12/1/2017.
 */

public class MADeviceDialogFragment extends DialogFragment {

    static final String FORCE_XTRA_INJECTION = "force_xtra_injection";
    static final String FORCE_TIME_INJECTION = "force_time_injection";
    static final String DELETE_AIDING_DATA = "delete_aiding_data";
    static final String GPS = "gps";
    static final String MAIN = "Main";
    static final String DEVICE_EDITOR = "DeviceEditor";
    static final String BATTERY_OPTIMIZATION_SETTINGS = "batteryOptimizationSettings";
    static final String MAIN_GPS = "Main-GPS";
    static final String DDF_KEY_CALLINGTAG = "DDF_key_callingtag";

    public static MADeviceDialogFragment newInstance(Bundle b) {
        MADeviceDialogFragment frag = new MADeviceDialogFragment();
        String callingTag = frag.getTag();
        b.putCharSequence(DDF_KEY_CALLINGTAG, callingTag);
        frag.setArguments(b);

        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString(Constants.DDF_KEY_TITLE);
        String dialogMessage = getArguments().getString(Constants.DDF_KEY_MESSAGE);
        String devAddress = getArguments().getString(Constants.DDF_KEY_ADDRESS);
        int devType = getArguments().getInt(Constants.DDF_KEY_DEVICE_TYPE);

        boolean deviceIsPowerDevice = (devType == BLEDeviceType.BIKE_POWER_DEVICE.intValue()
                || devType == BLEDeviceType.BIKE_LEFT_POWER_DEVICE.intValue()
                || devType == BLEDeviceType.BIKE_RIGHT_POWER_DEVICE.intValue());
        boolean deviceActive = getArguments().getBoolean(Constants.DDF_KEY_DEVICE_ACTIVE);
        AlertDialog.Builder mDialog = new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(dialogMessage)
                .setCancelable(true)
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (getTag().contains(MAIN)){
                                    ((MainActivity) getActivity()).doCancelClick();
                                } else if (getTag()
                                        .contains(DEVICE_EDITOR)) {
                                    ((BLEDeviceEditor) getActivity()).doCancelClick();
                                }
                            }
                        }
                );
        if (getTag().contains(BATTERY_OPTIMIZATION_SETTINGS)){
            mDialog.setPositiveButton(R.string.battery_settings,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ((MainActivity) getActivity()).doShowBatterySettings();
                        }
                    }
            );
        }
        // don't show forget button if this is the GPS dialog
         else if (!getTag().equals(MAIN_GPS)){
            mDialog.setPositiveButton(R.string.forget,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (getTag().equals(MAIN)){
                                ((MainActivity) getActivity()).doForgetClick(devAddress, devType);
                            } else {
                                ((BLEDeviceEditor) getActivity()).doForgetClick(devAddress);
                            }
                        }
                    }
            );
            // show reset button if this is the GPS dialog
        } else {
            mDialog.setPositiveButton(R.string.reset_gps, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    LocationManager mLocationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
                    try {
                        if (mLocationManager != null) {
                            Bundle bundle = new Bundle();
                            mLocationManager.sendExtraCommand(GPS, FORCE_XTRA_INJECTION, bundle);
                            mLocationManager.sendExtraCommand(GPS, FORCE_TIME_INJECTION, bundle);
                            mLocationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, DELETE_AIDING_DATA, null);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        if (deviceActive && deviceIsPowerDevice) {
            mDialog.setNeutralButton(R.string.calibrate,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (getTag().equals(MAIN)){
                                ((MainActivity) getActivity()).doCalibratePower(devAddress);
                            } else {
                                ((BLEDeviceEditor) getActivity()).doCalibrate(devAddress);
                            }
                        }
                    });

        }

        return mDialog.create();
    }

}

