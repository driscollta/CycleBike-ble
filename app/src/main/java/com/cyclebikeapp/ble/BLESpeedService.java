/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyclebikeapp.ble;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT32;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_SPEED_DATA_AVAILABLE;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_SPEED_GATT_CONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_SPEED_GATT_DISCONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_SPEED_GATT_SERVICES_DISCOVERED;
import static com.cyclebikeapp.ble.BLEConstants.EXTRAS_DEVICE_SPEED_ADDRESS;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_CSC_DATA_TYPE;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_CSC_DEVICE_TYPE;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_REV_DATA;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_REV_TIME_DATA;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_SPEED_DATA;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_WHEEL_REVS;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_WHEEL_REV_TIME;
import static com.cyclebikeapp.ble.BLEConstants.UUID_CSC_MEASUREMENT;
import static com.cyclebikeapp.ble.LocationUpdatesService.EXTRA_STARTED_FROM_NOTIFICATION;
import static com.cyclebikeapp.ble.MainActivity.debugBLEData;
import static com.cyclebikeapp.ble.MainActivity.debugBLEService;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BLESpeedService extends Service {
    private final static String LOGTAG = BLESpeedService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private final Handler mHandler = new Handler();

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == STATE_CONNECTED) {
                intentAction = ACTION_SPEED_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                if (debugBLEService){Log.v(LOGTAG, "Connected to GATT server.");}
                // Attempts to discover services after successful connection.
                boolean discoveredServices = mBluetoothGatt.discoverServices();
                if (debugBLEService){Log.v(LOGTAG, "Attempting to start Speed measurement service discovery: "
                        + (discoveredServices ? "yes" : "no"));}
            } else if (newState == STATE_DISCONNECTED) {
                intentAction = ACTION_SPEED_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                if (debugBLEService){Log.w(LOGTAG, "Disconnected from GATT server.");}
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_SPEED_GATT_SERVICES_DISCOVERED);
            } else {
                if (debugBLEService){Log.w(LOGTAG, "onSpeedServicesDiscovered received: " + status);}
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_SPEED_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_SPEED_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRAS_DEVICE_SPEED_ADDRESS, mBluetoothDeviceAddress);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        int shortUUID = BLEUtilities.getShortUUID(characteristic);
        BLEDataType dataType = BLEDataType.valueOf(shortUUID);
        intent.putExtra(EXTRA_CSC_DATA_TYPE, shortUUID);
        switch (dataType) {
            case CSC_MEAS:
                final int CADENCE_MASK = 0x02;
                final int SPEED_MASK = 0x01;
                int cscDeviceType = BLEDeviceType.UNKNOWN_DEVICE.intValue();
                int flag = characteristic.getIntValue(FORMAT_UINT8, 0);
                boolean hasCadenceData = (flag & CADENCE_MASK) == CADENCE_MASK;
                boolean hasSpeedData = (flag & SPEED_MASK) == SPEED_MASK;
                if (hasCadenceData && hasSpeedData) {
                    cscDeviceType = BLEDeviceType.BIKE_SPDCAD_DEVICE.intValue();
                } else if (hasCadenceData) {
                    cscDeviceType = BLEDeviceType.BIKE_CADENCE_DEVICE.intValue();
                } else if (hasSpeedData) {
                    cscDeviceType = BLEDeviceType.BIKE_SPD_DEVICE.intValue();
                }
                intent.putExtra(EXTRAS_DEVICE_SPEED_ADDRESS, mBluetoothDeviceAddress);

                intent.putExtra(EXTRA_CSC_DEVICE_TYPE, cscDeviceType);
                if (debugBLEData) {
                    Log.i(LOGTAG, "hasCadenceData: " + (hasCadenceData ? "true" : "false")
                            + " hasSpeedData: " + (hasSpeedData ? "true" : "false"));
                }
                if (debugBLEData) {
                    Log.i(LOGTAG, "flag: " + flag);
                }
                int revFormat;
                int revOffset;
                int timeOffset;
                if (hasSpeedData) {
                    final byte[] speedData = characteristic.getValue();
                    if (speedData != null && speedData.length > 0) {
                        final StringBuilder stringBuilder = new StringBuilder(speedData.length);
                        for (byte byteChar : speedData) {
                            stringBuilder.append(String.format("%02X ", byteChar));
                        }
                        if (debugBLEData) {
                            Log.i(LOGTAG, "received byte hex data: " + stringBuilder.toString());
                        }
                    }
                    revFormat = FORMAT_UINT32;
                    //the flag byte is the first 8 bits, the rev data is offset by "1"
                    revOffset = 1;
                    // the time offset depends on the FORMAT of the rev data, plus 1 for the flag data
                    timeOffset = revOffset + 4;
                    final int wheelRevs = characteristic.getIntValue(revFormat, revOffset);
                    if (debugBLEData) {
                        Log.i(LOGTAG, String.format("Received speed revs: %d", wheelRevs));
                    }
                    // format: The format type used to interpret the characteristic value.
                    // timeOffset: int Offset at which the integer value can be found.
                    final int wheelTime = characteristic.getIntValue(FORMAT_UINT16, timeOffset);
                    if (debugBLEData) {
                        Log.i(LOGTAG, String.format("Received speed rev timestamp: %d", wheelTime));
                    }
                    intent.putExtra(EXTRA_WHEEL_REVS, wheelRevs);
                    intent.putExtra(EXTRA_WHEEL_REV_TIME, wheelTime);
                }
                if (hasCadenceData) {
                    revFormat = FORMAT_UINT16;
                    //the flag byte is the first 8 bits, the rev data is offset by "1"
                    // speed data, if present takes 4 bytes for revs plus 2 bytes for time
                    revOffset = 1 + (hasSpeedData ? 6 : 0);
                    // the time offset depends on the FORMAT of the rev data, plus 1 for the flag data
                    timeOffset = revOffset + 2;
                    final byte[] cadData = characteristic.getValue();
                    if (cadData != null && cadData.length > 0) {
                        final StringBuilder stringBuilder = new StringBuilder(cadData.length);
                        for (byte byteChar : cadData) {
                            stringBuilder.append(String.format("%02X ", byteChar));
                        }
                        if (debugBLEData) {
                            Log.i(LOGTAG, "received byte hex data: " + stringBuilder.toString());
                        }
                    }
                    // the Wahoo cadence sensor does not implement the .getProperties() function.
                    // Since this is a CadenceService, we must assume that the sensor is a Cadence sensor
                    // and the data format and offset will obey the Bluetooth specification
                    final int cumulativeRevs = characteristic.getIntValue(revFormat, revOffset);
                    if (debugBLEData) {
                        Log.i(LOGTAG, String.format("Received cadence revs: %d", cumulativeRevs));
                    }
                    intent.putExtra(EXTRA_REV_DATA, cumulativeRevs);
                    // format: The format type used to interpret the characteristic value.
                    // timeOffset: int Offset at which the integer value can be found.
                    final int revTimestamp = characteristic.getIntValue(FORMAT_UINT16, timeOffset);
                    if (debugBLEData) {
                        Log.i(LOGTAG, String.format("Received cadence rev timestamp: %d", revTimestamp));
                    }
                    intent.putExtra(EXTRA_REV_TIME_DATA, revTimestamp);
                }
                break;
            case DEVICE_NAME:
                if (debugBLEData) {
                    Log.i(LOGTAG, "Speed Device Name: " + characteristic.getStringValue(0));
                }
                intent.putExtra(EXTRA_SPEED_DATA, characteristic.getStringValue(0));
                break;
            case APPEARANCE:
                // since Wahoo did not implement APPEARANCE in their earlier firmware we cannot use this characteristic
            case SERVICE_CHANGED:
                break;
            case BATTERY_LEVEL:
                if (debugBLEData) {
                    Log.i(LOGTAG, "Speed Battery: " + characteristic.getIntValue(FORMAT_UINT8, 0) + "%");
                }
                intent.putExtra(EXTRA_SPEED_DATA, characteristic.getIntValue(FORMAT_UINT8, 0) + "%");
                break;
            case MODEL_NUMBER:
                final byte[] model = characteristic.getValue();
                String modelNum = BLEUtilities.getStringFromByte(model);
                if (debugBLEData) {
                    Log.i(LOGTAG, "Speed Model #: " + modelNum);
                }
                intent.putExtra(EXTRA_SPEED_DATA, modelNum);
                break;
            case SERIAL_NUMBER:
                final byte[] sn = characteristic.getValue();
                String serial = BLEUtilities.getStringFromByte(sn);
                if (debugBLEData) {
                    Log.i(LOGTAG, "Speed Serial #: " + serial);
                }
                intent.putExtra(EXTRA_SPEED_DATA, serial);
                break;
            case FIRMWARE_REV:
                final byte[] fwRev = characteristic.getValue();
                String fwRevStr = BLEUtilities.getStringFromByte(fwRev);
                if (debugBLEData) {
                    Log.i(LOGTAG, "Speed Firmware rev: " + fwRevStr);
                }
                intent.putExtra(EXTRA_SPEED_DATA, fwRevStr);
                break;
            case HARDWARE_REV:
                final byte[] hwRev = characteristic.getValue();
                String hwRevStr = BLEUtilities.getStringFromByte(hwRev);
                if (debugBLEData) {
                    Log.i(LOGTAG, "Speed Hardware rev: " + hwRevStr);
                }
                intent.putExtra(EXTRA_SPEED_DATA, hwRevStr);
                break;
            case SOFTWARE_REV:
                final byte[] swRev = characteristic.getValue();
                String swRevStr = BLEUtilities.getStringFromByte(swRev);
                if (debugBLEData) {
                    Log.i(LOGTAG, "Speed Software rev: " + swRevStr);
                }
                intent.putExtra(EXTRA_SPEED_DATA, swRevStr);
                break;
            case MANUFACTURER_NAME:
                final byte[] manName = characteristic.getValue();
                String manNameStr = BLEUtilities.getStringFromByte(manName);
                if (debugBLEData) {
                    Log.i(LOGTAG, "Speed Manufacturer: " + manNameStr);
                }
                intent.putExtra(EXTRA_SPEED_DATA, manNameStr);
                break;
            case BIKE_SPD_CAD_FEATURE:
                //read bits to discover if has wheel revs and/or crank revs
                // if has wheel, ! has cad, set DeviceType Speed.intValue
                // if has wheel, has cad set DeviceType SpdCad.intValue
                // has cad, !has wheel set DeviceType Cad.intValue
                // intentputExtra()
                break;
            default:
                // For all other profiles, writes the data formatted in HEX.
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));
                    intent.putExtra(EXTRA_SPEED_DATA, new String(data) + "\n" + stringBuilder.toString());
                }
        }
        // code all data including device MAC address
        intent.putExtra(EXTRAS_DEVICE_SPEED_ADDRESS, mBluetoothDeviceAddress);
        sendBroadcast(intent);
    }

    public boolean isServiceConnected() {
        return mConnectionState == STATE_CONNECTED;
    }

    class LocalBinder extends Binder {
        BLESpeedService getService() {
            return BLESpeedService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        if (MainActivity.debugAppState)Log.i(TAG, "in onBind()");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        if (MainActivity.debugAppState)Log.i(TAG, "in onRebind()");
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (MainActivity.debugAppState)Log.i(TAG, "Last client unbound from service");
        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        close();
        if (!mChangingConfiguration && Utilities.requestingLocationUpdates(this)) {
            if (MainActivity.debugAppState)Log.i(TAG, "Starting foreground service");
            startForeground(NOTIFICATION_ID, getNotification());
        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification() {
        Intent intent = new Intent(this, BLESpeedService.class);
        CharSequence text = "";
        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);
        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        // The PendingIntent to launch activity.
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentText(text)
                .addAction(R.drawable.ic_launch, getString(R.string.launch_activity),
                        activityPendingIntent)
                .addAction(R.drawable.ic_cancel, getString(R.string.remove_location_updates),
                        servicePendingIntent)
                .setContentTitle(Utilities.getLocationTitle(this))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)
                .setWhen(System.currentTimeMillis());
        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }
        return builder.build();
    }
    private Handler mServiceHandler;
    private static final String TAG = BLESpeedService.class.getSimpleName();
    /**
     * The name of the channel for notifications.
     */
    private static final String CHANNEL_ID = "BLESpeedService";
    private static final int NOTIFICATION_ID = 42726655;
    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */
    private boolean mChangingConfiguration = false;
    @Override
    public void onCreate() {

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            // Set the Notification Channel for the Notification Manager.
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(mChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(LOGTAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(LOGTAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(LOGTAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null
                && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            if (debugBLEService) {Log.d(LOGTAG, "Trying to use an existing mBluetoothGatt for connection.");}
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                Log.w(LOGTAG, "BluetoothGatt failed to connect");
                disconnect();
                return false;
            }
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(LOGTAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        if (debugBLEService) {Log.d(LOGTAG, "Trying to create a new connection.");}
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(LOGTAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                close();
            }
        }, 100);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    private void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(LOGTAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(LOGTAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (debugBLEService) {Log.v(LOGTAG, "setCharacteristicNotification: " + characteristic.getUuid().toString());}
        // This is specific to CSC Measurement.
        if (UUID_CSC_MEASUREMENT.equals(characteristic.getUuid())) {

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(BLEConstants.CLIENT_CHARACTERISTIC_CONFIG));
            if (debugBLEService) {Log.v(LOGTAG, "writing UUID_CSC_MEASUREMENT descriptor: " + descriptor.getUuid().toString());}
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) {
            return null;
        }
        return mBluetoothGatt.getServices();
    }
}
