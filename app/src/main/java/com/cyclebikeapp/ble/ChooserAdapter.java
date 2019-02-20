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
 * Copyright  2013 cyclebikeapp. All Rights Reserved.
*/

class ChooserAdapter extends BaseAdapter {
	private final ArrayList<HashMap<String, String>> data;
	private static LayoutInflater inflater = null;

	ChooserAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
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
		if (convertView == null) {
            vi = inflater.inflate(R.layout.chooser_list_row, parent, false);
        }
		TextView title = vi.findViewById(R.id.title); // title
		TextView size = vi.findViewById(R.id.filesize); // filesize
		ImageView thumb_image = vi.findViewById(R.id.list_image);
		int viewWidth = parent.getWidth();
        HashMap<String, String> fileItem = data.get(position);
        int imageLevel = Integer.valueOf(fileItem.get(ShowFileList.KEY_THUMB));
		if (viewWidth < Constants.SMALL_SCREEN_WIDTH){
            if  (imageLevel == 2 || imageLevel == 0){
                thumb_image.setVisibility(View.VISIBLE);
            } else {
                thumb_image.setVisibility(View.GONE);
            }
        } else {
            thumb_image.setVisibility(View.VISIBLE);
        }
		// Setting all values in listview
		size.setText(fileItem.get(ShowFileList.KEY_FILESIZE));
		title.setText(fileItem.get(ShowFileList.KEY_FILENAME));
		thumb_image.setImageLevel(imageLevel);
		return vi;
	}
}
