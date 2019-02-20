package com.cyclebikeapp.ble;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import static com.cyclebikeapp.ble.Constants.KEY_BEARING;
import static com.cyclebikeapp.ble.Constants.KEY_DIM;
import static com.cyclebikeapp.ble.Constants.KEY_DISTANCE;
import static com.cyclebikeapp.ble.Constants.KEY_STREET;
import static com.cyclebikeapp.ble.Constants.KEY_TURN;
import static com.cyclebikeapp.ble.Constants.KEY_UNIT;
/*
 * Copyright 2013 cyclebikeapp. All Rights Reserved.
*/

class TurnByTurnListAdapter extends BaseAdapter {
	private static final String TRACKPOINT = "TRACKPOINT";
//displays the ListView for the turn-by-turn directions

	private final ArrayList<HashMap<String, String>> data;
	private static LayoutInflater inflater = null;

	TurnByTurnListAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
		data = d;
		inflater = (LayoutInflater) a
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public int getCount() {
		return data.size();
	}

	public Object getItem(int position) {
		return position;
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View vi = convertView;
		if (convertView == null)
			vi = inflater.inflate(R.layout.turn_list_row, null);

		TextView street = vi.findViewById(R.id.street_name); // title
		TextView distance = vi.findViewById(R.id.distance);
		TextView distanceUnit = vi.findViewById(R.id.distance_unit);
		ImageView turnIcon = vi.findViewById(R.id.turn_icon);
		ImageView bearingIcon = vi.findViewById(R.id.arrow_icon);
		HashMap<String, String> fileItem = new HashMap<>();
		fileItem = data.get(position);
		// Setting all values in listview
		String streetText = fileItem.get(KEY_STREET);
		street.setText(streetText);
		street.setTypeface(null, Typeface.NORMAL);
		if (!streetText.toUpperCase().contains(TRACKPOINT)) {
			street.setTypeface(null, Typeface.BOLD);			
		}
		//color value for street, distance, etc text_dim, white, gpsgreen
		int dimLevel = Integer.valueOf(fileItem.get(KEY_DIM));
		street.setTextColor(dimLevel);
		distanceUnit.setText(fileItem.get(KEY_UNIT));
		distanceUnit.setTextColor(dimLevel);
		distance.setText(fileItem.get(KEY_DISTANCE));
		distance.setTextColor(dimLevel);
		int imageLevel = Integer.valueOf(fileItem.get(KEY_TURN));
		turnIcon.setImageLevel(imageLevel);
		imageLevel = Integer.valueOf(fileItem.get(KEY_BEARING));
		bearingIcon.setImageLevel(imageLevel);
		return vi;
	}
}
