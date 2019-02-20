package com.cyclebikeapp.ble;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.os.Environment;
import android.util.Log;

import com.garmin.fit.Activity;
import com.garmin.fit.ActivityMesg;
import com.garmin.fit.DateTime;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.Event;
import com.garmin.fit.EventType;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.LapMesg;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import static com.cyclebikeapp.ble.Constants.FORMAT_4_3F;

@SuppressWarnings("ResultOfMethodCallIgnored")
class FITLogFile {
    private final String invalidFileMessg;
    private final String saxExceptionMessg;
    private final String fileNotFoundMessg;
    private final String latlonErrorMessg;
    private final String cantWriteMessg;
    private long versionNum;

    private static final String ERROR_OPENING_FILE = "Error opening file ";
    private static final String ENCODE_NULL = "encode null";
    private static final String ERROR_CLOSING_ENCODE = "Error closing encode";
    private static final Integer FIT_MANUFACTURER_DEVELOPMENT = 255;
    private static final String TCX = ".tcx";
    private static final String FIT = ".fit";
    private static final String PARSER_CONFIG = " ParserConfig";
    private static final String IO_EXCEPTION = " IOException";
    private static final String FIT_RUNTIME_EXCEPTION = "Fit Runtime Exception";
    private final String pathName = Environment.getExternalStorageDirectory().getAbsolutePath()
            + Constants.ACTIVITY_FILE_PATH;
    // semicircle per degrees is 2^31 / 180 degrees
    private static final double semicircle_per_degrees = 11930464.711111111111111111111111;

    private FileEncoder encode;
    private String error = "";
    private String sdError = "";
    private final MyTCXSAXHandler tcxSAXHandler = new MyTCXSAXHandler();
    private DateTime activityEndTime;
    private DateTime activityStartTime;
    private float totalTimerTime;
    private float totalDistance;
    private float avgSpeed;
    private float maxSpeed;
    private float avgCadence;
    private float maxCadence;
    private float avgHeartRate;
    private float maxHeartRate;
    private boolean fileEncoderBusy = false;
    private final boolean fileHasPermission;
    private Integer avgPower;
    private Integer maxPower;

    FITLogFile(Context context) {
        invalidFileMessg = context.getString(R.string.invalid_file_);
        saxExceptionMessg = context.getString(R.string._saxexception);
        fileNotFoundMessg = context.getString(R.string.file_not_found);
        latlonErrorMessg = context.getString(R.string._this_file_has_lat_long_errors);
        cantWriteMessg = context.getString(R.string.can_t_write_to_external_storage);
        fileHasPermission = updateExternalStorageState(context);
        PackageInfo pInfo;
        versionNum = 1L;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionNum = (long) pInfo.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        initSummaryValues();
    }

    private void initSummaryValues() {
        activityEndTime = new DateTime(0);
        activityStartTime = new DateTime(0);
        totalTimerTime = 0;
        totalDistance = 0;
        avgSpeed = 0;
        maxSpeed = 0;
        avgCadence = 0;
        maxCadence = 0;
        avgHeartRate = 0;
        maxHeartRate = 0;
        avgPower = 0;
        maxPower = 0;
    }

    /**
     * When opening a new activity file, use the first Location from BikeStat to
     * open a new fit file. Assume the tcx file has just been opened and the
     * outFileName has been composed with the new timestamp.
     *
     * @param bs is the BikeStat values
     * @return any error from opening a new FileEncoder
     */

