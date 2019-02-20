package com.cyclebikeapp.ble;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

class ExpListAdapter extends BaseExpandableListAdapter {
	
	private final ArrayList<String> groupList;
	private ArrayList<ArrayList<String>> childList = new ArrayList<>();
	private LayoutInflater mInflater;

    private class ViewHolder {
		ImageView icon;
		TextView title;
	}
	
	ExpListAdapter(ArrayList<String> groupList, ArrayList<ArrayList<String>> childList) {
		this.groupList = groupList;
		this.childList = childList;
	}

	void setInflater(LayoutInflater mInflater, Activity act) {
		this.mInflater = mInflater;
    }

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return null;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return 0;
	}

	@SuppressLint("InflateParams")
	@Override
    //TODO change these icons
	public View getChildView(int groupPosition, final int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		final int[] icons = { R.drawable.ant_hr_icon48_gr,
		R.drawable.ant_pwr_icon48_gr,
		R.drawable.ant_spd_icon48_gr,
		R.drawable.ant_cad_icon48_gr,
		R.drawable.ant_spdcad_icon48_gr};
		String[] items = childList.get(groupPosition).toArray(new String[childList.get(groupPosition).size()]);
        ViewHolder holder;
        if (convertView == null) {
			convertView = mInflater.inflate(R.layout.childrow, null);
			holder = new ViewHolder();
			holder.icon = (ImageView) convertView.findViewById(R.id.childImage);
			holder.title = (TextView) convertView.findViewById(R.id.childText);
			convertView.setTag(holder);
		} else {
			// view already defined, retrieve view holder
			holder = (ViewHolder) convertView.getTag();
		}
//		Log.i(this.getClass().getName(), "getChildView #items - " + items.length);
		holder.title.setText(items[childPosition]);
		holder.icon.setImageResource(icons[groupPosition]);
		convertView.setClickable(false);
		return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return childList.get(groupPosition).size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return null;
	}

	@Override
	public int getGroupCount() {
		return groupList.size();
	}

    @Override
	public long getGroupId(int groupPosition) {
		return 0;
	}

	@SuppressLint("InflateParams")
	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.grouprow, null);
		}
		((CheckedTextView) convertView).setText(groupList.get(groupPosition));
		((CheckedTextView) convertView).setChecked(isExpanded);
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
	    return true;
	}
}
