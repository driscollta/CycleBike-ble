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
import android.bluetooth.BluetoothProfile;
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

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT16;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT32;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_POWER_DATA_AVAILABLE;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_POWER_GATT_CONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_POWER_GATT_DISCONNECTED;
import static com.cyclebikeapp.ble.BLEConstants.ACTION_POWER_GATT_SERVICES_DISCOVERED;
import static com.cyclebikeapp.ble.BLEConstants.CALIBRATE_POWER_OPCODE;
import static com.cyclebikeapp.ble.BLEConstants.EXTRAS_DEVICE_POWER_ADDRESS;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_CAN_CHANGE_CRANK_LENGTH;
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
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_IS_DISTRIBUTED;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_SENSOR_LOCATION;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_WHEEL_REVS;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_POWER_WHEEL_REV_TIME;
import static com.cyclebikeapp.ble.BLEConstants.EXTRA_RESPONSE_VALUE;
import static com.cyclebikeapp.ble.BLEConstants.REQUEST_CRANK_LENGTH_OPCODE;
import static com.cyclebikeapp.ble.BLEConstants.RESPONSE_VALUE_SUCCESS;
import static com.cyclebikeapp.ble.BLEConstants.UUID_POWER_CONTROL_PT;
import static com.cyclebikeapp.ble.BLEConstants.UUID_POWER_MEASUREMENT;
import static com.cyclebikeapp.ble.BLEUtilities.byteToHexString;
import static com.cyclebikeapp.ble.BLEUtilities.isCharacteristicReadable;
import static com.cyclebikeapp.ble.BLEUtilities.shiftBits;
import static com.cyclebikeapp.ble.LocationUpdatesService.EXTRA_STARTED_FROM_NOTIFICATION;
import static com.cyclebikeapp.ble.MainActivity.debugBLEPowerCal;
import static com.cyclebikeapp.ble.MainActivity.debugBLEPowerCrank;
import static com.cyclebikeapp.ble.MainActivity.debugBLEPowerData;
import static com.cyclebikeapp.ble.MainActivity.debugBLEPowerWheel;
import static com.cyclebikeapp.ble.MainActivity.debugBLEService;

/**
 * Created by TommyD on 12/28/2016.
 *
 */