    String openNewFIT(BikeStat bs) {
        if (MainActivity.debugfit) {Log.i(this.getClass().getSimpleName(), "openNewFIT()");}
        setError("");
        //reset the summary
        initSummaryValues();
        // retrieve the new tcx file name
        String fitOutFileName = bs.tcxLog.outFileName;
        // Get the new data record. We don't open a new file until there is a new record from Location Listener
        Location tempLoc = bs.getLastLocation();
        fileEncoderBusy = true;
        if (fileHasPermission) {
            try {
                // replace the ".tcx" with ".fit"
                fitOutFileName = stripFilePath(delTCXFITSuffix(fitOutFileName) + FIT);
                if (MainActivity.debugfit) {Log.i(this.getClass().getSimpleName(), "new fitOutFileName: " + fitOutFileName);}
                // get tcx filepath
                File tcxDir = new File(pathName);
                if (!tcxDir.exists()) {
                    tcxDir.mkdirs();
                }
                encode = new FileEncoder(new File(tcxDir, fitOutFileName), Fit.ProtocolVersion.V1_0);
            } catch (FitRuntimeException e) {
                setError(ERROR_OPENING_FILE + fitOutFileName);
                e.printStackTrace();
                fileEncoderBusy = false;
                return error;
            }
            // adjust Garmin zero time offset
            activityStartTime = new DateTime(tempLoc.getTime() / 1000 - DateTime.OFFSET / 1000);
            activityEndTime = activityStartTime;
            // compose FileIdMesg and place it at the start of the fit file
            // add first RecordMesgs from BikeStat new location
            try {
                encode.write(composeFileIdMesg(activityStartTime)); // Encode the FileIDMesg
            } catch (FitRuntimeException e) {
                setError(FIT_RUNTIME_EXCEPTION);
                e.printStackTrace();
                fileEncoderBusy = false;
            }
            fileEncoderBusy = false;

        } else {
            setError(getSDError());
        }
        return error;
    }

    /**
     * When re-opening an activity file, read in the data from the tcx file and
     * write over the old fit file. A fit file cannot just be appended. Returns any error
     *
     * @param outFileName is the name of the activity file in tcx format. String
     *                    includes the entire path.
     * @return any error from parsing the tcx file
     */
    String reOpenFitFile(String outFileName) {
        if (MainActivity.debugfit) {
            Log.i(this.getClass().getSimpleName(), "reOpenFitFile() - outFileName: " + outFileName + " path: " + pathName);
        }
        error = "";
        File appFiles = new File(pathName);
        try {
            if (!appFiles.exists()) {
                appFiles.mkdirs();
            }
        } catch (SecurityException ignore) {
        }
        error = parseTCXFile(outFileName);
        //write summary values from tcxSAXHandler
        writeSummaryValuesFromParsing();
        // encode all the RecordMesg's from the tcxSAX handler
        fileEncoderBusy = true;
        encodeFITLogFileFromTCX(outFileName);
        fileEncoderBusy = false;
        return error;
    }

