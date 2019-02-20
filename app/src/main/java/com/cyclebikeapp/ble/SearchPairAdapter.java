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

class SearchPairAdapter extends BaseAdapter {
	private final ArrayList<HashMap<String, String>> data;
	private static LayoutInflater inflater = null;

	SearchPairAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
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
			vi = inflater.inflate(R.layout.search_pair_list_row, parent, false);

		TextView title = vi.findViewById(R.id.title); // title
		ImageView thumb_image = vi.findViewById(R.id.list_image);
		HashMap<String, String> fileItem;
        try {
            fileItem = data.get(position);
            // Setting all values in listview
            title.setText(fileItem.get(Constants.KEY_SP_DEVICE_NAME));
            int imageLevel = Integer.valueOf(fileItem.get(Constants.KEY_SP_INDB_ICON));
            thumb_image.setImageLevel(imageLevel);
        } catch (Exception ignore){

        }
		return vi;
	}
}
