package com.cyclebikeapp.ble;

class SpeedCadenceCounts {
	long prevCount = 0;
	long currCount = 0;
    long ertOfLastEvent = 0;
	long prevTime;
	/** time-stamp of last data */
	long currTime;
	/** event-count of this event */
	long eventCount = 0;
	/** estimated time stamp of data */
	long ertTimeStamp;
    /** estimated time stamp of new data that is not the same as the last data value*/
    long ertTimeStampNewData;
    /** time stamp of calibration*/
    private static final long JAN_1_2000 = 975596581L;
    long cal_est = JAN_1_2000;
	/**
	 * total accumulated revolutions used for wheel calibration or distance, or
	 * average cadence
	 */
	long calTotalCount = 0;
	/** total revolutions since sensor calibration was started */
	long cumulativeRevsAtCalStart = 0;
	/** total revolutions since sensor was started */
	long cumulativeRevolutions = 0;
	/** use this to determine zero cadence*/
	public long pausedCounter = 0;
	/** is this the first data from the speed or cadence sensor? */
	boolean initialized = false;
	/** is the wheel calibrated against the GPS sensor? */
	boolean isCalibrated = false;
	/** is the last data current */
	boolean isDataCurrent = false;
    /** is the last data different than the last value */
    boolean isDataNew = false;
	/** circumference of the wheel in meters */
	double wheelCircumference;
	/** initial distance during wheel calibration */
	double calGPSStartDist = 0.;
}
