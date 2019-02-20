package com.cyclebikeapp.ble;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import static com.cyclebikeapp.ble.Constants.JAN_1_2000;
import static com.cyclebikeapp.ble.Constants.MAX_SPEED;
import static com.cyclebikeapp.ble.Constants.PREFS_NAME;
import static com.cyclebikeapp.ble.Constants.TIME_STR_FORMAT;
import static com.cyclebikeapp.ble.Constants.TRIP_DISTANCE;
import static com.cyclebikeapp.ble.Constants.TRIP_TIME;
import static com.cyclebikeapp.ble.Constants.msecPerSec;
/*
 * Copyright  2013 cyclebikeapp. All Rights Reserved.
*/


class BikeStat {
    // use System time to indicate loss of GPS after 3 seconds
    long newFusedLocSysTimeStamp;
    long newGPSLocSysTimeStamp;
    private long firstLocSysTimeStamp;
    private final String tripTimeString;
    /** in m */
	private double gpsTripDistance;
	/** in meters, distance using calibrated speed sensor*/
	private double wheelTripDistance;
	private double prevWheelTripDistance;
	/** in meters, distance using speed sensor for spoofing locations in trainer mode*/
	private double spoofWheelTripDistance;
	/** in meters, distance using speed sensor for spoofing locations in trainer mode*/
	private double prevSpoofWheelTripDistance;
	/** in meters, distance using calibrated speed sensor*/
	private double powerWheelTripDistance;
	/** speed value from wheel sensor */
	private double sensorSpeed;
    /** is sensorSpeed current? This means we have data from the sensor, but the data may be the same value on repeated mesages*/
	private boolean sensorSpeedCurrent;
	/** speed value from PowerTap sensor */
	private double powerSpeed;
    /** is powerSpeed current? This means we have data from the sensor, but the data may be the same value on repeated mesages */
	private boolean powerSpeedCurrent;
    /** speed from gps receiver */
	private double gpsSpeed;
    /** is gps Speed current */
    boolean gpsSpeedCurrent;
	/** current bike Location (Latitude, Longitude, Altitude, time) */
	private Location lastGoodWP = new Location(LocationManager.GPS_PROVIDER);
	/** previous bike Location */
	private Location prevGoodWP = new Location(LocationManager.GPS_PROVIDER);
	private double wheelRideTime;
	private double powerWheelRideTime;
	/** Time in seconds since the current trip started */
	private double gpsRideTime;
	/** if we're paused, allow screen to dim, write app message
	calculate DOT using magnetic sensor and don't increment the ride time clock */
	private boolean paused = true;
    /** speed value to display and write to track record; combined in mergeSpeedSensors() */
    private double speed = 0.;
    /** in meters per sec by dividing tripDistance by tripTime */
    private double avgSpeed = 0;
    /** in meters per sec */
    private double maxSpeed = 0;
	/** heart-rate value from BLE heart-rate monitor (BPM) */
	private int heartRate = 0;
	/** average heart rate (BPM) */
	private int avgHeartRate = 0;
	/** maximum heart rate (BPM) */
	private int maxHeartRate = 0;
	/** cadence value from BLE speed-cadence sensor rpm*/
	private int cadence = 0;
	/** average cadence value rpm */
	private int avgCadence = 0;
	/** maximum cadence value rpm */
	private int maxCadence = 0;
	/** prev power from BLE power meter (Watts) */
	private int prevPower = 0;
    /** power from right-side power meter (Watts) */
    private int rightPower = 0;
    /** power from left-side power meter (Watts) */
    private int leftPower = 0;
	/** power from power meter (Watts) */
	private int power = 0;
	/** average power from BLE power meter (Watts) */
	private int avgPower = 0;
	/** maximum power from BLE power meter (Watts) */
	private int maxPower = 0;
	/** has ANT heart rate monitor */
	boolean hasHR = false;
	/** has BLE cadence sensor; either stand-alone speed or part of speed-cadence */
	boolean hasCadence = false;
	/** has BLE cadence sensor from crank power meter */
	boolean hasPowerCadence = false;
	/** has BLE left side power monitor as part of a distributed power meter*/
    boolean hasLeftPower = false;
    /** has BLE right side power monitor as part of a distributed power meter */
    boolean hasRightPower = false;
    /** has BLE power monitor */
    boolean hasPower = false;
    /** has power monitor that reports left/right percent power*/
    boolean hasPercentPower = false;
	/** calibrated speed sensor; either stand-alone speed or part of speed-cadence */
	boolean hasCalSpeedSensor = false;
	/** uncalibrated speed sensor; either stand-alone speed or part of speed-cadence. We can use this for trainer mode, or before we have GPS signals */
	boolean hasSpeedSensor = false;
	/** calibrated power meter speed sensor like from PowerTap */
	boolean hasCalPowerSpeedSensor = false;
	/** uncalibrated power meter speed sensor like from PowerTap. We can use this for trainer mode, or before we have GPS signals*/
	boolean hasPowerSpeedSensor = false;
	private int instantaneousCrankCadence;
	private int prevInstantaneousCrankCadence;
	private int instantaneousCrankCadenceEST;
    private final int colorWhite;
    private final int colorHiViz;
    private final int colorCalSpeed;
    private final int colorUncalSpeed;
	//private final Context myContext;
	/** this will be the tcx log file */
    final TCXLogFile tcxLog;
	/** this will be the fit log file */
    final FITLogFile fitLog;
    private int speedColor;
	private int powerCadence;
	private int pedalCadence;
    private final SharedPreferences.Editor editor;
    private final String logtag = this.getClass().getSimpleName();
    // twice the length of the bike crank arm
    private int leftCrankLength = 0;
    private int rightCrankLength = 0;
    private int satellitesInUse = 0;
    private float locationAccuracy;
    private boolean GPSCellNeeded;
    private boolean GPSWiFiNeeded;
    Iterable<GpsSatellite> satellites;
    boolean isNetworkEnabled;
    boolean isGPSEnabled;
    private boolean networkCellNeeded;
    private boolean networkWiFiNeeded;
    GnssStatus gpsSatelliteStatus;

