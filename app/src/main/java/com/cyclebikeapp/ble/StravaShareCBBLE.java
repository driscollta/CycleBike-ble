package com.cyclebikeapp.ble;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.cyclebikeapp.ble.rest.ActivityFileParams;
import com.cyclebikeapp.ble.rest.FileUploadService;
import com.cyclebikeapp.ble.rest.FileUploadServiceGenerator;
import com.cyclebikeapp.ble.rest.StravaFailureResponse;
import com.cyclebikeapp.ble.rest.StravaSuccessResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;

import static com.cyclebikeapp.ble.Constants.APP_NAME;
import static com.cyclebikeapp.ble.Constants.NOTIFICATION_CHANNEL_ID;
import static com.cyclebikeapp.ble.Constants.PREFS_NAME;


public class StravaShareCBBLE extends AppCompatActivity {
	private static final String UPLOAD_FILENAME = "upload_filename";
	private static final String MY_STATE_CODE = "mOTa3QLA3RDEhwcX";
	private static final String MY_STATE_EQUALS = "state=" + MY_STATE_CODE;
	private static final String OAUTH_ACCESS_TOKEN_URL = "https://www.strava.com/oauth/token";
	private static final String CLIENT_ID = "15150";
	private static final String CALLBACK_URL = "http://localhost";
	private static final String AUTH_NO_NETWORK_INTENT_RC = "88";
	private static final String ACCESS_TOKEN = "AccessToken";
	private static final String LOGIN = "login";
	private static final String AUTHORIZE = "authorize";
	private static final String REJECT_APPLICATION = "reject_application";
	private static final String ERROR_ACCESS_DENIED = "error=access_denied";
	private static final String CODE = "code=";
	private static final int EIGHT_HOURS = 8 * 60 * 60;
    private static final String BLURB = "Activity recorded with CycleBike Smart\nGet it @ www.cyclebikeapp.com";
    private static final String ACTIVITY_TITLE = "CycleBikeSmart Activity";

	private WebView web;
	private String uploadFilename;
	private Dialog auth_dialog;
	private Intent resultIntent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Log.i(APP_NAME + " - StravaShare", "StravaShare");
		super.onCreate(savedInstanceState);
		resultIntent = getIntent();
		uploadFilename = (resultIntent.getStringExtra(UPLOAD_FILENAME));
		setResult(Activity.RESULT_CANCELED, resultIntent.putExtra(AUTH_NO_NETWORK_INTENT_RC, 0));
		if (uploadFilename == null) {
			Toast.makeText(getApplicationContext(), "No file specified", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		setupActionBar();
		Log.i(APP_NAME, "filename: "+uploadFilename);
		// uploadFilename = "" means just log-in to Strava
		if (!hasStravaAccessToken() || uploadFilename.equals("")) {
			auth_dialog = new Dialog(this);
			new AuthorizeBackground().execute();
		} else if (!uploadFilename.equals("")) {
			// uploadFilename.equals("") would be just to authorize Strava
			int notificationNumber = makeUploadNotification(getString(R.string.file_to_strava),
					getString(R.string.file_to_strava),
					getString(R.string.waiting_to_upload));
			doUploadFileWait(new ActivityFileParams(uploadFilename, getAccessTokenString(), notificationNumber));
			finish();
		}
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	private void setupActionBar() {
		android.support.v7.app.ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.hide();
		}
	}
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Log.i(APP_NAME, "StravaShare onActivityResult() requestCode: " + requestCode);
		}

