package com.cyclebikeapp.ble;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
/*
 * Copyright  2013 cyclebikeapp. All Rights Reserved.
*/

/**
 * just pop-up the XML layout that has a scrollable list of text; the text sections 
 * are defined in a series of strings. The only other thing is to change the custom title
 */

public class AboutScroller extends AppCompatActivity {

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about_scroller);
		setupActionBar();
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	private void setupActionBar() {

		android.support.v7.app.ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			// Show the Up button in the action bar.
			actionBar.setTitle(R.string.about_cyclebike_);
			actionBar.show();
		}
	}

}
