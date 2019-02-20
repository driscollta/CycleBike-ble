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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_DATA_AVAILABLE;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_GATT_CONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_GATT_DISCONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_GATT_SERVICES_DISCOVERED;
import static com.cyclebikeapp.ble.BLEConstants.EXTRAS_DEVICE_ADDRESS;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_DATA;
import static com.cyclebikeapp.ble.MainActivity.debugBLEService;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BLEDiscoveryService extends Service {
    private final static String LOGTAG = BLEDiscoveryService.class.getSimpleName();

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
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                if (debugBLEService) Log.i(LOGTAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                boolean discoveredServices = mBluetoothGatt.discoverServices();
                if (debugBLEService) Log.i(LOGTAG, "Attempting to start service discovery:"  + (discoveredServices ? "yes" : "no"));
            } else if (newState == STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                if (debugBLEService) Log.i(LOGTAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (debugBLEService) Log.w(LOGTAG, "onServicesDiscovered() " + status);
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                // now we can examine Services and Characteristics of the GATT device via mBluetoothGatt.getServices()
            } else {
                if (debugBLEService) Log.w(LOGTAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRAS_DEVICE_ADDRESS, mBluetoothDeviceAddress);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (MainActivity.debugBLEService){
            Log.i(LOGTAG, "DataType uuid: " + characteristic.getUuid().toString().substring(4, 8));
        }
        BLEDataType type = BLEDataType.valueOf(Integer.parseInt(characteristic.getUuid().toString().substring(4, 8), 16));
        switch (type) {
            case APPEARANCE:
                final byte[] mData = characteristic.getValue();
                if (mData != null && mData.length > 0) {
                    final StringBuilder mStringBuilder = new StringBuilder(mData.length);
                    for (byte byteChar : mData) {
                        mStringBuilder.append(String.format("%02X ", byteChar));
                    }
                    if (MainActivity.debugBLEService) {
                        Log.i(LOGTAG, "APPEARANCE data: " + mStringBuilder.toString());
                    }
                }
                int offset = 0;
                BLEDeviceType appearance = BLEDeviceType.valueOf(characteristic.getIntValue(FORMAT_UINT16, offset));
                if (MainActivity.debugBLEService) {
                    Log.i(LOGTAG, "APPEARANCE Device type: " + appearance.toString());
                }
                intent.putExtra(EXTRA_DATA, String.valueOf(appearance.intValue()));
                break;
            case DEVICE_NAME:
                if (MainActivity.debugBLEService) {Log.i(LOGTAG, "Device Name: " + characteristic.getStringValue(0));}
                intent.putExtra(EXTRA_DATA, characteristic.getStringValue(0));
                break;
            case BIKE_SPD_CAD_FEATURE:
                //read bits to discover if has wheel revs and/or crank revs
                // if has wheel, !has cad, set DeviceType Speed.intValue
                // if has wheel, has cad set DeviceType SpdCad.intValue
                // has cad, !has wheel set DeviceType Cad.intValue
                // intentputExtra()
/*
                Bit Field Bit Size 	    Name Definition                 Key 	Value
                0 	    1 	        Wheel Revolution Data Supported     0 	    False
                                                                        1 	    True
                1 	    1 	        Crank Revolution Data Supported     0 	    False
                                                                        1 	    True
                2 	    1 	        Multiple Sensor Locations Supported 0 	    False
                */
                // & 0x01 retrieves bit 0, which is the speed feature, & 0x02 gets bit 2, etc
                int flag = characteristic.getIntValue(FORMAT_UINT16, 0);
                final byte[] mByteData = characteristic.getValue();
                if (mByteData != null && mByteData.length > 0) {
                    final StringBuilder mStringBuilder = new StringBuilder(mByteData.length);
                    for (byte byteChar : mByteData) {
                        mStringBuilder.append(String.format("%02X ", byteChar));
                    }
                    if (MainActivity.debugBLEService) {
                        Log.i(LOGTAG, "BIKE_SPD_CAD_FEATURE data: " + mStringBuilder.toString());
                    }
                }
                if (MainActivity.debugBLEService) {
                    Log.i(LOGTAG, "reading BIKE_SPD_CAD_FEATURE props - flag: " + flag);
                }
                boolean wheelRevsSupported = (flag & 0x01) != 0;
                boolean crankRevsSupported = (flag & 0x02) != 0;
                if (MainActivity.debugBLEService) {
                    Log.i(LOGTAG, "reading BIKE_SPD_CAD_FEATURE props - wheelRevs: "
                            + (wheelRevsSupported ? "true" : "false")
                            + " crank revs: " + (crankRevsSupported ? "true" : "false"));
                }
                int devTypeInt;
                if (wheelRevsSupported && !crankRevsSupported) {
                    devTypeInt = BLEDeviceType.BIKE_SPD_DEVICE.intValue();
                    //Log.i(LOGTAG, "Device is BIKE_SPD_DEVICE " + devTypeInt);
                } else if (!wheelRevsSupported && crankRevsSupported) {
                    devTypeInt = BLEDeviceType.BIKE_CADENCE_DEVICE.intValue();
                    //Log.i(LOGTAG, "Device is BIKE_CADENCE_DEVICE " + devTypeInt);
                } else if (wheelRevsSupported && crankRevsSupported) {
                    devTypeInt = BLEDeviceType.BIKE_SPDCAD_DEVICE.intValue();
                    //Log.i(LOGTAG, "Device is BIKE_SPDCAD_DEVICE " + devTypeInt);
                } else {
                    devTypeInt = BLEDeviceType.UNKNOWN_DEVICE.intValue();
                }
                intent.putExtra(EXTRA_DATA, String.valueOf(devTypeInt));
                break;
            default:
                // For all other profiles, writes the data formatted in HEX.
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));
                    intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
                }
        }
        // code all data including device MAC address
        intent.putExtra(EXTRAS_DEVICE_ADDRESS, mBluetoothDeviceAddress);
        sendBroadcast(intent);
    }

    class LocalBinder extends Binder {
        BLEDiscoveryService getService() {
            return BLEDiscoveryService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
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

        // Previously connected device. Try to reconnect. This will not be used since we .close() after .disconnect()
        if (mBluetoothDeviceAddress != null
                && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.i(LOGTAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(LOGTAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.wtf(LOGTAG, "Trying to create a new connection.");
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
        mHandler.postDelayed(this::close, 20);
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

    public boolean isServiceConnected() {
        return mConnectionState == STATE_CONNECTED;
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
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
            boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(LOGTAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) {
            Log.wtf(LOGTAG, "mBluetoothGatt is null");
            return null;
        }
        return mBluetoothGatt.getServices();
    }
}