    private Float deltaDOT;
    private long pausedClock;

    /**
	 * BikeStat contains all bike related information: trip distance, time,
	 * speeds, and control access to the log file
	 * @param context is the main activity context
	 */
    @SuppressLint("CommitPrefEdits")
    BikeStat(Context context) {
        gpsSatelliteStatus = null;
        GPSCellNeeded = false;
        GPSWiFiNeeded = false;
        networkCellNeeded = false;
        networkWiFiNeeded = false;
        isGPSEnabled = false;
        isNetworkEnabled = false;
		gpsRideTime = 0.4;
		tcxLog = new TCXLogFile(context);
		fitLog = new FITLogFile(context);
        editor = context.getSharedPreferences(PREFS_NAME, 0).edit();
        colorWhite = ContextCompat.getColor(context, R.color.white);
        colorHiViz = ContextCompat.getColor(context, R.color.texthiviz);
        colorUncalSpeed = ContextCompat.getColor(context, R.color.uncal_speed);
        colorCalSpeed = ContextCompat.getColor(context, R.color.cal_speed);
        tripTimeString = context.getString(R.string.tripTimeStr);
        firstLocSysTimeStamp = Long.MAX_VALUE;
        newGPSLocSysTimeStamp = JAN_1_2000;
        newFusedLocSysTimeStamp = JAN_1_2000;
    }
	
