package com.cyclebikeapp.ble;


import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
/*
 * Copyright 2013 cyclebikeapp. All Rights Reserved.
*/

class TMAdapter extends BaseAdapter {
	private final ArrayList<HashMap<String, String>> data;
	private static LayoutInflater inflater = null;

	TMAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
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
			vi = inflater.inflate(R.layout.trainer_mode_list_row, parent, false);

		TextView title = vi.findViewById(R.id.title); // title
		ImageView thumb_image = vi.findViewById(R.id.list_image);
		HashMap<String, String> fileItem;
		fileItem = data.get(position);

		// Setting all values in listview
		title.setText(fileItem.get(TrainerModeSettings.KEY_FILENAME));
		int imageLevel = Integer.valueOf(fileItem.get(TrainerModeSettings.KEY_THUMB));
		thumb_image.setImageLevel(imageLevel);
		return vi;
	}
}
