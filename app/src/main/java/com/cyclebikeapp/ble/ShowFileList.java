package com.cyclebikeapp.ble;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.cyclebikeapp.ble.Constants.ACTIVITY_FILE_TYPE;
import static com.cyclebikeapp.ble.Constants.CHOOSER_TYPE;
import static com.cyclebikeapp.ble.Constants.CHOOSER_TYPE_GPX_DIRECTORY;
import static com.cyclebikeapp.ble.Constants.CHOOSER_TYPE_GPX_FILE;
import static com.cyclebikeapp.ble.Constants.CHOOSER_TYPE_TCX_DIRECTORY;
import static com.cyclebikeapp.ble.Constants.CHOOSER_TYPE_TCX_FILE;
import static com.cyclebikeapp.ble.Constants.FIT;
import static com.cyclebikeapp.ble.Constants.FIT_ACTIVITY_TYPE;
import static com.cyclebikeapp.ble.Constants.FORMAT_4_1F;
import static com.cyclebikeapp.ble.Constants.GPX;
import static com.cyclebikeapp.ble.Constants.KEY_CHOOSER_CODE;
import static com.cyclebikeapp.ble.Constants.KEY_CHOSEN_GPXFILE;
import static com.cyclebikeapp.ble.Constants.KEY_CHOSEN_TCXFILE;
import static com.cyclebikeapp.ble.Constants.KEY_GPXPATH;
import static com.cyclebikeapp.ble.Constants.PREFS_NAME;
import static com.cyclebikeapp.ble.Constants.ROUTE_FILE_TYPE;
import static com.cyclebikeapp.ble.Constants.TCX;
import static com.cyclebikeapp.ble.Constants.TCX_ACTIVITY_TYPE;
import static com.cyclebikeapp.ble.Constants.TCX_LOG_FILE_NAME;
import static com.cyclebikeapp.ble.Constants.XML;

/*
 * Copyright 2013 cyclebikeapp. All Rights Reserved.
 * @author Tom
 *
 */
public class ShowFileList extends AppCompatActivity {

