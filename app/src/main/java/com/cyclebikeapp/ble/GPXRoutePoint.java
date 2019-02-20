package com.cyclebikeapp.ble;

import java.io.Serializable;

class GPXRoutePoint implements Serializable {
	private static final long serialVersionUID = 1L;
	double lat = 0.;
	double lon = 0.;
	double elevation = 0.;
	String name = "";
	String comment = "";
	String desc = "";
	public String type = "";
	String sym = "";
	/** 
	 * kind = 1 means this is a RoutePoint
	 * kind = 100 means this is a TrackPoint
	 * kind = 99 means a cluster of TrackPoints
	 * kind = 98 means a cluster of TrackPoints that should be preserved
	 */
	public int kind;
	/** number of TrackPoints within a certain distance */
	public int clusterSize;
	/** mark to delete this point when merging arrays*/
	boolean delete = false;

	private boolean beenThere = false;//use this field to signal if we've been to this WayPoint
	/** index to pointing arrows for the display showing relative direction to the WayPoint from the DOT */
    final int relBearIconIndex = 0;
	/** distance from previous WayPoint in a Route to the WayPoint (miles) straight-path travel */
	double distFromPrevWP = 0;
	private int WPNum = 0;
	/** name of the turn to make at this waypoint; parsed from xml file in findRoutePointTurnName()
	 *  and abbreviated to the first letter eg. "L", "R", "S"*/
	private String turnDir = "";
	/** index to icons L = 2, SL = 3, S = 4, R = 5, etc*/
	int turnIconIndex = 0;
	/** Street name to turn onto; String displayed.
	picks up everything after "on" or "onto" as a keyword */
	private String streetName = "";
	/**when WayPoint is in a NavRoute this field is the accumulated distance WayPoint-to-WayPoint
	this is used to help decide if the current position is at this WayPoint based on tripDistance
	field in BikeStat*/
	private double routeMiles = 0;

	GPXRoutePoint() {

	}

	public int getBearFromHereIndex(){	
		/* bearing to the WP from current location relative to true North */
		double bearFromHere = 0;
		return (int) Math.floor((bearFromHere / 45) + 0.5);
	}
	public int getRelBearFromHereIndex(){	
		/* bearing to the WP from current location relative to the direction of travel */
		double relBearFromHere = 0;
		return (int) Math.floor((relBearFromHere / 45) + 0.5);
	}

	boolean isBeenThere() {
		return beenThere;
	}
	void setBeenThere(boolean beenThere) {
		this.beenThere = beenThere;
	}
	public int getWPNum() {
		return WPNum;
	}
	void setWPNum(int wPNum) {
		WPNum = wPNum;
	}
	public String getTurnDir() {
		return turnDir;
	}
	void setTurnDir(String turnDir) {
		this.turnDir = turnDir;
	}
	String getStreetName() {
		return streetName;
	}
	void setStreetName(String streetName) {
		this.streetName = streetName;
	}
	double getRouteMiles() {
		return routeMiles;
	}
	void setRouteMiles(double routeMiles) {
		this.routeMiles = routeMiles;
	}

}
