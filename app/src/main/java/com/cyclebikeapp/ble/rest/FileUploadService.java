package com.cyclebikeapp.ble.rest;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Header;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.mime.TypedFile;

/**
 * Created by TommyD on 12/10/2015.
 * these are the minimum @Part elements required for the Strava API
 * the String authorization is the OAuth accessToken
 * the TypedFile file is the complete path to the .tcx, .fit, etc file to upload
 * the String description is the corresponding file type "tcx", "fit", etc
 * reference https://futurestud.io/blog/retrofit-how-to-upload-files
 */
public interface FileUploadService {

    @Multipart
    @POST("/uploads")
    void upload(@Header("Authorization") String authorization,
            @Part("file") TypedFile file,
            @Part("data_type") String description,
            @Part("name") String activity_title,
            @Part("description") String blurb,
            Callback<Response> cb);
}