	private static final String IC_XML_FILE = "6";// xml file icon
	private static final String IC_FIT_FILE = "4";//ic_file_fit
	private static final String IC_TCX_FILE = "3";//tcx file icon
	private static final String IC_FOLDER_UP = "2";// ic_folder_up
	private static final String IC_FILE = "1";//ic_file
	private static final String IC_FOLDER = "0";//ic_folder
	private static final CharSequence FTYPE_GPX = GPX;
 	private static final CharSequence FTYPE_TCX = TCX;
	private static final CharSequence FTYPE_XML = XML;
	private static final CharSequence FTYPE_FIT = FIT;
	public static final String KEY_FILENAME = "filename";
	public static final String KEY_THUMB = "icon_type";
	public static final String KEY_FILESIZE = "filesize";
    private static final String CB_HISTORY = "_CB_history";
    private File gpxPath = new File(Environment.getExternalStorageDirectory().toString());
	private final File activityFilePath = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
			+ Constants.ACTIVITY_FILE_PATH);
	private List<File> fileFileList = new ArrayList<>();

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_file_list_view);
		setupActionBar();
		final Intent intent = getIntent();
		final int chooserType = intent.getIntExtra(CHOOSER_TYPE, 0);
		ArrayList<HashMap<String, String>> filesList = new ArrayList<>();
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		gpxPath = new File(settings.getString(KEY_GPXPATH, gpxPath.toString()));
		// in case user presses back button
		setResult(Activity.RESULT_CANCELED);
        try {
            if (!activityFilePath.exists()) {
                activityFilePath.mkdirs();
            }
            if (!gpxPath.exists()) {
                gpxPath.mkdirs();
            }
        } catch (SecurityException ignore) {
        }
		if (chooserType == ROUTE_FILE_TYPE) {
			handleGPXFileList(filesList, settings);
		} else {
			handleTCX_FITFileList(filesList, settings);			
		}
        ListView list = findViewById(R.id.list);

		// Getting adapter by passing files data ArrayList
        ChooserAdapter adapter = new ChooserAdapter(this, filesList);
		list.setAdapter(adapter);
		list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// do something with the list item clicked; save in shared
				// preferences editor; also save the code for what was clicked:
				// file, or directory. We'll use this code when we resume() CycleBike Main activity
				// to either call ShowFileList if directory again, or load a file. 
				// If user hits back button, code is zero and resume() will do nothing
				File clickedFile;
				switch (position) {
					case 0:
						// selected the top directory element, which navigates up in
						// the directory tree
						if (chooserType == ROUTE_FILE_TYPE) {
							clickedFile = new File(gpxPath.getParent());
						} else {
							clickedFile = new File(activityFilePath.getParent());
						}
						// if no parent to top level folder, or folder is
						// ExternalStorageDirectory, don't return parent
						if ((clickedFile.toString().equals(Environment
								.getExternalStorageDirectory().getParent()))) {
							clickedFile = new File(Environment
									.getExternalStorageDirectory().toString());
						}
						break;
					default:
						clickedFile = fileFileList.get(position - 1);
				}
				int chooserCode = 0;
				switch (chooserType) {
					case ROUTE_FILE_TYPE:
						chooserCode = handleGPXChoice(clickedFile);
						break;
					case ACTIVITY_FILE_TYPE:
						chooserCode = handleActivityFileChoice(clickedFile);
						break;
				}
                if (chooserCode != CHOOSER_TYPE_TCX_DIRECTORY) {
                    setResult(Activity.RESULT_OK, intent.putExtra(KEY_CHOOSER_CODE, chooserCode));
                    finish();
                }
			}//onItemClick

			private int handleActivityFileChoice(File clickedFile) {
				int chooserCode;
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				SharedPreferences.Editor editor = settings.edit();
				if (clickedFile.isDirectory()) {
					chooserCode = CHOOSER_TYPE_TCX_DIRECTORY;
					//ignore user clicking on a directory; only want to display activity files in the apps files directory
				} else {
					chooserCode = CHOOSER_TYPE_TCX_FILE;
					editor.putString(KEY_CHOSEN_TCXFILE, clickedFile.toString());
				}
				editor.apply();
				return chooserCode;
			}//TCXChoice

			private int handleGPXChoice(File clickedFile) {
				int chooserCode;
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				SharedPreferences.Editor editor = settings.edit();
				if (clickedFile.isDirectory()) {
					chooserCode = CHOOSER_TYPE_GPX_DIRECTORY;
					editor.putString(KEY_GPXPATH, clickedFile.toString());
				} else {
					chooserCode = CHOOSER_TYPE_GPX_FILE;
					editor.putString(KEY_GPXPATH, gpxPath.getPath());
					editor.putString(KEY_CHOSEN_GPXFILE, clickedFile.toString());
				}
				editor.apply();
				return chooserCode;
			}//GPXChoice
		});//OnItemClickListener
	}// onCreate
	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	private void setupActionBar() {

		android.support.v7.app.ActionBar actionBar = getSupportActionBar();

	}
	private void handleTCX_FITFileList(
			ArrayList<HashMap<String, String>> filesList,
			SharedPreferences settings) {
		android.support.v7.app.ActionBar ab = getSupportActionBar();
		if (ab != null) ab.setTitle(R.string.cyclebike_choose_track_file);
        mLoadActivityFileList();
		// go thru the fileList and convert to HashMap
		HashMap<String, String> mapA = new HashMap<>();
		String firstFilePath = activityFilePath.toString();
		String iconLevel = IC_FOLDER;// folder icon
		String displayName = delPathPrefix(firstFilePath);
		mapA.put(KEY_FILENAME, displayName);
		// set filesize, for directory set to ""
		mapA.put(KEY_THUMB, iconLevel);
		filesList.add(mapA);

		if (fileFileList != null) {
			for (int i = 0; i < fileFileList.size(); i++) {
				// creating new HashMap
				HashMap<String, String> map = new HashMap<>();
				String imageLevel = (fileFileList.get(i).isDirectory()) ? IC_FOLDER : IC_FILE;
				String fileNameStr = fileFileList.get(i).getName();
				if (fileNameStr.endsWith(TCX)){
					map.put(KEY_FILENAME, delSuffix(delSuffix(fileNameStr, TCX), CB_HISTORY));
					imageLevel = IC_TCX_FILE;
				} else if (fileNameStr.endsWith(FIT)){
					map.put(KEY_FILENAME, delSuffix(delSuffix(fileNameStr, FIT), CB_HISTORY));
					imageLevel = IC_FIT_FILE;
				}
				//set file size
				map.put(KEY_FILESIZE, getFileSize(fileFileList.get(i), settings));
				// set folder or file icon
				map.put(KEY_THUMB, imageLevel);
				// adding HashList to ArrayList
				filesList.add(map);
			}
		}
	}//handleTCXFileList()

	/**
	 * Get the size of a file in the activity file list
	 * @param file the file in question
	 * @param settings SharedPreferences
	 * @return size of the file, '' if directory, "new" if the current log file, which may not have been saved
	 */
	@SuppressLint("DefaultLocale")
	private String getFileSize(File file, SharedPreferences settings) {
		String fileSize;
		if (file.isDirectory()){
			fileSize = "";
		} else if (fileIsoutFileName(file, settings)){
			fileSize = "new";
		} else {
			double fileLength = file.length()/1000.;
			String suffix = " kB";
			if (fileLength > 950) {
				fileLength = fileLength / 1000.;
				suffix = " MB";
			}
				fileSize = "" + String.format(FORMAT_4_1F, fileLength) + suffix;
		}
        if (Utilities.isScreenWidthSmall(getApplicationContext())){
            fileSize = "";
		}
		return fileSize;
	}

	/**
	 * Determine if the file is the current log file. Read Shared Prefs for log file, strip path and suffix before comparing names
	 * @param file the file in question
	 * @param settings SharedPreferences
	 * @return true if the file in question is the current log file
	 */
	private boolean fileIsoutFileName(File file, SharedPreferences settings) {
		String logFileName = settings.getString(TCX_LOG_FILE_NAME, "");
		String bareLogFileName = delSuffix(logFileName, TCX);
		return file.getName().contains(bareLogFileName);
	}

	private void handleGPXFileList(
			ArrayList<HashMap<String, String>> filesList, SharedPreferences settings) {
		android.support.v7.app.ActionBar ab = getSupportActionBar();
		if (ab != null) ab.setTitle(R.string.cyclebike_load_route_file);
		String s = settings.getString(KEY_GPXPATH, "");
		if (!s.equals(""))
			gpxPath = new File(s);
		else
			gpxPath = new File(Environment.getExternalStorageDirectory().toString());
			mLoadGPXFileList();
		// go thru the fileList and convert to HashMap
		HashMap<String, String> mapA = new HashMap<>();
		String firstFilePath = gpxPath.toString();
		String iconLevel = IC_FOLDER;// folder icon
		if (!firstFilePath.equals(Environment.getExternalStorageDirectory().toString())) {
			iconLevel = IC_FOLDER_UP;// folder icon with up arrow
		}
		String displayName = delPathPrefix(firstFilePath);
		mapA.put(KEY_FILENAME, displayName);
		// set filesize, for directory set to ""
		// 	mapA.put(KEY_FILESIZE,"");
		mapA.put(KEY_THUMB, iconLevel);
		filesList.add(mapA);

		if (fileFileList != null) {
			for (int i = 0; i < fileFileList.size(); i++) {
				// creating new HashMap
				HashMap<String, String> map = new HashMap<>();
				String imageLevel = (fileFileList.get(i).isDirectory()) ? IC_FOLDER : IC_FILE;
				String fileNameStr = fileFileList.get(i).getName();
				if (fileNameStr.endsWith(GPX)){
					map.put(KEY_FILENAME, delSuffix(fileNameStr, GPX));
					imageLevel = IC_FILE;
				} else if (fileNameStr.endsWith(TCX)){
					map.put(KEY_FILENAME, delSuffix(fileNameStr, TCX));										
					imageLevel = IC_TCX_FILE;
				} else if (fileNameStr.endsWith(XML)){
					map.put(KEY_FILENAME, delSuffix(fileNameStr, XML));										
					imageLevel = IC_XML_FILE;
				} else {
					map.put(KEY_FILENAME, fileNameStr);
				}
				// set folder or file icon
				map.put(KEY_THUMB, imageLevel);
				// set filesize, for route files set to ""
				map.put(KEY_FILESIZE,"");
				// adding HashList to ArrayList
				filesList.add(map);
			}
		}
	}//handleGPXFileList()

	@Override
	protected void onStop() {
		super.onStop();
		saveState();
		finish();
	}

	private void saveState() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(KEY_GPXPATH, gpxPath.getPath());
		editor.apply();
	}
	

	private void mLoadActivityFileList() {
		try {
            if (!activityFilePath.exists()) {
                activityFilePath.mkdirs();
            }
		} catch (SecurityException ignore) {
		}

		if (activityFilePath.exists() && activityFilePath.canRead() && activityFilePath.isDirectory()) {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					File sel = new File(dir, filename);
					return filename.contains(getActivityFileType())
							|| sel.isDirectory();
				}
			};
			fileFileList = Arrays.asList(activityFilePath.listFiles(filter));
			Collections.sort(fileFileList, new DirTimeComparator());
		}
	}// loadActivityFileList()

	private CharSequence getActivityFileType() {
		CharSequence fType = "";
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

		String activityFileType = sharedPref.getString(getResources().getString(R.string.pref_activity_file_key), TCX_ACTIVITY_TYPE);
		if (activityFileType.equals(TCX_ACTIVITY_TYPE)){
			fType = FTYPE_TCX;
		} else if (activityFileType.equals(FIT_ACTIVITY_TYPE)){
			fType = FTYPE_FIT;
		}
		return fType;
	}

	private void mLoadGPXFileList() {
		try {
            if (!gpxPath.exists()) {
                gpxPath.mkdirs();
            }
		} catch (SecurityException ignore) {
		}
		if (gpxPath.exists() && gpxPath.canRead() && gpxPath.isDirectory()) {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					File sel = new File(dir, filename);
					return filename.contains(FTYPE_GPX)
							|| filename.contains(FTYPE_TCX)
							|| filename.contains(FTYPE_XML)
							|| sel.isDirectory();
				}
			};
			fileFileList = Arrays.asList(gpxPath.listFiles(filter));
			Collections.sort(fileFileList, new DirAzComparator());
		}
	}// loadGPXFileList()

	private String delPathPrefix(String dirName) {
		// delete the path prefix from directory names
		String pathPrefix = Environment.getExternalStorageDirectory().toString();
		int start = dirName.lastIndexOf(pathPrefix) + pathPrefix.length();
		int end = dirName.length();
		if ((end - start) <= 0) {
			dirName = "";
		} else {
			dirName = dirName.substring(start, end);
		}
		return dirName;
	}

	private String delSuffix(String fileName, String suffix) {
		// delete the specified suffix on the displayed filenames
		int end = fileName.indexOf(suffix, 0);
		if (end > 0) {
			fileName = fileName.substring(0, end);
		}
		return fileName;
	}
	private class DirAzComparator implements Comparator<File> {
		@Override
		public int compare(File f1, File f2) {
			int f1Dir = f1.isDirectory() ? -1 : 0;
			int f2Dir = f2.isDirectory() ? -1 : 0;
			//compare a directory to a file
			if ((f1Dir - f2Dir) != 0) {
				return (f1Dir - f2Dir);
			}
			// compare a directory to a directory or file to file, sort a-z
			return (f1.getName().toUpperCase(Locale.getDefault()).compareTo(f2
					.getName().toUpperCase(Locale.getDefault())));
		}
	}

	/** sort alphabetically, then by last modified time */
    private class DirTimeComparator implements Comparator<File> {
		@Override
		public int compare(File f1, File f2) {
			int f1Dir = f1.isDirectory() ? -1 : 0;
			int f2Dir = f2.isDirectory() ? -1 : 0;
//compare a directory to a file
			if ((f1Dir - f2Dir) != 0) {
				return (f1Dir - f2Dir);
			}
// compare a directory to a directory, sort a-z
			if (f1.isDirectory() && f2.isDirectory()){
			return (f1.getName().toUpperCase(Locale.getDefault()).compareTo(f2
					.getName().toUpperCase(Locale.getDefault())));
			}
// compare a file to a file sort new-old
			return (int) (f2.lastModified() - f1.lastModified());
		}
	}

}
