package com.cyclebikeapp.ble.rest;

import java.io.File;

import retrofit.mime.TypedFile;

/**
 * Created by TommyD on 12/11/2015.
 *
 */

public class ActivityFileParams {
    private final String filename;
    public final String dataType;
    public final String authorization;
    public final TypedFile typedFile;
    public final int notificationNumber;

 /**
  * Construct the parameters needed for uploading file to Strava
  *  @param filename    is the file to be uploaded including the path
  * @param accessToken is the accessToken to the user account
  *                    authorization field contains the text for the @Header("Authorization")
  *                    typedFile field is used in the @Part("file")
  * @param notificationNumber keep track of uploaded file via notification number
  */
 public ActivityFileParams(String filename, String accessToken, int notificationNumber) {
        this.authorization = "Bearer " + accessToken;
        this.filename = filename;
        this.notificationNumber = notificationNumber;
        dataType = findDataType();
        typedFile = new TypedFile("multipart/form-data", new File(filename));

    }


    /**
     * Return the type of the data file
     *
     * @return either "fit" or "tcx" or "gpx" depending on the filename suffix. Returns empty if other type
     */
    private String findDataType() {
        // possible values: fit, fit.gz, tcx, tcx.gz, gpx, gpx.gz
        String fileType = "";
        if (filename.endsWith(".fit")) {
            fileType = "fit";
        } else if (filename.endsWith(".tcx")) {
            fileType = "tcx";
        } else if (filename.endsWith(".fit.gz")) {
            fileType = "fit.gz";
        } else if (filename.endsWith(".tcx.gz")) {
            fileType = "tcx.gz";
        } else if (filename.endsWith(".gpx")) {
            fileType = "gpx";
        } else if (filename.endsWith(".gpx.gz")) {
            fileType = "gpx.gz";
        }
        //Log.i("CycleBike", "file_type - " + fileType);
        return fileType;
    }
}
