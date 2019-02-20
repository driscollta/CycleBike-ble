package com.cyclebikeapp.ble;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class GetAccessToken {
    private static InputStream is = null;
    private static JSONObject jObj = null;
    private static String json = "";
    public GetAccessToken() {
    }
    private final List<NameValuePair> params = new ArrayList<>();
    Map<String, String> mapn;
    private DefaultHttpClient httpClient;
    private HttpPost httpPost;
 
    public JSONObject getToken(String address,
    		String authCode,
    		String client_id,
    		String client_secret,
    		String redirect_uri) {
        // Making HTTP request
        try {
            // DefaultHttpClient
            httpClient = new DefaultHttpClient();
            httpPost = new HttpPost(address);
            params.add(new BasicNameValuePair("code", authCode));
            params.add(new BasicNameValuePair("client_id", client_id));
            params.add(new BasicNameValuePair("client_secret", client_secret));
            params.add(new BasicNameValuePair("redirect_uri", redirect_uri));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            is = httpEntity.getContent(); 
        } catch (UnsupportedEncodingException e) {
        	Log.e("CycleBike", "GetAccessToken: " + "UnsupportedEncodingException");
            e.printStackTrace();
        } catch (ClientProtocolException e) {
        	Log.e("CycleBike", "GetAccessToken: " + "ClientProtocolException");
            e.printStackTrace();
        } catch (IOException e) {
        	Log.e("CycleBike", "GetAccessToken: " + "IOException");
            e.printStackTrace();
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "n");
            }
            is.close();
            json = sb.toString();
            //Log.v("CycleBike",  "JSONStr: " + json);
        } catch (Exception e) {
            e.getMessage();
            //Log.e("CycleBike", " Buffer Error: " + "Error converting result " + e.toString());
        }
        // Parse the String to a JSON Object
        try {
            jObj = new JSONObject(json);
        } catch (JSONException e) {
            //Log.e("CycleBike", "JSON Parser: " + "Error parsing data " + e.toString());
        }
        // Return JSON String
        return jObj;
    }
 
}
