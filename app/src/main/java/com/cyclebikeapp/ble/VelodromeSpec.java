package com.cyclebikeapp.ble;

import android.location.Location;

class VelodromeSpec {
	/** semi-minor axis in meters */
private double r;
/** semi-major axis in meters */
private double R;
private Location centerLocation;
/** rotation of the major axis from east-west in degrees; positive towards North */
private double tilt;
private String name;
public String latString;
public String lonString;
/** descriptive text about the Velodrome that may be displayed on the UI */
private String comment;


double getMajorAxis() {
	return R;
}
void setMajorAxis(double r) {
	this.R = r;
}
double getMinorAxis() {
	return r;
}
void setMinorAxis(double r) {
	this.r = r;
}
Location getCenterLocation() {
	return centerLocation;
}
void setCenterLocation(Location centerLocation) {
	this.centerLocation = centerLocation;
}
double getTilt() {
	return tilt;
}
void setTilt(double tilt) {
	this.tilt = tilt;
}
String getVelodromeName() {
	return name;
}
void setVelodromeName(String name) {
	this.name = name;
}
public String getComment() {
	return comment;
}
public void setComment(String comment) {
	this.comment = comment;
}
}