	/** the newLocation Handler should call this routine with the new position data.
	 *  If this is the first location of this Trip, put the new Location in both lastGoodWP & prevGoodWP
	 * @param firstLocation true if this is the first location of the trip
	 * @param myPlace the new Location data
	 * */
	void setLastGoodWP(Location myPlace, boolean firstLocation) {
		if (firstLocation) {
		    firstLocSysTimeStamp = System.currentTimeMillis();
            lastGoodWP = myPlace;
            lastGoodWP.setTime(myPlace.getTime() + 2);
			prevGoodWP = myPlace;
			prevGoodWP.setTime(myPlace.getTime() + 1);
		} else {
			// swap lastGoodWP into prevGoodWP and copy newLocation to lastGoodWP
			prevGoodWP = lastGoodWP;
			lastGoodWP = myPlace;
			calcTripDistSpeed();
		}
		locationAccuracy = myPlace.getAccuracy();
        if (myPlace.hasSpeed()) {
            gpsSpeed = myPlace.getSpeed();
            gpsSpeedCurrent = true;
        } else {
		    gpsSpeed = -1.;
            gpsSpeedCurrent = false;
        }
	}

    /**
     * given a new, valid Location re-calculate all the fields affected by the new location measurement; time is in seconds
     */
    private void calcTripDistSpeed() {
        float[] results = {0};
        //distanceBetween returns in meters
        Location.distanceBetween(lastGoodWP.getLatitude(), lastGoodWP.getLongitude(),
                prevGoodWP.getLatitude(), prevGoodWP.getLongitude(), results);
        double deltaDistance = (double) results[0];

        double deltaTime = Math.abs(lastGoodWP.getTime() - prevGoodWP.getTime()) / msecPerSec;
        if (deltaTime < 0.001) {
            deltaTime = .05;
        }
        if (!isPaused()) {
            gpsTripDistance += deltaDistance;
            wheelTripDistance += deltaDistance;
            gpsRideTime += deltaTime;
            //save in sharedPrefs in case app crashes, we'll have the trip time and distance to restore
            editor.putString(TRIP_TIME, Double.toString(gpsRideTime));
            editor.putString(TRIP_DISTANCE, Double.toString(gpsTripDistance));
            editor.putString(MAX_SPEED, Double.toString(getMaxSpeed()));
            editor.apply();
        }
        avgSpeed = gpsTripDistance / gpsRideTime;
        // rideTime was initialized to .4 sec to prevent / zero errors
    }

	public void reset() {
		gpsTripDistance = 0.0;
		gpsRideTime = 0.1;
		maxSpeed = 0.0;
		avgSpeed = 0.0;
		speed = 0;
		maxCadence = 0;
		maxHeartRate  = 0;
		maxPower = 0;
		spoofWheelTripDistance = 0.0;
		prevSpoofWheelTripDistance = 0.0;
		prevWheelTripDistance = 0.0;
		wheelTripDistance = 0.0;
		wheelRideTime = 0.1;
		powerWheelTripDistance = 0.0;
		powerWheelRideTime = 0.1;
	}