public class BLEPowerService extends Service {
    private final static String LOGTAG = BLEPowerService.class.getSimpleName();

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
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_POWER_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                if (debugBLEService){Log.wtf(LOGTAG, "Connected to GATT server.");}
                // Attempts to discover services after successful connection.
                if (mBluetoothGatt != null) {
                    boolean discoveredServices = mBluetoothGatt.discoverServices();
                    if (debugBLEService) {
                        Log.wtf(LOGTAG, "Attempting to start measurement service discovery: "
                                + (discoveredServices ? "yes" : "no"));
                    }
                } else{
                    mConnectionState = STATE_DISCONNECTED;
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_POWER_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                if (debugBLEService){Log.w(LOGTAG, "Disconnected from GATT server.");}
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_POWER_GATT_SERVICES_DISCOVERED);
            } else {
                if (debugBLEService){Log.w(LOGTAG, "onServicesDiscovered() failure status: " + status);}
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            final byte[] powerData = characteristic.getValue();
            if (powerData != null && powerData.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(powerData.length);
                for (byte byteChar : powerData) {
                    stringBuilder.append(String.format("%02X ", byteChar));
                }
                if (debugBLEPowerData) {
                    Log.i(LOGTAG, "onCharacteristicRead - received byte hex data: " + stringBuilder.toString());
                }
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_POWER_DATA_AVAILABLE, characteristic);
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status){
            Log.w(LOGTAG, "onCharacteristicWrite: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                boolean success = gatt.readCharacteristic(characteristic);
                if (success) {
                    Log.w(LOGTAG, "onCharacteristicWrite: " + byteToHexString(characteristic.getValue()));
                }
            }
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.w(LOGTAG, "onReliableWriteCompleted: " + status);
        }

        /**
         * When setting-up, or cancelling a measurement this call-back tells us when the remote device actually
         * got the message. Now we know it's safe to write another descriptor. We'll send a broadcast to the GATT receiver
         * to execute the next measurement.
         *
         * @param gatt GATT client invoked
         * @param descriptor the descriptor written to the device
         * @param status The result of the write operation
         *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                BluetoothGattDescriptor descriptor,
                int status) {
            if (debugBLEService){
                Log.w(LOGTAG, "onDescriptorWrite: " + status);
            }
            writeCharacteristic(descriptor.getCharacteristic());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            if (debugBLEPowerData) {
                //Log.w(LOGTAG, "onCharacteristicChanged() " + characteristic.getUuid().toString());
                //Log.i(LOGTAG, "received byte hex data: " + byteToHexString(characteristic.getValue()));
            }
            broadcastUpdate(ACTION_POWER_DATA_AVAILABLE, characteristic);
        }
    };



    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRAS_DEVICE_POWER_ADDRESS, mBluetoothDeviceAddress);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        int shortUUID = BLEUtilities.getShortUUID(characteristic);
        BLEDataType type = BLEDataType.valueOf(shortUUID);
        intent.putExtra(EXTRA_POWER_DATA_TYPE, shortUUID);
        intent.putExtra(EXTRAS_DEVICE_POWER_ADDRESS, mBluetoothDeviceAddress);
        switch (type) {
            case BIKE_POWER_MEAS:
    /* 16-bit: bit 0 = 1, Pedal Power Balance Present; bit 1 = 1, Pedal Power Balance Reference; bit 2 = 1, Accumulated Torque Present,
    bit 3 = 1, Accumulated Torque Source; bit 4 = 1, Wheel Revolution Data Present; bit 5 = 1, Crank Revolution Data Present, etc
     offset 1: sint16:  Instantaneous Power (W);
     offset 2?: uint8: Pedal Power Balance
     offset 3?: uint16:  Accumulated Torque;
     offset 4?: uint32 (Wheel revs) Cumulative Wheel Revolutions, or uint16 (Cumulative Crank Revolutions)
     offset 5: uint16 Last Wheel Event Time in seconds with a resolution of 1/1024
     */
                if (debugBLEPowerData || debugBLEPowerCrank || debugBLEPowerCal) {Log.i(LOGTAG, "Device_power_address: " + mBluetoothDeviceAddress);}
                int flag = characteristic.getIntValue(FORMAT_UINT16, 0);
                final int HAS_PPBALANCE_MASK = 0x01;
                final int HAS_ACCUMTORQUE_MASK = 0x04;
                final int HAS_WHEEL_REV_MASK = 0x10;
                final int HAS_CRANK_REV_MASK = 0x20;
                final int IS_OFFSET_COMPENSATION_MASK = 0x1000;

                boolean hasPPBalance = (flag & HAS_PPBALANCE_MASK) == HAS_PPBALANCE_MASK;
                boolean hasAccumTorque = (flag & HAS_ACCUMTORQUE_MASK) == HAS_ACCUMTORQUE_MASK;
                boolean hasWheelRev = (flag & HAS_WHEEL_REV_MASK) == HAS_WHEEL_REV_MASK;
                boolean hasCrankRev = (flag & HAS_CRANK_REV_MASK) == HAS_CRANK_REV_MASK;
                boolean isOffsetCompensationIndicator = (flag & IS_OFFSET_COMPENSATION_MASK) == IS_OFFSET_COMPENSATION_MASK;
                // the flag takes up the first two bytes
                int iPowerOffset = 2;
                // ppBalance format is 1 byte, if it's present
                int accumTorqueOffset = iPowerOffset + 2 + (hasPPBalance ? 1 : 0);
                //accumTorque format is 2 bytes, if it's present
                int wheelRevOffset = accumTorqueOffset + (hasAccumTorque ? 2 : 0);
                // wheelRev format is 4 bytes
                int wheelRevTimeOffset = wheelRevOffset + 4;
                // wheelRev format is 4 bytes plus wheelRevTime format is another 2 bytes, if it's present
                int crankRevOffset = wheelRevOffset + (hasWheelRev ? 6 : 0);
                //crankRev format is 2 bytes
                int crankRevTimeOffset = crankRevOffset + 2;
                int iPowerFormat = FORMAT_SINT16;
                int ppBalanceFormat = FORMAT_UINT8;
                int accumTorqueFormat = FORMAT_UINT16;
                int wheelRevFormat = FORMAT_UINT32;
                int crankRevFormat = FORMAT_UINT16;
                int wheelRevTimeFormat = FORMAT_UINT16;
                int crankRevTimeFormat = FORMAT_UINT16;
   /*                 if (debugBLEPowerData) {
                    Log.i(LOGTAG, "flag: " + flag);
                    Log.i(LOGTAG, "offset Compensation Indicator = bit 12");
                    Log.i(LOGTAG, "isOffsetCompensationIndicator: " + (isOffsetCompensationIndicator ? "true" : "false"));
              Log.d(LOGTAG, "Accumulated Torque Present = bit 3");
                    Log.d(LOGTAG, "hasAccumTorque: " + (hasAccumTorque?"true":"false"));
                    Log.d(LOGTAG, "Wheel Rev present = bit 4");
                    Log.d(LOGTAG, "hasWheelRev: " + (hasWheelRev ? "true" : "false"));
                    Log.i(LOGTAG, "Crank Revolution Data Present = bit 5");
                    Log.i(LOGTAG, "hasCrankRev: " + (hasCrankRev ? "true" : "false"));
                    Log.i(LOGTAG, "hasPPBalance: " + (hasPPBalance ? "true" : "false"));
                }
                if (isOffsetCompensationIndicator) {
                    String hexString = byteToHexString(characteristic.getValue());
                    intent.putExtra(EXTRA_POWER_DATA, hexString);
                    if (debugBLEPowerData) {Log.i(LOGTAG, "Calibration Data: " + hexString);}
                }*/
                final int iPower = characteristic.getIntValue(iPowerFormat, iPowerOffset);
                if (debugBLEPowerData) {Log.i(LOGTAG, String.format("Received instantaneousPower: %d", iPower));}
                int wheelRevs = 0;
                int wheelTime = 0;
                int crankRevs = 0;
                int crankTime = 0;
                if (hasWheelRev) {
                    wheelRevs = characteristic.getIntValue(wheelRevFormat, wheelRevOffset);
                    if (debugBLEPowerWheel) {Log.i(LOGTAG, String.format("Received wheelRevs: %d", wheelRevs));}
                    wheelTime = characteristic.getIntValue(wheelRevTimeFormat, wheelRevTimeOffset);
                    if (debugBLEPowerWheel) {Log.i(LOGTAG, String.format("Received wheelTime: %d", wheelTime));}
                }
                if (hasCrankRev) {
                    crankRevs = characteristic.getIntValue(crankRevFormat, crankRevOffset);
                    if (debugBLEPowerCrank) {Log.i(LOGTAG, String.format("Received crankRevs: %d", crankRevs));}
                    crankTime = characteristic.getIntValue(crankRevTimeFormat, crankRevTimeOffset);
                    if (debugBLEPowerCrank) {Log.i(LOGTAG, String.format("Received crankTime: %d", crankTime));}
                }

                intent.putExtra(EXTRA_POWER_IPOWER, iPower);
                intent.putExtra(EXTRA_POWER_HAS_SPEED, hasWheelRev);
                intent.putExtra(EXTRA_POWER_HAS_CAD, hasCrankRev);
                intent.putExtra(EXTRA_POWER_WHEEL_REVS, wheelRevs);
                intent.putExtra(EXTRA_POWER_WHEEL_REV_TIME, wheelTime);
                intent.putExtra(EXTRA_POWER_CRANK_REVS, crankRevs);
                intent.putExtra(EXTRA_POWER_CRANK_REV_TIME, crankTime);
                break;
            case DEVICE_NAME:
                if (debugBLEPowerData) {Log.i(LOGTAG, "Device Name: " + characteristic.getStringValue(0));}
                intent.putExtra(EXTRA_POWER_DATA, characteristic.getStringValue(0));
                break;
            case APPEARANCE:
            case SERVICE_CHANGED:
                break;
            case BATTERY_LEVEL:
                if (debugBLEPowerData) {Log.i(LOGTAG, "Battery: " + characteristic.getIntValue(FORMAT_UINT8, 0) + "%");}
                intent.putExtra(EXTRA_POWER_DATA, characteristic.getIntValue(FORMAT_UINT8, 0) + "%");
                break;
            case MODEL_NUMBER:
                final byte[] model = characteristic.getValue();
                String modelNum = BLEUtilities.getStringFromByte(model);
                if (debugBLEPowerData) {Log.i(LOGTAG, "Power Model #: " + modelNum);}
                intent.putExtra(EXTRA_POWER_DATA, modelNum);
                break;
            case SERIAL_NUMBER:
                final byte[] sn = characteristic.getValue();
                String serial = BLEUtilities.getStringFromByte(sn);
                if (debugBLEPowerData) {Log.i(LOGTAG, "Power Serial #: " + serial);}
                intent.putExtra(EXTRA_POWER_DATA, serial);
                break;
            case FIRMWARE_REV:
                final byte[] fwRev = characteristic.getValue();
                String fwRevStr = BLEUtilities.getStringFromByte(fwRev);
                if (debugBLEPowerData) {Log.i(LOGTAG, "Power Firmware rev: " + fwRevStr);}
                intent.putExtra(EXTRA_POWER_DATA, fwRevStr);
                break;
            case HARDWARE_REV:
                final byte[] hwRev = characteristic.getValue();
                String hwRevStr = BLEUtilities.getStringFromByte(hwRev);
                if (debugBLEPowerData) {Log.i(LOGTAG, "Power Hardware rev: " + hwRevStr);}
                intent.putExtra(EXTRA_POWER_DATA, hwRevStr);
                break;
            case SOFTWARE_REV:
                final byte[] swRev = characteristic.getValue();
                String swRevStr = BLEUtilities.getStringFromByte(swRev);
                if (debugBLEPowerData) {Log.i(LOGTAG, "Power Software rev: " + swRevStr);}
                intent.putExtra(EXTRA_POWER_DATA, swRevStr);
                break;
            case MANUFACTURER_NAME:
                final byte[] manName = characteristic.getValue();
                String manNameStr = BLEUtilities.getStringFromByte(manName);
                if (debugBLEPowerData) {Log.i(LOGTAG, "Power Manufacturer: " + manNameStr);}
                intent.putExtra(EXTRA_POWER_DATA, manNameStr);
                break;
            case SENSOR_LOCATION:
                int sensorLocation = characteristic.getIntValue(FORMAT_UINT8, 0);
                if (debugBLEPowerData) {Log.i(LOGTAG, "Power sensorLocation: " + BLESensorLocation.valueOf(sensorLocation));}
                intent.putExtra(EXTRA_POWER_SENSOR_LOCATION, sensorLocation);
                break;
            case BIKE_POWER_VECTOR:
                break;
            case BIKE_POWER_FEATURE:
                //read bits to discover if has wheel revs and/or crank revs
                // if has wheel, ! has cad, set DeviceType Speed.intValue
                // if has wheel, has cad set DeviceType SpdCad.intValue
                // has cad, !has wheel set DeviceType Cad.intValue
                // intentputExtra()
                int bikeFeatureFlag = characteristic.getIntValue(FORMAT_UINT32, 0);
                boolean canChangeCrankLength = (bikeFeatureFlag & 0x1000) == 0x1000;
                boolean hasPowerCadence = (bikeFeatureFlag & 0x08) == 0x08;
                boolean hasPowerSpeed = (bikeFeatureFlag & 0x04) == 0x04;
                boolean hasOffsetCompIndication = (bikeFeatureFlag & 0x100) == 0x100;
                boolean hasOffsetCompensation = (bikeFeatureFlag & 0x200) == 0x200;
                // see if this is a distributed system
                boolean isDistributedSystem = (shiftBits(bikeFeatureFlag, 20) & 0x02) == 0x02;
                if (debugBLEPowerData) {Log.i(LOGTAG, "canChangeCrankLength: " + (canChangeCrankLength ? "yes" : "no"));}
                if (debugBLEPowerData) {Log.i(LOGTAG, "isDistributedSystem: " + (isDistributedSystem ? "yes" : "no"));}
                if (debugBLEPowerData) {Log.i(LOGTAG, "hasOffsetCompIndication: " + (hasOffsetCompIndication ? "yes" : "no"));}
                if (debugBLEPowerData) {Log.i(LOGTAG, "hasOffsetCompensation: " + (hasOffsetCompensation ? "yes" : "no"));}
                if (debugBLEPowerData) {Log.i(LOGTAG, "hasPowerCadence: " + (hasPowerCadence ? "yes" : "no"));}
                if (debugBLEPowerData) {Log.i(LOGTAG, "hasPowerSpeed: " + (hasPowerSpeed ? "yes" : "no"));}
                intent.putExtra(EXTRA_CAN_CHANGE_CRANK_LENGTH, canChangeCrankLength);
                intent.putExtra(EXTRA_POWER_IS_DISTRIBUTED, isDistributedSystem);
                intent.putExtra(EXTRA_POWER_DATA, byteToHexString(characteristic.getValue()));
                break;
            case BIKE_POWER_CONTROL_PT:
                final byte[] powerControlPtData = characteristic.getValue();
                // was calibration successful
                int responseValue = characteristic.getIntValue(FORMAT_UINT8, 2);
                intent.putExtra(EXTRA_RESPONSE_VALUE, responseValue);
                int opCode = characteristic.getIntValue(FORMAT_UINT8, 1);
                if (debugBLEPowerCrank || debugBLEPowerCal) {Log.i(LOGTAG, "Power Control Point opcode: " + opCode);}
                intent.putExtra(EXTRA_PCP_OPCODE, opCode);
                if (opCode == CALIBRATE_POWER_OPCODE && responseValue == RESPONSE_VALUE_SUCCESS) {
                    // is this a calibration
                    int calOffset = powerControlPtData.length - 2;
                    int calValue = characteristic.getIntValue(FORMAT_SINT16, calOffset);
                    if (debugBLEPowerCal) {Log.i(LOGTAG, "Power Control Point calValue: " + calValue);}
                    intent.putExtra(EXTRA_POWER_CAL_VALUE, calValue);
                } else if (opCode == REQUEST_CRANK_LENGTH_OPCODE && responseValue == RESPONSE_VALUE_SUCCESS) {
                    // is this a crank length value
                    int crankLengthOffset = 3;
                    int crankLengthValue = characteristic.getIntValue(FORMAT_UINT16, crankLengthOffset);
                    if (debugBLEPowerCrank) {Log.i(LOGTAG, "Power Control Point crankLengthValue: " + crankLengthValue);}
                    intent.putExtra(EXTRA_POWER_CRANK_LENGTH, crankLengthValue);
                }

                break;
            default:
                // For all other profiles, writes the data formatted in HEX.
                intent.putExtra(EXTRA_POWER_DATA, byteToHexString(characteristic.getValue()));
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BLEPowerService getService() {
            return BLEPowerService.this;
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
        Intent intent = new Intent(this, BLEPowerService.class);
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
    private static final String TAG = BLEPowerService.class.getSimpleName();
    /**
     * The name of the channel for notifications.
     */
    private static final String CHANNEL_ID = "BLEPowerService";
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
    private final IBinder mBinder = new BLEPowerService.LocalBinder();

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
            if (debugBLEService) {Log.i(LOGTAG, "Trying to use an existing mBluetoothGatt for connection.");}
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
        if (debugBLEService) {Log.i(LOGTAG, "Trying to create a new connection.");}
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
        }, 20);
    }

    public boolean isServiceConnected() {
        return mConnectionState == BluetoothProfile.STATE_CONNECTED;
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
     * asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(LOGTAG, "BluetoothAdapter not initialized");
            return;
        }
        if (debugBLEService){
            Log.i(LOGTAG, "Characteristic readable?" + characteristic.getUuid() + (isCharacteristicReadable(characteristic)?" yes":" no"));
        }
        boolean readSuccess = mBluetoothGatt.readCharacteristic(characteristic);
        if (debugBLEService){
            Log.i(LOGTAG, "read Characteristic " + characteristic.getUuid() + (readSuccess?" yes":" no"));
        }

    }

    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}.
     *
     * @param characteristic The characteristic to write to.
     */
    private void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(LOGTAG, "BluetoothAdapter not initialized");
            return;
        }
        if (debugBLEService){
            Log.w(LOGTAG, "writing calibration characteristic");
        }
        mBluetoothGatt.writeCharacteristic(characteristic);
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

        // This is specific to Power Measurement.
        if (UUID_POWER_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(BLEConstants.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Enables or disables indication on a given characteristic.
     *
     * @param characteristic Characteristic to act on.
     */
    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(LOGTAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, true);
        // This is specific to Power Control PT .
        if (UUID_POWER_CONTROL_PT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(BLEConstants.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
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