    /**
     * When re-opening an activity file, read in the data from the tcx file and
     * write over the old fit file. A fit file cannot just be appended. Use SAX
     * parser to read the given .tcx file and extract data to an ArrayList of
     * RecordMesg; returns any error
     *
     * @param outFileName is the name of the activity file in tcx format.
     * @return any error from parsing the tcx file
     */
    private String parseTCXFile(String outFileName) {
        long timingStartTime = System.nanoTime();
        setError("");
        BufferedReader r = null;
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            XMLReader xr = parser.getXMLReader();
            xr.setContentHandler(tcxSAXHandler);
            xr.setErrorHandler(tcxSAXHandler);
            // uses the entire path, not just filename
            r = new BufferedReader(new FileReader(new File(pathName, outFileName)));
            xr.parse(new InputSource(r));
        } catch (SAXException e) {
            e.printStackTrace();
            setError(invalidFileMessg + saxExceptionMessg);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            setError(fileNotFoundMessg + " " + outFileName);
        } catch (IOException e) {
            e.printStackTrace();
            setError(invalidFileMessg + IO_EXCEPTION);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            setError(invalidFileMessg + PARSER_CONFIG);
        } catch (NumberFormatException e) {
            setError(invalidFileMessg + latlonErrorMessg);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } // finally
        if (MainActivity.debugfit) {
            Log.d(this.getClass().getSimpleName(), "parseTCXLogFile - # records: " + tcxSAXHandler.handlersRecordMesgs.size());
            Log.d(this.getClass().getSimpleName(), "parseTCXLogFile - duration: " + String.format(FORMAT_4_3F, (System.nanoTime() - timingStartTime) / 1000000000.) + " s");
        }
        return error;
    }

    /**
     * When re-opening the fit file, update the summary values so they can be
     * written when the file is closed. The data comes from the tcxSAXHandler's
     * summary data
     */
    private void writeSummaryValuesFromParsing() {
        if (MainActivity.debugfit) Log.i(this.getClass().getSimpleName(), "writeSummaryValuesFromParsing()");
        totalTimerTime = tcxSAXHandler.totalTimerTime;
        totalDistance = tcxSAXHandler.totalDistance;
        avgSpeed = tcxSAXHandler.avgSpeed;
        maxSpeed = tcxSAXHandler.maxSpeed;
        avgCadence = tcxSAXHandler.avgCadence;
        maxCadence = tcxSAXHandler.maxCadence;
        avgHeartRate = tcxSAXHandler.avgHeartRate;
        maxHeartRate = tcxSAXHandler.maxHeartRate;
        avgPower = (int) tcxSAXHandler.avgPower;
        maxPower = (int) tcxSAXHandler.maxPower;
    }

    /**
     * Write all information to the actual .fit output file from parsing the tcx
     * file. This is done when re-opening the tcx file on restarting the app if
     * the tcx file isn't old
     *
     * @param fitOutFileName is the name of the tcx activity file
     */
    private void encodeFITLogFileFromTCX(String fitOutFileName) {
        if (MainActivity.debugfit) {
            Log.i(this.getClass().getSimpleName(), "encodeFITLogFileFromTCX() - " + "fitOutFileName: " + fitOutFileName);
        }
        // test file storage
        long timingStartTime = System.nanoTime();
        setError("");
        if (fileHasPermission) {
            try {
                // replace the ".tcx" with ".fit"
                fitOutFileName = stripFilePath(delTCXFITSuffix(fitOutFileName) + FIT);
                if (MainActivity.debugfit) {
                    Log.i(this.getClass().getSimpleName(), "encodeFITLogFileFromTCX() - " + "mod fitOutFileName: " + fitOutFileName);
                }
                // get tcx filepath from shared prefs, if null, use default
                File appFiles = new File(pathName);
                if (!appFiles.exists()) {
                    appFiles.mkdirs();
                }
                encode = new FileEncoder(new File(pathName, fitOutFileName), Fit.ProtocolVersion.V1_0);
            } catch (Exception e) {
                setError(ERROR_OPENING_FILE + fitOutFileName);
                e.printStackTrace();
                return;
            }
            if (tcxSAXHandler.handlersRecordMesgs == null
                    || tcxSAXHandler.handlersRecordMesgs.size() == 0) {
                if (MainActivity.debugfit) {Log.i(this.getClass().getSimpleName(), "encodeFITLogFileFromTCX() - no records in tcx file");}
                return;
            }
            setActivityStartTime(tcxSAXHandler.handlersRecordMesgs.get(0));
            setActivityEndTime(tcxSAXHandler.handlersRecordMesgs.get(tcxSAXHandler.handlersRecordMesgs.size() - 1));
            // compose FileIdMesg and place it at the start of the fit file
            // add RecordMesgs from parsing tcx file
            try {
                encode.write(composeFileIdMesg(activityStartTime)); // Encode the FileIDMesg
            } catch (FitRuntimeException e) {
                setError(FIT_RUNTIME_EXCEPTION);
            }
            encodeFITRecordsFromParsing(tcxSAXHandler.handlersRecordMesgs);
            //			encode.write(composeDeviceIdMesg(activityStartTime));
            if (MainActivity.debugfit) {
                Log.d(this.getClass().getSimpleName(), "encodeFITLogFile - # records: " + tcxSAXHandler.handlersRecordMesgs.size());
                Log.d(this.getClass().getSimpleName(), "encodeFITLogFile - duration: " + String.format(FORMAT_4_3F, (System.nanoTime() - timingStartTime) / 1000000000.) + " s");
            }
        } else {
            setError(getSDError());
        }
    }

    /**
     * Encode all the RecordMesgs to the fit file when reopening the tcx file.
     *
     * @param handlersRecordMesgs is an ArrayList of RecordMesg received from the SAXParser
     */
    private void encodeFITRecordsFromParsing(ArrayList<RecordMesg> handlersRecordMesgs) {
        try {
            for (RecordMesg mRecordMesg : handlersRecordMesgs) {
                encode.write(mRecordMesg);
            }
        } catch (FitRuntimeException e) {
            setError(FIT_RUNTIME_EXCEPTION);
        }
    }

    /**
     * Write data values from BikeStat and the new Location to a RecordMesg that
     * can be written to the fit file
     *
     * @param bs the BikeStat structure containing the new data
     */
    void writeRecordMesg(BikeStat bs) {
        // Location could get events before we've opened a new fit file, or re-opened and parsed fit file
        // ignore attempts to write until encode FileEncoder has been initialized
        if (encode == null) {
            if (MainActivity.debugfit) { Log.w(this.getClass().getSimpleName(), "writeRecordMesg(): encode == null"); }
            return;
        }
        if (fileEncoderBusy) {
            return;
        }
        if (MainActivity.debugfit) { Log.i(this.getClass().getSimpleName(), "writeRecordMesg()"); }

        //write summary values like TripTime, TripDistance, averages. maximums every time
        RecordMesg aRecordMesg;
        aRecordMesg = composeRecordMesg(bs);
        setActivityEndTime(aRecordMesg);
        saveSummaryValuesFromNewRecord(bs);
        try {
            encode.write(aRecordMesg);
        } catch (FitRuntimeException e) {
            if (MainActivity.debugfit) { e.printStackTrace(); }
            setError(FIT_RUNTIME_EXCEPTION);
        }
    }

    /**
     * Compose data values from BikeStat and the new Location to a RecordMesg that
     * can be written to the fit file
     *
     * @param bs the BikeStat structure containing the new data
     */
    private RecordMesg composeRecordMesg(BikeStat bs) {
        RecordMesg aRecordMesg = new RecordMesg();
        //write cadence, HR, power values, use speed sensor, etc when available
        aRecordMesg.setHeartRate((short) bs.getHR());
        // write cadence: <Cadence>92</Cadence>
        aRecordMesg.setCadence((short) bs.getCadence());
        aRecordMesg.setPower((int) ((bs.getPower() + bs.getPrevPower()) / 2.) );
        Location tempLoc = bs.getLastGoodWP();
        // write time
        int time = (int) (tempLoc.getTime() / 1000 - DateTime.OFFSET / 1000);
        aRecordMesg.setTimestamp(new DateTime(time));
        // write position
        int latSC = (int) Math.round(tempLoc.getLatitude() * semicircle_per_degrees);
        aRecordMesg.setPositionLat(latSC);
        int lonSC = (int) Math.round(tempLoc.getLongitude() * semicircle_per_degrees);
        aRecordMesg.setPositionLong(lonSC);
        // write altitude
        aRecordMesg.setAltitude((float) tempLoc.getAltitude());
        // write distance
        aRecordMesg.setDistance((float) bs.getGPSTripDistance());
        aRecordMesg.setSpeed((float) bs.getSpeed());
        //aRecordMesg.setGpsAccuracy((short) Math.round(bs.getLocationAccuracy()));
        return aRecordMesg;
    }

    /**
     * When leaving the main activity, close the fit file so it will be valid.
     * This includes writing all the summary messages
     * @param outFileName for debugging, may need name of file being closed
     */
    String closeFitFile(String outFileName) {
        if (MainActivity.debugfit) {Log.i(this.getClass().getSimpleName(), "closeFitFile()");}
        long startTime = System.nanoTime();
        // if encode is null, just return
        if (encode == null) {
            if (MainActivity.debugfit) {Log.w(this.getClass().getSimpleName(), "closeFitFile() - encode null");}
            setError(ENCODE_NULL);
            return getError();
        }
        fileEncoderBusy = true;
        try {
            encode.write(composeFITLapMesg());
            encode.write(composeFITSessionMesg());
            encode.write(composeFITActivityMesg());
            encode.close();
        } catch (FitRuntimeException e) {
            setError(ERROR_CLOSING_ENCODE);
            if (MainActivity.debugfit) {Log.e(this.getClass().getSimpleName(), "closeFitFile() - Error closing encode.");}
            e.printStackTrace();
        }
        if (MainActivity.debugfit) {
            Log.d(this.getClass().getSimpleName(), "closeFITLogFile - duration: "
                    + String.format(FORMAT_4_3F, (System.nanoTime() - startTime) / 1000000000.) + " s");
        }
        fileEncoderBusy = false;
        return getError();
    }

    /**
     * set the end time of this activity when parsing an old tcx file, or adding
     * a record to the file
     *
     * @param theRecordMesg is the last RecordMesg in the activity file
     */
    private void setActivityEndTime(RecordMesg theRecordMesg) {
        activityEndTime = theRecordMesg.getTimestamp();
    }

    /**
     * Set the start time of this activity when parsing an old tcx file, or
     * opening a new tcx file.
     *
     * @param theRecordMesg is the first RecordMesg in the activity file
     */
    private void setActivityStartTime(RecordMesg theRecordMesg) {
        activityStartTime = theRecordMesg.getTimestamp();
    }

    /**
     * For each new record we write, update the summary values so they can be
     * written when the file is closed
     *
     * @param bs BikeStat structure containing all the data
     */
    private void saveSummaryValuesFromNewRecord(BikeStat bs) {
        totalTimerTime = (float) bs.getGPSRideTime();
        totalDistance = (float) bs.getGPSTripDistance();
        avgSpeed = (float) bs.getAvgSpeed();
        maxSpeed = (float) bs.getMaxSpeed();
        avgCadence = bs.getAvgCadence();
        maxCadence = bs.getMaxCadence();
        avgHeartRate = bs.getAvgHeartRate();
        maxHeartRate = bs.getMaxHeartRate();
        avgPower = bs.getAvgPower();
        maxPower = bs.getMaxPower();
    }

    /**
     * We'll write the activity message just before closing the encoded file
     *
     * @return the ActivityMesg
     */
    private ActivityMesg composeFITActivityMesg() {
        ActivityMesg mActivityMesg = new ActivityMesg();
        // to upload to Strava the Timestamp may have to be the start time
        mActivityMesg.setTimestamp(activityStartTime);
        mActivityMesg.setEvent(Event.SESSION);
        mActivityMesg.setEventType(EventType.STOP);
        mActivityMesg.setTotalTimerTime(totalTimerTime);
        mActivityMesg.setNumSessions(1);
        mActivityMesg.setType(Activity.AUTO_MULTI_SPORT);
        return mActivityMesg;
    }

    /**
     * We'll write the lap message just before closing the encoded file
     *
     * @return the LapMesg
     */
    private LapMesg composeFITLapMesg() {
// use summary values from handler
        //These are the ones used by Strava
//		lap 	timestamp
//				total_elapsed_time
//				total_timer_time
//				total_distance
//				total_ascent
        LapMesg mLapMesg = new LapMesg();
        mLapMesg.setStartTime(new DateTime(activityStartTime));
        // to upload to Strava the Timestamp may have to be the start time
        mLapMesg.setTimestamp(new DateTime(activityStartTime));
        float totalElapsedTime = (float) (activityEndTime.getTimestamp() - activityStartTime.getTimestamp());
        mLapMesg.setTotalElapsedTime(totalElapsedTime);
        // TotalTimerTime excludes pauses; "ride-time"
        mLapMesg.setTotalTimerTime(totalTimerTime);
        mLapMesg.setTotalDistance(totalDistance);
        mLapMesg.setAvgSpeed(avgSpeed);
        mLapMesg.setMaxSpeed(maxSpeed);
        mLapMesg.setAvgCadence((short) avgCadence);
        mLapMesg.setMaxCadence((short) maxCadence);
        mLapMesg.setAvgHeartRate((short) avgHeartRate);
        mLapMesg.setMaxHeartRate((short) maxHeartRate);
        mLapMesg.setAvgPower(avgPower);
        mLapMesg.setMaxPower(maxPower);
        mLapMesg.setEvent(Event.SESSION);
        mLapMesg.setEventType(EventType.STOP);
        return mLapMesg;
    }

    /**
     * Here's the data summary written just before closing the file
     *
     * @return the SessionMesg
     */
    private SessionMesg composeFITSessionMesg() {
        //use summary values from handler
        //These are the ones used by Strava
//		session 	sport
//					total_elapsed_time
//					total_timer_time
//					total_distance
//					total_ascent
        SessionMesg mSessionMesg = new SessionMesg();
        mSessionMesg.setSport(com.garmin.fit.Sport.CYCLING);
        float totalElapsedTime = (float) (activityEndTime.getTimestamp()
                - activityStartTime.getTimestamp());
        mSessionMesg.setTotalElapsedTime(totalElapsedTime);
        // TotalTimerTime excludes pauses; "ride-time"
        mSessionMesg.setTotalTimerTime(totalTimerTime);
        mSessionMesg.setTotalDistance(totalDistance);
        // to upload to Strava the Timestamp may have to be the start time
        mSessionMesg.setTimestamp(new DateTime(activityStartTime));
        mSessionMesg.setStartTime(new DateTime(activityStartTime));
        mSessionMesg.setAvgSpeed(avgSpeed);
        mSessionMesg.setMaxSpeed(maxSpeed);
        mSessionMesg.setAvgCadence((short) avgCadence);
        mSessionMesg.setMaxCadence((short) maxCadence);
        mSessionMesg.setAvgPower(avgPower);
        mSessionMesg.setMaxPower(maxPower);
        mSessionMesg.setEvent(Event.SESSION);
        mSessionMesg.setEventType(EventType.STOP);
        return mSessionMesg;
    }

    /**
     * Here's the data summary written at the beginning of the file
     *
     * @return thefileIdMesg
     */
    private FileIdMesg composeFileIdMesg(DateTime activityStartTime) {
        // Generate FileIdMessage
        // Every FIT file MUST contain a 'File ID' message as the first message
        if (MainActivity.debugfit) Log.d(this.getClass().getSimpleName(), "composeFileIdMesg()");
        FileIdMesg fileIdMesg = new FileIdMesg();
        fileIdMesg.setTimeCreated(activityStartTime);
        fileIdMesg.setManufacturer(FIT_MANUFACTURER_DEVELOPMENT);
        fileIdMesg.setProductName(Constants.APP_NAME);
        fileIdMesg.setType(com.garmin.fit.File.ACTIVITY);
        fileIdMesg.setProduct(1);
        fileIdMesg.setSerialNumber(versionNum);
                return fileIdMesg;
    }

    private DeviceInfoMesg composeDeviceIdMesg(DateTime activityStartTime) {
        // deviceInfo - needed for Garmin Connect upload...
        DeviceInfoMesg deviceInfo = new DeviceInfoMesg();
        deviceInfo.setTimestamp(activityStartTime);
        deviceInfo.setManufacturer(FIT_MANUFACTURER_DEVELOPMENT);
        return deviceInfo;
    }

    /**
     * delete path prefix; private storage names can't have path symbols.
     * Filename returned from ShowFileList has path characters in it
     *
     * @param fileName the file name that possibly has path characters in it
     * @return the fileName without path characters
     */
    String stripFilePath(String fileName) {

        if (fileName != null) {
            int start = fileName.lastIndexOf("/") + 1;
            int end = fileName.length();
            // Log.i(this.getClass().getName(), "stripPath start: " + start + " end: " + end);
            if ((end - start) <= 0) {
                fileName = "";
            } else {
                fileName = fileName.substring(start, end);
            }
        }
        return fileName;
    }

    /**
     * Delete the .tcx or .fit suffix on the file name. Use the same filename
     * for tcx and fit files, but will add the right suffix when encoding fit or
     * tcx files.
     *
     * @param fileName the file name to strip
     * @return the filenam stripped of the suffix
     */
    String delTCXFITSuffix(String fileName) {
        // delete the .tcx suffix on the fileName
        int end = fileName.indexOf(TCX, 0);
        if (end > 0) {
            fileName = fileName.substring(0, end);
        }
        // now test for ".fit" suffix and delete that
        end = fileName.indexOf(FIT, 0);
        if (end > 0) {
            fileName = fileName.substring(0, end);
        }
        return fileName;
    }

    private boolean updateExternalStorageState(Context context) {
        boolean mExtStorWriteable = Utilities.isExternalStorageWritable(context);
        setStorageError("");
        if (!mExtStorWriteable) {
            setStorageError(cantWriteMessg);
        }
        return mExtStorWriteable;
    }

    private void setStorageError(String sdError) {
        this.sdError = sdError;
    }

    private String getSDError() {
        return sdError;
    }

    private String getError() {
        return error;
    }

    private void setError(String error) {
        this.error = error;
    }

    /**
     * Small activity files can accumulate. Delete any fit file with size < 500 bytes
     *
     * @param bs                 BikeStat Class to retrieve the current logOut filename, which we don't want to purge
     * @param chosenActivityFile the file we are trying to share;
     *                           it may be small so don't purge it until it has been shared
     */
    void purgeSmallActivityFiles(final BikeStat bs, final String chosenActivityFile) {
        new Thread(new Runnable() {
            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            public void run() {
                List<File> fitFiles = mLoadActivityFileList();
                String logOutName = delTCXFITSuffix(bs.tcxLog.outFileName);
                String bareChosenActivityFile = stripFilePath(delTCXFITSuffix(chosenActivityFile));
                if (MainActivity.debugfit) {
                    Log.w(this.getClass().getSimpleName(), "purgeSmall - logOutName: " + logOutName);
                    Log.w(this.getClass().getSimpleName(), "purgeSmall - chosenActivityFile: " + bareChosenActivityFile);
                }
                for (File aFitFile : fitFiles) {
                    if (!aFitFile.isDirectory()
                            // don't purge the current logout file
                            && !aFitFile.getName().contains(logOutName)
                            // don't purge the file we're sharing, exclude this test if no ActivityFile chosen
                            && (!aFitFile.getName().contains(bareChosenActivityFile) || ("").equals(bareChosenActivityFile))) {

                        try {
                            if (MainActivity.debugfit) {
                                Log.w(this.getClass().getSimpleName(), "deleting .fit file: " + aFitFile.toString());
                            }
                            aFitFile.delete();
                            // also delete the .tcx file
                            String theTCXFile = delTCXFITSuffix(aFitFile.toString()) + ".tcx";
                            if (MainActivity.debugfit) {
                                Log.w(this.getClass().getSimpleName(), "deleting .tcx file: " + theTCXFile);
                            }
                            new File(theTCXFile).delete();
                        } catch (SecurityException ignore) {

                        }
                    }
                }
            }
        }).start();
    }

    /**
     * Get a list of small .fit activity files in the directory. We only need to look at .fit files,
     * the corresponding .tcx file will also be deleted.
     *
     * @return a List of the Files
     */
    private List<File> mLoadActivityFileList() {
        File activityFilePath = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + Constants.ACTIVITY_FILE_PATH);
        List<File> fileFileList = new ArrayList<>();
        try {
            if (!activityFilePath.exists()){
                activityFilePath.mkdirs();
            }
        } catch (SecurityException ignore) {
        }
        if (activityFilePath.exists() && activityFilePath.canRead() && activityFilePath.isDirectory()) {
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    // look for .fit files with size < 1.5 kB
                    return filename.contains(FIT) && (sel.length() < Constants.SMALL_FIT_FILE_SIZE);
                }
            };
            fileFileList = Arrays.asList(activityFilePath.listFiles(filter));
        }
        return fileFileList;
    }// loadActivityFileList()

    boolean isFileEncoderBusy() {
        return fileEncoderBusy;
    }

    void setFileEncoderBusy(boolean fileEncoderBusy) {
        this.fileEncoderBusy = fileEncoderBusy;
    }

}