    /**
     * Called when a speed value is received from a speed sensor, or GPS.
     * Depending on what sensors are available. Speed is used in display and writing track,
     * Also set the color for the speed display depending on calibration, Settings choice.
     * <p>
     * outside of trainer mode
     * 1) if a calibrated speedSensor available use that
     * 2) if a calibrated powerTap available use that
     * 3) if location not current use either speed sensor, or PowerTap
     * 4) use GPS if location is current
     * 5) set -1 if no speed sensors and no GPS. refreshSpeed wil set display to "XX.X"
     * <p>
     * In trainerMode
     * 1) if speedSensor available, use sensorSpeed
     * 2) if powerSpeedSensor available use powerSpeed
     */
    void mergeSpeedSensors(boolean trainerMode, boolean isHiViz) {

        speedColor = colorWhite;
        if (isHiViz) {
            speedColor = colorHiViz;
        }
        if (trainerMode) {
            if (hasSpeedSensor && sensorSpeedCurrent) {
                // Log.wtf(logtaggetName(), "mergeSpeedSensors TM-  hasSpeedSensor & current");
                speed = sensorSpeed;
            } else if (hasPowerSpeedSensor && powerSpeedCurrent) {
                // Log.wtf(logtaggetName(), "mergeSpeedSensors TM- hasPowerSpeedSensor & current");
                speed = powerSpeed;
            }
        } else {
            if (hasCalSpeedSensor && sensorSpeedCurrent) {
                //Log.wtf(logtaggetName(), "mergeSpeedSensors: hasCalSpeedSensor & current");
                speed = sensorSpeed;
                speedColor = colorCalSpeed;
            } else if (hasCalPowerSpeedSensor && powerSpeedCurrent) {
                //Log.wtf(logtaggetName(), "mergeSpeedSensors: hasCalPowerSpeedSensor & current");
                speed = powerSpeed;
                speedColor = colorCalSpeed;
            } else if (gpsSpeedCurrent) {
                //Log.wtf(logtaggetName(), "mergeSpeedSensors: gpsSpeedCurrent");
                speed = gpsSpeed;
            } else if (hasSpeedSensor && sensorSpeedCurrent) {
                //Log.wtf(logtaggetName(), "mergeSpeedSensors: hasSpeedSensor & current");
                speed = sensorSpeed;
                speedColor = colorUncalSpeed;
            } else if (hasPowerSpeedSensor && powerSpeedCurrent) {
                Log.wtf(logtag, "mergeSpeedSensors: hasPowerSpeedSensor & current");
                speed = powerSpeed;
                speedColor = colorUncalSpeed;
            }
        }
        if (speed > maxSpeed) {
            maxSpeed = speed;
        }
    }

    /**
     * Based on the source for speed, set the speedometer display color
     * called from refreshSpeed()
     * @return color value for speed display
     */
    int getSpeedColor() {
        return speedColor;
    }

	double getGPSTripDistance() {
		return gpsTripDistance;
	}

	double getAvgSpeed() {
		return avgSpeed;
	}

	double getMaxSpeed() {
		return maxSpeed;
	}

	Location getLastGoodWP() {
		return lastGoodWP; // a pointer to this Location
	}

	double getGPSRideTime() {
		return gpsRideTime;
	}

	/** method to display trip time as a string hours:minutes:seconds */
	@SuppressLint("DefaultLocale")
	String getTripTimeStr(double time) {
        long longTime = (long) time;
        int timeDay = (int) TimeUnit.SECONDS.toDays(longTime);
        int timeHours = (int) TimeUnit.SECONDS.toHours(longTime);
        int timeMinutes = (int) TimeUnit.SECONDS.toMinutes(longTime);

		int hours = timeHours - (timeDay * 24);
		int minutes = timeMinutes - (timeHours * 60);
		int seconds = (int) TimeUnit.SECONDS.toSeconds(longTime) - (timeMinutes * 60);
        return String.format(tripTimeString,
                String.format(TIME_STR_FORMAT, hours),
                String.format(TIME_STR_FORMAT, minutes),
                String.format(TIME_STR_FORMAT, seconds));
	}

	void setGPSTripTime(double d) {
		this.gpsRideTime = d;
	}

	void setGPSTripDistance(double d) {
		this.gpsTripDistance = d;
	}

	boolean isPaused() {
		return paused;
	}

	void setPaused(boolean paused) {
		this.paused = paused;
	}

	void setHR(int heartRate) {
		this.heartRate = heartRate;
	}
	int getHR() {
		return heartRate;
	}

	public void setPower(int power) {
		this.power = power;
	}

	public int getPower() {
		return power;
	}

	int getAvgHeartRate() {
		return avgHeartRate;
	}

	void setAvgHeartRate(int avgHeartRate) {
		this.avgHeartRate = avgHeartRate;
	}

	int getAvgPower() {
		return avgPower;
	}

	void setAvgPower(int avgPower) {
		this.avgPower = avgPower;
	}

