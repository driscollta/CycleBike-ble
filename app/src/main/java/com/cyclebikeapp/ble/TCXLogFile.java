package com.cyclebikeapp.ble;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Environment;
import android.util.Log;

import com.garmin.fit.DateTime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static com.cyclebikeapp.ble.Constants.FORMAT_3_1F;
import static com.cyclebikeapp.ble.Constants.FORMAT_4_3F;
import static com.cyclebikeapp.ble.Constants.PREFS_NAME;
import static com.cyclebikeapp.ble.Constants.TCX_LOG_FILE_FOOTER_LENGTH;
import static com.cyclebikeapp.ble.Constants.TCX_LOG_FILE_NAME;
import static com.cyclebikeapp.ble.Utilities.isFileSpaceAvailable;

@SuppressWarnings("ResultOfMethodCallIgnored")
class TCXLogFile {
	private static final String FILENAME_SUFFIX = "_CB.tcx";
	private static final String NO_LOGOUT = "no logout";
	private static final String LAP_END = "\">\n";
	private static final String LAP_START_TIME = "\t\t<Lap StartTime= \"";
	private static final String ID_END = "</Id>\n";
	private static final String ID_START = "\t\t<Id>";
	private static final String DATE_TIME_Z_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
	private static final String SMY_RIDETIME_START = "\t\t<TotalTimeSeconds>";
	private static final String SMY_RIDETIME_END = "</TotalTimeSeconds>\n";
	private static final String SMY_DIST_END = "</DistanceMeters>\n";
	private static final String SMY_MAXSPEED_START = "\t\t<MaximumSpeed>";
	private static final String SMY_MAXSPEED_END = "</MaximumSpeed>\n";
	private static final String TAB = "\t";
	private static final String SMY_DIST_START = "\t\t<DistanceMeters>";
	private static final String SMY_EXTENSIONS_START = "\t\t<Extensions>\n\t\t\t" + "<LX>\n";
	//+ "<LX xmlns=\"http://www.garmin.com/xmlschemas/ActivityExtension/v2\">\n";
	private static final String SMY_EXTENSIONS_END = "\t\t\t</LX>\n\t\t</Extensions>\n";
	private static final String SMY_AVGCAD_START = "\t\t<Cadence>";
	private static final String SMY_AVGCAD_END = "</Cadence>\n";
	private static final String SMY_AVGPOW_START = "\t\t<AvgWatts>";
	private static final String SMY_AVGPOW_END = "</AvgWatts>\n";
	private static final String SMY_AVGHR_START = "\t\t<AverageHeartRateBpm>\n\t\t\t<Value>";
//	private static final String SMY_AVGHR_START = "\t\t<AverageHeartRateBpm xsi:type=\"HeartRateInBeatsPerMinute_t\">\n\t\t\t<Value>";
	private static final String SMY_AVGHR_END = "</Value>\n\t\t</AverageHeartRateBpm>\n";
	private static final String SMY_MAXHR_START = "\t\t<MaximumHeartRateBpm>\n\t\t\t<Value>";
//	private static final String SMY_MAXHR_START = "\t\t<MaximumHeartRateBpm xsi:type=\"HeartRateInBeatsPerMinute_t\">\n\t\t\t<Value>";
	private static final String SMY_MAXHR_END = "</Value>\n\t\t</MaximumHeartRateBpm>\n";
	private static final String SMY_AVGSPEED_START = "\t\t\t<AvgSpeed>";
	private static final String SMY_AVGSPEED_END = "</AvgSpeed>\n";
	private static final String SMY_MAXCAD_START = "\t\t\t<MaxBikeCadence>";
	private static final String SMY_MAXCAD_END = "</MaxBikeCadence>\n";
	private static final String TRACK_START_TAG = "\t\t<Track>\n";
	private static final String TRACK_END_TAG = "\t\t</Track>\n";
	private static final String TCX_HEADER1 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n";
	private static final String TCX_HEADER2 = "<TrainingCenterDatabase>\n";
//	private static final String TCX_HEADER2 = "<TrainingCenterDatabase xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/ActivityExtension/v2 http://www.garmin.com/xmlschemas/ActivityExtensionv2.xsd http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2 http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd\">\n";
	private static final String TCX_HEADER3 = "<Activities>\n\t<Activity Sport=\"Biking\">\n";
	private static final String LAP_END_TAG = "\t\t</Lap>\n";
	private static final String TCX_FOOTER1 = LAP_END_TAG + "\t</Activity>\n"
			+ "</Activities>\n";
	private static final String TCX_FOOTER2 = "</TrainingCenterDatabase>";
	private static final String TCX_FOOTER = TCX_FOOTER1 + TCX_FOOTER2;
	private static final String HR_START_TAG = "\t\t\t\t<HeartRateBpm>" + "\n\t\t\t\t\t<Value>";
//	private static final String HR_START_TAG = "\t\t\t\t<HeartRateBpm xsi:type=\"HeartRateInBeatsPerMinute_t\">" + "\n\t\t\t\t\t<Value>";
	private static final String HR_END_TAG = "</Value>\n\t\t\t\t</HeartRateBpm>\n";
	private static final String TIME_START_TAG = "\t\t\t\t<Time>";
	private static final String TIME_END_TAG = "</Time>\n";
	private static final String UF_TIME_START_TAG = "\t\t\t\t<UFTime>";
	private static final String UF_TIME_END_TAG = "</UFTime>\n";
	private static final String POSITION_START_TAG = "\t\t\t\t<Position>\n";
	private static final String POSITION_END_TAG = "\t\t\t\t</Position>\n";
	private static final String LAT_SC_START_TAG = "\t\t\t\t<LatitudeDegreesSC>";
	private static final String LAT_SC_END_TAG = "</LatitudeDegreesSC>\n";
	private static final String LAT_START_TAG = "\t\t\t\t<LatitudeDegrees>";
	private static final String LAT_END_TAG = "</LatitudeDegrees>\n";
	private static final String LON_START_TAG = "\t\t\t\t<LongitudeDegrees>";
	private static final String LON_END_TAG = "</LongitudeDegrees>\n";
	private static final String NUM_SATS_START_TAG = "\t\t\t\t<NumSatellites>";
	private static final String NUM_SATS_END_TAG = "</NumSatellites>\n";
    private static final String LOC_ACCURACY_START_TAG = "\t\t\t\t<LocationAccuracy>";
    private static final String LOC_ACCURACY_END_TAG = "</LocationAccuracy>\n";
    private static final String LON_SC_START_TAG = "\t\t\t\t<LongitudeDegreesSC>";
    private static final String LON_SC_END_TAG = "</LongitudeDegreesSC>\n";
	private static final String ALT_START_TAG = "\t\t\t\t<AltitudeMeters>";
	private static final String ALT_END_TAG = "</AltitudeMeters>\n";
	private static final String DIST_START_TAG = TAB + TAB + SMY_DIST_START;
	private static final String DIST_END_TAG = SMY_DIST_END;
	private static final String EXT_START_TAG = "\t\t\t\t<Extensions>\n\t\t\t\t\t"
			+ "<TPX>\n";
//	+ "<TPX xmlns=\"http://www.garmin.com/xmlschemas/ActivityExtension/v2\">\n";
	private static final String EXT_END_TAG = "\t\t\t\t\t</TPX>\n\t\t\t\t</Extensions>\n";
	private static final String SPEED_START_TAG = "\t\t\t\t\t<Speed>";
	private static final String SPEED_END_TAG = "</Speed>\n";
	private static final String CADENCE_START_TAG = TAB + TAB + SMY_AVGCAD_START;
	private static final String CADENCE_END_TAG = SMY_AVGCAD_END;
	private static final String POWER_START_TAG = "\t\t\t\t\t<Watts>";
	private static final String POWER_END_TAG = "</Watts>\n";
	private static final String TRACK_PT_START_TAG = "\t\t\t<Trackpoint>\n";
	private static final String TRACK_PT_END_TAG = "\t\t\t</Trackpoint>\n";
	private static final String FORMAT_7F = "%.7f";
    // MB of file storage required
    private static final int requiredStorageSpace = 20;
	// semicircle per degrees is 2^31 / 180 degrees
	private static final double semicircle_per_degrees = 11930464.711111111111111111111111;
	private static final String FORMAT_2F = "%.2f";
    // reset-time in milliseconds */
	private static final long oneHour = 60 * 60 * 1000;
	//key to last modified time of write new track
	private static final String LAST_MODIFIED = "last_modified_time";
	private static final String pathName = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ Constants.ACTIVITY_FILE_PATH;
	
