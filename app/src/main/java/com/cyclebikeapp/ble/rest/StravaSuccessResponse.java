package com.cyclebikeapp.ble.rest;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by TommyD on 12/11/2015.
 * used to convert Retrofit Response to JSONObject that we can parse to extract information we want
 */
public class StravaSuccessResponse {
    private static final String EXTERNAL_ID = "external_id";
    private static final String ERROR = "error";
    private static final String STATUS = "status";
    private static final String ACTIVITY_ID = "activity_id";
    private static final String ID = "id";
    private int id = -1;
    private String filename;
    private String error = "";
    private String status;
    private int activityID = -1;
// Example response
// {"id": 16486788,"external_id": "test.fit","error": null,"status": "Your activity is still being processed.","activity_id": null}

    /**
     *
     * @param s is derived from the Retrofit response string from a successful attempt to upload a file
     */
    public StravaSuccessResponse(String s) {
        try {
            JSONObject obj = new JSONObject(s);
            String temp1 = obj.getString(ID);
            if (temp1 != null && !("").equals(temp1)) try {
                id = Integer.parseInt(temp1);
            } catch (NumberFormatException e) {
                //e.printStackTrace();
            }
            filename = obj.getString(EXTERNAL_ID);
            error = obj.getString(ERROR);
            status = obj.getString(STATUS);
            String temp = obj.getString(ACTIVITY_ID);
            if (temp != null && !("").equals(temp)) try {
                activityID = Integer.parseInt(temp);
            } catch (NumberFormatException e) {
               // e.printStackTrace();
            }
        } catch (JSONException e) {
            //e.printStackTrace();
        }
    }

}
