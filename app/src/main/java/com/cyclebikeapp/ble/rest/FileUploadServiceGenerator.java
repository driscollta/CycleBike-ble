package com.cyclebikeapp.ble.rest;

import com.squareup.okhttp.OkHttpClient;

import retrofit.RestAdapter;
import retrofit.client.OkClient;

/**
 * Created by TommyD on 12/10/2015.
 * using version 1.9.0 of Retrofit has all the functionality we need
 * reference https://futurestud.io/blog/retrofit-how-to-upload-files
 */
public class FileUploadServiceGenerator {
    private static final String STRAVA_API_BASE_URL = "https://www.strava.com/api/v3";

    private static final RestAdapter.Builder builder = new RestAdapter.Builder()
            .setEndpoint(STRAVA_API_BASE_URL)
            .setClient(new OkClient(new OkHttpClient()));

    public static <S> S createService(Class<S> serviceClass) {
        RestAdapter adapter = builder.build();
        return adapter.create(serviceClass);
    }
}