	/**
	 * Called from the background AsyncTask after we know if there is a network connection
	 * Since we've erased cookies when authorization is approved to reset Auth log-in,
	 * the web.loadUrl() actually returns the log-in url, which we intercept in the WebViewClient onPageFinished()
	 */
    private void doStravaAuth() {

		// set back-navigation thru web pages and allow user to cancel authorization with back button
		auth_dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				//Log.i(APP_NAME, "onDismiss()");
				// ref: http://stackoverflow.com/questions/16941930/back-navigation-in-a-webview-android-app
				// But wasn't able to get onBackPressed() to work. It works in onDismiss().
				int cBFL = web.copyBackForwardList().getCurrentIndex();
				// We started with about:blank, so cBFL = 1 would be about:blank, which we don't want to show
				if (cBFL > 1) {
					// onDismiss() somehow dismisses the dialog, so we have to show it again
					auth_dialog.show();
					web.goBack();
				} else {
					Toast.makeText(getApplicationContext(),
							R.string.auth_cancelled, Toast.LENGTH_SHORT).show();
					auth_dialog.dismiss();
					finish();
				}
			}
		});
		auth_dialog.setContentView(R.layout.auth_dialog);
		auth_dialog.getWindow().setTitle("");
		auth_dialog.show();

		web = auth_dialog.findViewById(R.id.webv);
		web.loadUrl("about:blank");
		web.clearCache(true);
		web.setWebViewClient(new WebViewClient() {
			final SharedPreferences pref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			final SharedPreferences.Editor edit = pref.edit();
			String authCode;

			@Override
			public void onReceivedHttpAuthRequest(WebView view,
												  HttpAuthHandler handler,
												  String host, String realm) {
				//Log.v(APP_NAME, " - onReceivedHttpAuthRequest" + "host: " + host + " realm: " + realm);
			}

			@Override
			public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {
				//Log.v(APP_NAME, " - onReceivedLoginRequest" + "account: " + account + " realm: " + realm);
				// Notify the host application that a request to automatically
				// log in the user has been processed. But we never receive this;
				// it seems to be the key to getting Strava OAuth2.0 to work
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				//Log.v(APP_NAME, " - shouldOverrideUrlLoading" + "Url: " + url);
				if (url.contains(CODE) && url.contains(MY_STATE_EQUALS)) {
					authCode = Uri.parse(url).getQueryParameter("code");
					//Log.i(APP_NAME, "Authorization Code : " + authCode);
					edit.putString("AuthCode", authCode).apply();
					//exchange the authorization code for an accessToken
					new AccessTokenGet().execute();
					// erase cookies so log-in dialog will show next time
					restoreStravaLogin();
					return true;
				} else if ((url.contains(REJECT_APPLICATION)
						|| url.contains(ERROR_ACCESS_DENIED))
						&& url.contains(MY_STATE_EQUALS)) {
					// user cancelled authorization
					Toast.makeText(getApplicationContext(),
							R.string.auth_cancelled, Toast.LENGTH_SHORT)
							.show();
					auth_dialog.dismiss();
					finish();
					return true;
				}
				return false;
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				//Log.v(APP_NAME, "onPageStarted() - url: " + url);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				//Log.v(APP_NAME, "onPageFinished() - url: " + url);
				if (url.contains(AUTHORIZE)) {
					auth_dialog.setTitle(R.string.authorize);
				} else if (url.contains(LOGIN)) {
					auth_dialog.setTitle(R.string.login);
				} else if (url.contains("cyclebikeapp")) {
					auth_dialog.setTitle("CycleBikeApp");
				} else if (url.contains("strava")) {
					auth_dialog.setTitle("Strava");
				} else {
					auth_dialog.setTitle("");
				}
			}
		});
		auth_dialog.setTitle(R.string.loading_page);
		auth_dialog.show();
		web.loadUrl("https://www.strava.com/oauth/authorize?redirect_uri=http://localhost"
				+ "&response_type=code&client_id=" + CLIENT_ID
				+ "&scope=write&approval_prompt=force&state=" + MY_STATE_CODE);
	}

	/**
	 * The authorize process saves user login data in cookies. If we want to
	 * allow user to log-in with a different Strava account just remove all
	 * cookies. Could do this anyway since we'll always have a valid access
	 * token
	 */
    private void restoreStravaLogin() {
		CookieManager mgr = CookieManager.getInstance();
		if (mgr.hasCookies()) {
			mgr.removeAllCookie();
		}
	}

	/**
	 * Test for internet access, then either complain or proceed with authorization.
	 * The internet test may block, so put it in AsyncTask.
	 */
	private class AuthorizeBackground extends AsyncTask<String, String, Boolean>{
		@Override
		protected Boolean doInBackground(String... params) {
			return isOnline();
		}
		@Override
		protected void onPostExecute(Boolean hasNetwork) {
			if (!hasNetwork) {
				complainNoNetwork(AUTH_NO_NETWORK_INTENT_RC);
			} else {
				doStravaAuth();
			}
		}
	}

	/**
	 * Exchange authorization code for access token. On success, upload the file
	 */
	private class AccessTokenGet extends AsyncTask<String, String, JSONObject> {
		String authCode;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			authCode = getApplicationContext().getSharedPreferences(
					PREFS_NAME, MODE_PRIVATE).getString("AuthCode", "");
//			Log.v(APP_NAME, "StravaShare: " + "AccessTokenGet - preExecute");
//			Log.i(APP_NAME, "StravaShare: "+ "AccessTokenGet - authCode: " + authCode);
		}

		@Override
		protected JSONObject doInBackground(String... args) {
//			Log.v(APP_NAME, "StravaShare:  AccessTokenGet - doInBackground");
			GetAccessToken jParser = new GetAccessToken();
			return jParser.getToken(OAUTH_ACCESS_TOKEN_URL,
					authCode, CLIENT_ID, getString(R.string.client_shhhh), CALLBACK_URL);
		}

		@Override
		protected void onPostExecute(JSONObject json) {
//			Log.v(DisplayActivity.APP_NAME, "AccessTokenGet - onPostExecute");
			if (json != null) {
				try {
					String accessToken = json.getString("access_token");
//					Log.d(DisplayActivity.APP_NAME, "Token Access: " + accessToken);
					setAccessToken(accessToken);
					if (!("").equals(uploadFilename)) {
						int notificationNumber = makeUploadNotification(getString(R.string.file_to_strava),
								getString(R.string.file_to_strava),
								getString(R.string.processing));
						new UploadFileBackground().execute(new ActivityFileParams(uploadFilename, getAccessTokenString(), notificationNumber));
					} else {
						finish();
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
				Toast.makeText(getApplicationContext(), "Network Error",
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	 * Check whether we have to Authorize a users Strava account.
	 *
	 * @return true if the accessToken is not empty. This doesn't mean we are still authorized
	 * since the user could have revoked access
	 */
	private boolean hasStravaAccessToken() {
//		Log.i(APP_NAME, "has token? " + (!("").equals(getAccessTokenString()) ? "true" : "false"));
		return !("").equals(getAccessTokenString());
	}

	/**
	 * Ping the google DNS server to see if we have an internet connection.
	 * Used before requesting log-in and authorization to Strava.
	 * ref: http://stackoverflow.com/questions/1560788/how-to-check-internet-access-on-android-inetaddress-never-timeouts
	 *
	 * @return true if we have an internet connection
	 */
	private boolean isOnline() {

		Runtime runtime = Runtime.getRuntime();
		try {
			Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
			int exitValue = ipProcess.waitFor();
			//Log.w(APP_NAME, "isOnline()  -exitValue: " + exitValue);
			return (exitValue == 0);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * We detected that we're not online, post an Alert dialog to have the user change Settings.
	 *
	 * @param requestCode tells onActivityResult where to go next, in case we use this Alert elsewhere
	 */
	private void complainNoNetwork(String requestCode) {
		//Log.i(APP_NAME, "complainNoNetwork()");
		String noNetworkComplainTitle = "No Data Network";
		String noNetworkComplainText = "Authorization needs Internet access";
		// show alert dialog with cancel, enable buttons
		dealWithDialog(noNetworkComplainText, noNetworkComplainTitle, requestCode);
	}

	private void dealWithDialog(String message, String title, final String requestCode) {
		setResult(Activity.RESULT_OK, resultIntent.putExtra(requestCode, 1));
		android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
		//  if okay button pressed, show wifi or cell settings menu
		builder.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						//check again if the user is online: they may have gone to settings after Alert is posted
						if (!isOnline()) {
							Intent networkIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
							startActivity(networkIntent);
							finish();
						} else {
							doStravaAuth();
						}
					}
				});
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						//user refused to establish data connection, cancel the StravaShare process
						setResult(Activity.RESULT_CANCELED, resultIntent.putExtra(requestCode, 0));
						finish();
					}
				});
		builder.setCancelable(true);
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				setResult(Activity.RESULT_CANCELED, resultIntent.putExtra(requestCode, 0));
				dialog.dismiss();
				finish();
			}
		});
		// Set other dialog properties
		builder.setMessage(message).setTitle(title).show();
	}

	/**
	 * Wait for Internet to come on-line before uploading the file.
	 * This avoids NETWORK errors that make the user have to do the share task over again.
	 * pass thru the ActivityFileParams to UploadFileBackground AsyncTask, which is called in onPostExecute
	 * update the Notification to "Processing..." once we have network, or TIMEOUT if waited > 8 hours
	 * Only have to do this when we've by-passed the Authorization. We ensure network before Authorization
	 */

	private void doUploadFileWait(final ActivityFileParams params) {
		new Thread() {
			@Override
			public void run() {
				final int timeoutSec = EIGHT_HOURS;// up to 8 hours it will try to upload even if we're offline
				int waitedSec = 0;
				boolean timedOut = false;
				try {
					while (!isOnline() && !timedOut) {
						sleep(2000);
						waitedSec += 2;
						timedOut = (waitedSec > timeoutSec);
					}
					sleep(2000);
					// repeat this after two seconds until we get a second ping
					// might prevent NETWORK_ERROR
					while (!isOnline() && !timedOut) {
						sleep(2000);
						waitedSec += 2;
						timedOut = (waitedSec > timeoutSec);
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					String tickerText = getString(R.string.file_to_strava);
					String titleText = getString(R.string.file_to_strava);
					String contentText = getString(R.string.processing);
					if (timedOut){
						tickerText = getString(R.string.upload_failed);
						titleText = getString(R.string.upload_failed);
						contentText = "TIMEOUT - no network";
					}
					updateUploadNotification(tickerText, titleText, contentText, params.notificationNumber);
					if (!timedOut) {
						new UploadFileBackground().execute(params);
					}
				}
			}
		}.start();
	}

	/**
	 * Wrapper to upload file as an AsyncTask
	 * after composing ActivityFileParams with filename, accessToken, and notification number
	 */
    private class UploadFileBackground extends AsyncTask<ActivityFileParams, Void, Void> {

		@Override
		protected Void doInBackground(ActivityFileParams... params) {
			uploadFile(params[0]);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
		}
	}

	private void updateUploadNotification(String tickerText, String titleText, String updateContent, int notificationNumber) {
		NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Bitmap b = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ble_notification_icon);
        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)				.setSmallIcon(R.drawable.ic_upload)
				.setLargeIcon(b)
				.setContentTitle(titleText)
				.setContentText(updateContent)
                .setOngoing(false)
                .setTicker(tickerText);
        if (mNotifyMgr != null) {
            mNotifyMgr.notify(notificationNumber, mNotifyBuilder.build());
        }
    }

	private int makeUploadNotification(String tickerText, String titleText, String contentText) {
//		Log.i(APP_NAME, "makeUploadNotification()");
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "CBble Notifications", NotificationManager.IMPORTANCE_DEFAULT);
            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setShowBadge(false);
            if (mNotifyMgr != null) {
                mNotifyMgr.createNotificationChannel(notificationChannel);
            }
        }
		Bitmap b = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ble_notification_icon);
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_upload)
                .setLargeIcon(b)
                .setContentTitle(titleText)
                .setContentText(contentText)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setTicker(tickerText)
                .setOngoing(true)
                .setShowWhen(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        //return a random integer for notification number
		int mNotificationId = getNotificationNumber();
        if (mNotifyMgr != null) {
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }
        return mNotificationId;
	}

	/**
	 * create a new Retrofit FileUploadService and upload the specified file
	 * @param params are the ActivityFileParams required by the Strava upload API
	 */
	private void uploadFile(final ActivityFileParams params) {

		FileUploadService service = FileUploadServiceGenerator.createService(FileUploadService.class);
		// call the Retrofit service and analyze the callback JSON to deal with success or failure
		service.upload(params.authorization, params.typedFile, params.dataType, ACTIVITY_TITLE, BLURB, new Callback<Response>() {
					@Override
					public void success(Response result, Response response) {
						//Try to get response body
						TypedByteArray typ = (TypedByteArray) result.getBody();
						StravaSuccessResponse sResp = new StravaSuccessResponse(new String(typ.getBytes()));
/*
						Log.v(APP_NAME, "Upload" + "sResp + success: id: " + sResp.id
								+ " filename: " + sResp.filename
								+ " error: " + sResp.error
								+ " status: " + sResp.status
								+ " activity_id: " + sResp.activityID
								+ "notificationNumber: " + params.notificationNumber);
						// test StravaResponse for errors and alert user, etc
						Log.v(APP_NAME, "Upload" + "success: response reason " + response.getReason());
*/
						updateUploadNotification(getString(R.string.upload_complete),
								getString(R.string.upload_complete), getString(R.string.activity_added),
								params.notificationNumber);
					}

					@Override
					public void failure(RetrofitError error) {

						Response resp = error.getResponse();
						if (resp != null) {
							// duplicate file returns "Bad Request"
							//Log.v(APP_NAME, "failure reason " + resp.getReason());
							TypedByteArray typ = (TypedByteArray) resp.getBody();
							if (typ != null) {
//								Log.v(APP_NAME, "Upload failure: " + new String(typ.getBytes()));
								StravaFailureResponse stravaResponse = new StravaFailureResponse(new String(typ.getBytes()));
/*
								Log.v(APP_NAME, "Upload" + "failure: message: " + stravaResponse.message
										+ " resource: " + stravaResponse.error_resource
										+ " field: " + stravaResponse.error_field
										+ " code: " + stravaResponse.error_code);
*/
								updateUploadNotification(getString(R.string.upload_failed) + stravaResponse.error_code,
										"Upload to Strava failed", getString(R.string.upload_failed) + stravaResponse.error_code,
										params.notificationNumber);
								if (stravaResponse.error_code.contains("access_token invalid")
										|| "Authorization Error".equals(stravaResponse.message)){
									setAccessToken("");
								}
							}
							// wrong accessToken returns message: "Authorization Error"
							// with resource: "Athelete", field: "access_token", code: "invalid"
						}
						String errorKind = error.getKind().name();
						if (errorKind.toUpperCase().contains(("NETWORK"))) {
							updateUploadNotification(getString(R.string.upload_failed) + errorKind + " error",
									"Upload to Strava failed", getString(R.string.upload_failed) + errorKind + " error",
									params.notificationNumber);
						}
						// wrong file path returns failure kind: "NETWORK" - we won't have a file path error
						// airplane mode also returns failure kind: "NETWORK" - we'll test for a network first
//						Log.v(APP_NAME, "failure kind " + error.getKind().name());
						// duplicate file returns
						// {"id":499265057,"external_id":"11_29_2015-8_39_20_AM_CB_history.fit",
						// "error":"11_29_2015-8_39_20_AM_CB_history.fit duplicate of activity 448326251",
						// "status":"There was an error processing your activity.","activity_id":null}
					}
				}
		);
		finish();
	}


	/**
	 * @return a random number for the Notification number; it will be saved in ActivityFileParams for the upload
	 */
	private int getNotificationNumber() {
		Random r = new Random();
		return r.nextInt(Integer.MAX_VALUE);
	}

	/**
	 * save the accessToken safely away in SharedPrefs; we also delete the accessToken when we want to log-in to another account
	 *
	 * @param accessToken result of authorization code exchange
	 */
    private void setAccessToken(String accessToken) {
		//Log.i(APP_NAME, "saveAccessToken: token = " + accessToken);
		SharedPreferences pref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		SharedPreferences.Editor edit = pref.edit();
		edit.putString(ACCESS_TOKEN, accessToken).apply();
	}

	/**
	 * Returns the Strava access token saved in SharedPreferences
	 *
	 * @return accessToken as a String
	 */
	private String getAccessTokenString() {
		SharedPreferences pref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		return pref.getString(ACCESS_TOKEN, "");
	}

}