	private RandomAccessFile logout = null;
	private String error = "";
	private String sdError = "";
	String outFileName = "";
	Integer outFileFooterLength = 1;
	private final Context context;
    private final SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_Z_FORMAT, Locale.US);

	TCXLogFile(Context context) {
		this.context = context;
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/** Open the Random access file to append more data */
	void reopenTCX(BikeStat bs, NavRoute nr) {
        if (MainActivity.debugOldTCXFile){Log.i(this.getClass().getSimpleName(),"reopenTCX");}
		// presumably the file exists, just re-open it here and position the file pointer
		setError("");
		boolean fileHasPermission = updateExternalStorageState(context);

		if (fileHasPermission && isFileSpaceAvailable(requiredStorageSpace)) {
			File appFiles = new File(pathName);
            if (!appFiles.exists()) {
                appFiles.mkdirs();
            }
			try {
				logout = new RandomAccessFile(new File(appFiles, outFileName), "rw");
			} catch (FileNotFoundException e) {
				setError(context.getString(R.string.error_reopening_raf));
				Log.w(this.getClass().getSimpleName(), context.getString(R.string.error_re_opening_log_file) + e.toString());
			}
			if (logout != null) {
				try {
					// position pointer to write record
					// using the previous footer length (saved in Shared Prefs)
					logout.seek(logout.length() - outFileFooterLength);
					// add new Track when re-opening file
					nr.trackClosed = true;
					saveTCXName_FooterLength();
				} catch (IOException e) {
					setError(context.getString(R.string.error_re_opening_log_file));
					Log.w(this.getClass().getSimpleName(), context.getString(R.string.error_re_opening_log_file) + e.toString());
				}
			}
		} else {
			setError(getSDError());
		}
	}

	/**
	 * We'll write the footer after each record is written so that we can
	 * instantly close the file and have a valid XML document, The footer
	 * consists of a Track closing tag, a summary of data, and 4 other closing
	 * tags as shown below
	 * @param bs is the BikeStat data needed to extract the summary values
	 */
	private String composeFooter(BikeStat bs) {
		// </Lap>
		// </Activity>
		// </Activities>
		// </TrainingCenterDatabase>
		return TRACK_END_TAG + composeSummary(bs) + TCX_FOOTER;
	}

	/**
	 * Here's the data summary included in the footer. Some web-sites use this,
	 * like Garmin connect, but not Strava
	 */
	@SuppressLint("DefaultLocale")
	private String composeSummary(BikeStat bs) {
		// <TotalTimeSeconds>xxx.xxx</TotalTimeSeconds>
		// <DistanceMeters>xxx.xxx</DistanceMeters>
		// <MaximumSpeed>xx.xxx</MaximumSpeed>
		// <AverageHeartRateBpm xsi:type="HeartRateInBeatsPerMinute_t">
		// <Value>xxx</Value>
		// </AverageHeartRateBpm>
		// <MaximumHeartRateBpm xsi:type="HeartRateInBeatsPerMinute_t">
		// <Value>xxx</Value>
		// </MaximumHeartRateBpm>
		// <Cadence>xxx</Cadence>
		// <Extensions>
		// <LX xmlns="http://www.garmin.com/xmlschemas/ActivityExtension/v2">
		
		// <AvgWatts>175</AvgWatts>
		
		// <AvgSpeed>xxx.xxx</AvgSpeed>
		// <MaxBikeCadence>xxx</MaxBikeCadence>
		// </LX>
		// </Extensions>
		String avgCadText = Integer.toString(bs.getAvgCadence());		
		String avgPowText = Integer.toString(bs.getAvgPower());		
		String maxCadText = Integer.toString(bs.getMaxCadence());

       StringBuilder bSummary = new StringBuilder();
        bSummary.append(SMY_RIDETIME_START)
                .append(String.format(FORMAT_2F, bs.getGPSRideTime()))
                .append(SMY_RIDETIME_END)
                .append(SMY_DIST_START)
                .append(String.format(FORMAT_2F, bs.getGPSTripDistance()))
                .append(SMY_DIST_END)
                .append(SMY_MAXSPEED_START)
                .append(String.format(FORMAT_2F, bs.getMaxSpeed()))
                .append(SMY_MAXSPEED_END)

                .append(SMY_AVGHR_START)
                .append(Integer.toString(bs.getAvgHeartRate()))
                .append(SMY_AVGHR_END)

                .append(SMY_MAXHR_START)
                .append(Integer.toString(bs.getMaxHeartRate()))
                .append(SMY_MAXHR_END)

                .append(SMY_AVGCAD_START)
                .append(avgCadText)
                .append(SMY_AVGCAD_END)

                .append(SMY_AVGPOW_START)
                .append(avgPowText)
                .append(SMY_AVGPOW_END)

                .append(SMY_EXTENSIONS_START)
                .append(SMY_AVGSPEED_START)
                .append(String.format(FORMAT_2F, bs.getAvgSpeed()))
                .append(SMY_AVGSPEED_END)

                .append(SMY_MAXCAD_START)
                .append(maxCadText)
                .append(SMY_MAXCAD_END)
                .append(SMY_EXTENSIONS_END);

//		Log.v(this.getClass().getName(), "Summary: " + bSummary);
		return String.valueOf(bSummary);
	}

	void openNewTCX(BikeStat bs, NavRoute nr) {
		//close the previous Log File, if it exists
        if (MainActivity.debugOldTCXFile) Log.i(this.getClass().getSimpleName(), "openNewTCX()");
		closeTCXLogFile();
		// compose ID tag
		String idTag = composeIDTag(bs.getLastGoodWP());
		// test file storage
		setError("");
		boolean fileHasPermission = updateExternalStorageState(context);
        if (MainActivity.debugOldTCXFile) Log.i(this.getClass().getSimpleName(), "fileHasPermission? " + (fileHasPermission?"yes":"no")
        + "isFileSpaceAvailable? " + (isFileSpaceAvailable(requiredStorageSpace)?"yes":"no"));
		if (fileHasPermission && isFileSpaceAvailable(requiredStorageSpace)) {
			File appFiles = new File(pathName);
            if (!appFiles.exists()) {
                appFiles.mkdirs();
            }
			// open Random Access File
				try {
					logout = new RandomAccessFile(new File(appFiles, outFileName), "rw");
				} catch (FileNotFoundException e) {
					setError(context.getString(R.string.error_opening_raf));
					Log.w(this.getClass().getSimpleName(), context.getString(R.string.error_opening_raf) + e.toString());
				}

			if (logout != null) {
				try {
					// write header
					logout.writeBytes(TCX_HEADER1 + TCX_HEADER2 + TCX_HEADER3);
					// write ID tag
					logout.writeBytes(ID_START + idTag + ID_END);
					// write Lap start tag
					logout.writeBytes(LAP_START_TIME + idTag + LAP_END);
					// write Track start tag
					logout.writeBytes(TRACK_START_TAG);
					// write first record
					writeTCXRecord(bs, nr);
					// write footer
					positionPointer(bs);
					saveTCXName_FooterLength();
				} catch (IOException e) {
					setError(context.getString(R.string.error_opening_log_file));
					Log.w(this.getClass().getSimpleName(), context.getString(R.string.error_opening_log_file) + e.toString());
				}
			}
		} else {
			setError(getSDError());
		}
	}

	private void saveTCXName_FooterLength() {
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		settings.edit().putString(TCX_LOG_FILE_NAME, outFileName).apply();
		settings.edit().putInt(TCX_LOG_FILE_FOOTER_LENGTH, outFileFooterLength).apply();
	}

	/**
	 * Compose the footer for the .tcx file including the summary and closing
	 * tags; also position the file pointer to write the next track-point record
	 * save the footer length so we can re-open the file and position the file
	 * pointer from Shared Preferences
	 */
	private void positionPointer(BikeStat bs) throws IOException {
		String footer = composeFooter(bs);
		logout.writeBytes(footer);
		// position pointer to write record
		long pointerPosition = logout.length() - footer.length();
		logout.seek(pointerPosition);
		//save the footer length so shared preferences can pick it up
		outFileFooterLength = footer.length();
	}

	/**
	 * the Id tag is used to identify the files in Strava, and possibly other
	 * sites like Garmin connect. It must match the time-stamp of the first
	 * track-point, so use the Location that opens the file
	 * @param tempLoc the first Location written to the file. Use to get time stamp
	 * @return the ID tag
	 */
	private String composeIDTag(Location tempLoc) {
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_Z_FORMAT, Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String idTag = sdf.format(tempLoc.getTime());
		return idTag.replace("+0000", "Z");
	}

	void closeTCXLogFile() {
        if (MainActivity.debugOldTCXFile) Log.i(this.getClass().getSimpleName(), "closeTCXLogFile()");
		setError("");
        long startTime = System.nanoTime();
		// test if the SD card is available and file exists
//		fileHasPermission = updateExternalStorageState();
		if (logout != null) {
			try {
				logout.close();
			} catch (IOException e) {
				setError(context.getString(R.string.error_closing_log_file));
				Log.w(this.getClass().getSimpleName(), context.getString(R.string.error_closing_log_file) + e.toString());
			}
		} else {
			setError(NO_LOGOUT);
		}
        if (MainActivity.debugfit) {
            Log.d(this.getClass().getSimpleName(), "closeTCXLogFile - duration: "
                    + String.format(FORMAT_4_3F, (System.nanoTime() - startTime) / 1000000000.) + " s");
        }
	}
	
	String composeTCXFileName(){
		String format = "M_d_yy-h_m_a";
		String suffix = FILENAME_SUFFIX;
		SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
		return sdf.format(new Date(System.currentTimeMillis())) + suffix;		
	}

	/**
	 * Determine if the current tcx file is too old, by reading the lastModified
	 * time, and we should not add to this tcx file, but open a new tcx file.
	 * Assume the file is not open.
	 * @param outFileName the tcx filename we are writing to
	 * @param resetTime how long should constitute and old file - user parameter
	 * @return true if the file is old
	 */
	boolean readTCXFileLastModTime(String outFileName, long resetTime) {
		boolean old = true;
		// test if the SD card is available
		setError("");
		boolean fileHasPermission = updateExternalStorageState(context);
		if (fileHasPermission) {
            if (MainActivity.debugOldTCXFile)Log.i(this.getClass().getSimpleName(), "readTCXFileLastModTime, fileHasPermission");
			// test files directory under Android/data/...
			File appFiles = new File(pathName);
            if (!appFiles.exists()) {
                appFiles.mkdirs();
            }
			if (appFiles.isDirectory()) {
                File tempFile = new File(appFiles, outFileName);
				if (tempFile.exists()) {
					// find out when we last wrote to the file
					// get a millisecond value of the current time
					SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
					long timeTag = Long.valueOf(settings.getString(LAST_MODIFIED, "0"));
					long now = new Date(System.currentTimeMillis()).getTime();
					old = Math.abs(now - timeTag) > resetTime * oneHour;
				} else {
					//file doesn't exist, so can't check date modified
					//just open a new tcx file with old = true
					setError(context.getString(R.string.file_not_found));
					old = true;
				}
			} else {
                if (MainActivity.debugOldTCXFile)Log.i(this.getClass().getSimpleName(), "readTCXFileLastModTime, SD card not mounted");
				setError(context.getString(R.string.sd_card_not_mounted_remove_usb_cable_));
			}
		} else {
			setError(getSDError());
		}
		return old;
	}

	/**
	 * write a track-point record to the open tcx file assuming that the file
	 * pointer is properly positioned; write track-point header
	 * @param bs is the BikeStat data about the trip
	 * @param nr is the NavRoute data about the route we are navigating
	 */
	@SuppressLint("DefaultLocale")
	void writeTCXRecord(BikeStat bs, NavRoute nr) {
		long startTime  = System.nanoTime();
		Location tempLoc = bs.getLastGoodWP();
		setError("");
		boolean fileHasPermission = updateExternalStorageState(context);
		if (fileHasPermission & (logout != null)) {
            if (MainActivity.debugOldTCXFile) Log.i(this.getClass().getSimpleName(), "writeTCXRecord()");
			try {
				if (nr.trackClosed) {
					writeTCXNewTrack(bs);
				}
				logout.writeBytes(TRACK_PT_START_TAG);
				// <Time>2013-03-12T15:22:29Z</Time>
				// write time
				//String formattedTime = sdf.format(tempLoc.getTime());
				logout.writeBytes(TIME_START_TAG);
                logout.writeBytes(sdf.format(tempLoc.getTime()).replace("+0000", "Z"));
                logout.writeBytes(TIME_END_TAG);
				int time = (int) (tempLoc.getTime()/1000 - DateTime.OFFSET/1000);
				logout.writeBytes(UF_TIME_START_TAG );
                logout.writeBytes(Integer.toString(time));
                logout.writeBytes(UF_TIME_END_TAG);
				// write position
				// <Position>
				// <LatitudeDegrees>37.4216169</LatitudeDegrees>
				// <LongitudeDegrees>-122.1806843</LongitudeDegrees>
				// </Position>
				logout.writeBytes(POSITION_START_TAG);
				logout.writeBytes(LAT_START_TAG);
                logout.writeBytes(String.format(FORMAT_7F, tempLoc.getLatitude()));
                logout.writeBytes(LAT_END_TAG);
				logout.writeBytes(LON_START_TAG);
                logout.writeBytes(String.format(FORMAT_7F, tempLoc.getLongitude()));
                logout.writeBytes(LON_END_TAG);
				int latSC = (int) Math.round(tempLoc.getLatitude() * semicircle_per_degrees);
				int lonSC = (int) Math.round(tempLoc.getLongitude() * semicircle_per_degrees);
				logout.writeBytes(LAT_SC_START_TAG);
                logout.writeBytes(Integer.toString(latSC));
                logout.writeBytes(LAT_SC_END_TAG);
				logout.writeBytes(LON_SC_START_TAG);
                logout.writeBytes(Integer.toString(lonSC));
                logout.writeBytes(LON_SC_END_TAG);
/*                int numSats = bs.getSatellitesInUse();
                logout.writeBytes(NUM_SATS_START_TAG);
                logout.writeBytes(Integer.toString(numSats));
                logout.writeBytes(NUM_SATS_END_TAG);
                logout.writeBytes(LOC_ACCURACY_START_TAG);
                int locAccuracy = Math.round(bs.getLocationAccuracy());
                logout.writeBytes(Integer.toString(locAccuracy));
                logout.writeBytes(LOC_ACCURACY_END_TAG);*/
				logout.writeBytes(POSITION_END_TAG);
				// write altitude
				// <AltitudeMeters>46.6000000</AltitudeMeters>
				logout.writeBytes(ALT_START_TAG);
                logout.writeBytes(String.format(FORMAT_2F, tempLoc.getAltitude()));
                logout.writeBytes(ALT_END_TAG);
				// write distance: <DistanceMeters>6557.8100586</DistanceMeters>
				logout.writeBytes(DIST_START_TAG);
				logout.writeBytes(String.format(FORMAT_2F, bs.getGPSTripDistance()));
				logout.writeBytes(DIST_END_TAG);
				// write heart rate
				// <HeartRateBpm xsi:type="HeartRateInBeatsPerMinute_t">
				// <Value>122</Value>
				// </HeartRateBpm>
				if (bs.hasHR) {
					logout.writeBytes(HR_START_TAG );
                    logout.writeBytes(Integer.toString(bs.getHR()) );
                    if (MainActivity.debugBLEData){
                        Log.w(this.getClass().getSimpleName(), "writing HR to TCX file: " + bs.getHR());
                    }
                    logout.writeBytes(HR_END_TAG);
				}
				// write cadence: <Cadence>92</Cadence>
				if (bs.hasCadence || bs.hasPowerCadence) {
					logout.writeBytes(CADENCE_START_TAG);
                    logout.writeBytes(Integer.toString(bs.getCadence()));
                    logout.writeBytes(CADENCE_END_TAG);
				}
				// write extensions - speed
				// <Extensions>
				// <TPX
				// xmlns="http://www.garmin.com/xmlschemas/ActivityExtension/v2"
				// CadenceSensor="Bike">
				// <Speed>8.0990000</Speed>
				// </TPX>
				// </Extensions>
				logout.writeBytes(EXT_START_TAG);
					// use GPS speed when no speed sensor
				logout.writeBytes(SPEED_START_TAG);
                logout.writeBytes(String.format(FORMAT_2F, bs.getSpeed()));
                logout.writeBytes(SPEED_END_TAG);
				if (bs.hasPower) {
					logout.writeBytes(POWER_START_TAG);
                    logout.writeBytes(Integer.toString((int) ((bs.getPower() + bs.getPrevPower()) / 2.)));
                    logout.writeBytes(POWER_END_TAG);
				}
				logout.writeBytes(EXT_END_TAG);				

				// finally write the end tag
				logout.writeBytes(TRACK_PT_END_TAG);
				positionPointer(bs);
				SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
				long timeTag = new Date(System.currentTimeMillis()).getTime();
				settings.edit().putString(LAST_MODIFIED, Long.toString(timeTag)).apply();
				saveTCXName_FooterLength();
			} catch (IOException e) {
				setError(context.getString(R.string.error_writing_data_to_log_file));
				Log.w(this.getClass().getSimpleName(),
						context.getString(R.string.error_writing_data_to_log_file) + " trkpt " + e.toString());
			}
		} else if (!getSDError().equals("")) {
			setError(getSDError());
			closeTCXLogFile();
		} else {
			setError(NO_LOGOUT);
		}
		if (MainActivity.debugRefreshTiming){
            Log.w(this.getClass().getName(), "writeTrack time: " + String.format(FORMAT_3_1F, (System.nanoTime() - startTime)/1000000.) + " msec");
        }
	}

	/** When paused, end the old Track and start a new one 
	 * @param bs the BikeStat data about the trip
	 * */
	private void writeTCXNewTrack(BikeStat bs) {
		// when paused, end the old Track and start a new one
		// assume that the file pointer is properly positioned to start with
		setError("");
		boolean fileHasPermission = updateExternalStorageState(context);
		if (fileHasPermission & (logout != null)) {
			try {
				logout.writeBytes(TRACK_END_TAG);
				logout.writeBytes(TRACK_START_TAG);
				positionPointer(bs);
				SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
				long timeTag = new Date(System.currentTimeMillis()).getTime();
				settings.edit().putString(LAST_MODIFIED, Long.toString(timeTag)).apply();
				saveTCXName_FooterLength();
			} catch (IOException e) {
				setError(context.getString(R.string.error_writing_data_to_log_file));
				Log.w(this.getClass().getSimpleName(),
						context.getString(R.string.error_writing_data_to_log_file) + " trkpt " + e.toString());
			}
		} else if (!getSDError().equals("")) {
			setError(getSDError());
			closeTCXLogFile();
		} else {
			setError(NO_LOGOUT);
		}
	}

	private boolean updateExternalStorageState(Context mContext) {
		boolean mExtStorWriteable = Utilities.isExternalStorageWritable(mContext);
        setStorageError("");
        if (!mExtStorWriteable) {
            closeTCXLogFile();
            setStorageError(mContext.getString(R.string.can_t_write_to_external_storage));
        }
		return mExtStorWriteable;
	}

	private void setStorageError(String sdError) {
		this.sdError= sdError;		
	}
	
	private String getSDError() {
		return sdError;
	}
	
	public String getError() {
		return error;
	}

	private void setError(String error) {
		this.error = error;
	}


}