	int getMaxHeartRate() {
		return maxHeartRate;
	}

	void setMaxHeartRate(int maxHeartRate) {
		this.maxHeartRate = maxHeartRate;
	}

	int getMaxPower() {
		return maxPower;
	}

	void setMaxPower(int maxPower) {
		this.maxPower = maxPower;
	}

	public int getCadence() {
		return cadence;
	}
	public void setCadence(int cadence) {
		this.cadence = cadence;
	}
	void setPedalCadence(int iCadence) {
        if (iCadence < Constants.MAXIMUM_CT_CADENCE) {
            this.pedalCadence = iCadence;
            if (hasCadence) {
                this.cadence = pedalCadence;
            } else {
                this.cadence = powerCadence;
            }
        }
	}
	void setPowerCadence(int iCadence) {
        if (iCadence < Constants.MAXIMUM_CT_CADENCE) {
            this.powerCadence = iCadence;
            if (hasCadence) {
                this.cadence = pedalCadence;
            } else {
                this.cadence = powerCadence;
            }
        }
	}
	int getPowerCadence(){return powerCadence;}
	int getMaxCadence() {
		return maxCadence;
	}

	void setMaxCadence(int maxCadence) {
		this.maxCadence = maxCadence;
	}

	int getAvgCadence() {
		return avgCadence;
	}

	void setAvgCadence(int avgCadence) {
		this.avgCadence = avgCadence;
	}

	double getWheelTripDistance() {
		return wheelTripDistance;
	}

	void setWheelTripDistance(double wheelTripDistance) {
		this.wheelTripDistance = wheelTripDistance;
	}

	double getPowerWheelTripDistance() {
		return powerWheelTripDistance;
	}

	void setPowerWheelTripDistance(double powerWheelTripDistance) {
		this.powerWheelTripDistance = powerWheelTripDistance;
	}

	int getPrevPower() {
		return prevPower;
	}

	void setPrevPower(int prevPower) {
		this.prevPower = prevPower;
	}

	double getPrevWheelTripDistance() {
		return prevWheelTripDistance;
	}

	void setPrevWheelTripDistance(double prevWheelTripDistance) {
		this.prevWheelTripDistance = prevWheelTripDistance;
	}

	double getSpoofWheelTripDistance() {
		return spoofWheelTripDistance;
	}

	void setSpoofWheelTripDistance(double spoofWheelTripDistance) {
		this.spoofWheelTripDistance = spoofWheelTripDistance;
	}

	double getPrevSpoofWheelTripDistance() {
		return prevSpoofWheelTripDistance;
	}

	void setPrevSpoofWheelTripDistance(double prevSpoofWheelTripDistance) {
		this.prevSpoofWheelTripDistance = prevSpoofWheelTripDistance;
	}
	Location getLastLocation() {
		return lastGoodWP;
	}

	int getInstantaneousCrankCadence() {
		return instantaneousCrankCadence;
	}

	void setInstantaneousCrankCadence(int instantaneousCrankCadence) {
		this.instantaneousCrankCadence = instantaneousCrankCadence;
	}

	int getPrevInstantaneousCrankCadence() {
		return prevInstantaneousCrankCadence;
	}

	void setPrevInstantaneousCrankCadence(
			int prevInstantaneousCrankCadence) {
		this.prevInstantaneousCrankCadence = prevInstantaneousCrankCadence;
	}

	int getInstantaneousCrankCadenceEST() {
		return instantaneousCrankCadenceEST;
	}

	void setInstantaneousCrankCadenceEST(int instantaneousCrankCadenceEST) {
		this.instantaneousCrankCadenceEST = instantaneousCrankCadenceEST;
	}

	double getPowerWheelRideTime() {
		return powerWheelRideTime;
	}

	void setPowerWheelRideTime(double powerWheelRideTime) {
		this.powerWheelRideTime = powerWheelRideTime;
	}

