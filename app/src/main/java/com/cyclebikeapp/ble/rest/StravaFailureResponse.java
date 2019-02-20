package com.cyclebikeapp.ble.rest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by TommyD on 12/11/2015.
 *
 */
public class StravaFailureResponse {
    private static final String MESSAGE = "message";
    private static final String ERRORS = "errors";
    private static final String FIELD = "field";
    private static final String RESOURCE = "resource";
    private static final String CODE = "code";
    private static final String ERROR = "error";
    private static final String DUPLICATE = "duplicate";
    private static final String ERROR_PROCESSING = "error processing";
    private static final String UNRECOGNIZED_FILE_TYPE = "Unrecognized file type";
    private static final String OTHER_ERROR = "Other error";
    private static final String PROCESSING_ERROR = "processing error";
    private static final String DUPLICATE_ACTIVITY = "duplicate activity";
    public String message;
    private String error_field;
    public String error_code;
    private String error_resource;

    /**
     *  * response type 1
     * {"message":"Authorization Error","errors":[{"resource":"Athlete","field":"access_token","code":"invalid"}]}
     * or
     * response type 2
     * {"id":499265057,"external_id":"11_29_2015-8_39_20_AM_CB_history.fit",
     * "error":"11_29_2015-8_39_20_AM_CB_history.fit duplicate of activity 448326251",
     * "status":"There was an error processing your activity.","activity_id":null}
     * @param s is derived from the Retrofit response string from a failed attempt to upload a file
     *          the field error_code contains the details of the error
    */

    public StravaFailureResponse(String s) {
        message = "";
        error_field = "";
        error_code = "";
        error_resource = "";
        //look for response type 1
         try {
            JSONObject obj = new JSONObject(s);
            message = obj.getString(MESSAGE);
            //Log.v("CycleBike", "failureResponse type 1 message: " + message);
            JSONArray errors = obj.getJSONArray(ERRORS);
            JSONObject errorObj = errors.getJSONObject(0);
            error_field = errorObj.getString(FIELD);
            error_resource = errorObj.getString(RESOURCE);
            error_code = error_field + " " + errorObj.getString(CODE);
        } catch (JSONException e) {
            //e.printStackTrace();
        }
        // maybe response type 2
        if (("").equals(message)) {
            try {
                JSONObject oObj = new JSONObject(s);
                String error = oObj.getString(ERROR);
                //Log.v("CycleBike", "failureResponse type 2 error: " + error);
                if (error.contains(DUPLICATE)) {
                    error_code = DUPLICATE_ACTIVITY;
                } else if (error.contains(ERROR_PROCESSING)) {
                    error_code = PROCESSING_ERROR;
                } else if (error.contains(UNRECOGNIZED_FILE_TYPE)){
                    error_code = UNRECOGNIZED_FILE_TYPE;
                } else error_code = OTHER_ERROR;
            } catch (JSONException e) {
               // e.printStackTrace();
            }
        }
    }

}
