package com.cyclebikeapp.ble;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

@SuppressLint("AppCompatCustomView")
public class CustomTextView extends TextView{

	    public CustomTextView(Context context, AttributeSet attrs, int defStyle) {
	        super(context, attrs, defStyle);
	    }

	    public CustomTextView(Context context, AttributeSet attrs) {
	        super(context, attrs);
	    }

	    public CustomTextView(Context context) {
	        super(context);
	    }

}