	double getWheelRideTime() {
		return wheelRideTime;
	}

	void setWheelRideTime(double wheelRideTime) {
		this.wheelRideTime = wheelRideTime;
	}

	public double getSpeed() {
		return speed;
	}

	void setSensorSpeed(double sensorSpeed) {
		this.sensorSpeed = sensorSpeed;
	}

	void setPowerSpeed(double powerSpeed) {
		this.powerSpeed = powerSpeed;
	}

	void setSensorSpeedCurrent(boolean sensorSpeedCurrent) {
		this.sensorSpeedCurrent = sensorSpeedCurrent;
	}
    boolean getSensorSpeedCurrent() {
        return sensorSpeedCurrent;
    }
	void setPowerSpeedCurrent(boolean powerSpeedCurrent) {
		this.powerSpeedCurrent = powerSpeedCurrent;
	}
    boolean getPowerSensorSpeedCurrent() {
        return powerSpeedCurrent;
    }
    void setMaxSpeed(Double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    double getSensorSpeed() {
        return sensorSpeed;
    }

    double getGpsSpeed() {
        return gpsSpeed;
    }

    double getPowerSpeed() {
        return powerSpeed;
    }

    int getPedalCadence() {
        return pedalCadence;
    }


    void setRightPower(int rightPower) {
        hasRightPower = true;
        this.rightPower = rightPower;
        if (hasLeftPower) {
            power = leftPower + rightPower;
        } else {
            power = 2 * rightPower;
        }
    }

    void setLeftPower(int leftPower) {
        hasLeftPower = true;
        this.leftPower = leftPower;
        if (hasRightPower) {
            power = leftPower + rightPower;
        } else {
            power = 2 * leftPower;
        }
    }

    void setLeftCrankLength(int leftCrankLength) {
        this.leftCrankLength = leftCrankLength;
    }

    void setRightCrankLength(int rightCrankLength) {
        this.rightCrankLength = rightCrankLength;
    }

    int getLeftCrankLength() {
        return leftCrankLength;
    }

    int getRightCrankLength() {
        return rightCrankLength;
    }

    void setSatellitesInUse(int satellitesInUse) {
        this.satellitesInUse = satellitesInUse;
    }

    float getLocationAccuracy() {
        return locationAccuracy;
    }

    int getSatellitesInUse() {
        return satellitesInUse;
    }

    boolean getGPSCellNeeded() {
        return GPSCellNeeded;
    }

    boolean getGPSWiFiNeeded() {
        return GPSWiFiNeeded;
    }

    public void setGPSCellNeeded(boolean GPSCellNeeded) {
        this.GPSCellNeeded = GPSCellNeeded;
    }

    public void setGPSWiFiNeeded(boolean GPSWiFiNeeded) {
        this.GPSWiFiNeeded = GPSWiFiNeeded;
    }

    public void setNetworkCellNeeded(boolean networkCellNeeded) {
        this.networkCellNeeded = networkCellNeeded;
    }

    void setNetworkWiFiNeeded(boolean networkWiFiNeeded) {
        this.networkWiFiNeeded = networkWiFiNeeded;
    }

    boolean isNetworkCellNeeded() {
        return networkCellNeeded;
    }

    boolean isNetworkWiFiNeeded() {
        return networkWiFiNeeded;
    }

    long getFirstLocSysTimeStamp() {
        return firstLocSysTimeStamp;
    }
    void setFirstLocSysTimeStamp(long firstLocSysTimeStamp) {
        this.firstLocSysTimeStamp = firstLocSysTimeStamp;
    }

    public Float getDeltaDOT() {
        return deltaDOT;
    }

    void setDeltaDOT(Float deltaDOT) {
        this.deltaDOT = deltaDOT;
    }

    void setPausedClock(long pausedClock) {
        this.pausedClock = pausedClock;
    }

    public long getPausedClock() {
        return pausedClock;
    }

}
