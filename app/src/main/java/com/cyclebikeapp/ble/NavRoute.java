package com.cyclebikeapp.ble;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/*
 * Copyright 2013, 2014 cyclebikeapp. All Rights Reserved.
 */
class NavRoute {
    private static final String SLIGHT_LEFT_IT = "LEGGERMENTE A SINISTRA";
	private static final String SLIGHT_LEFT_FR = "LEGGERMENTE A GAUCHE";
	private static final String SLIGHT_LEFT_ES = "LIGERAMENTE A LA IZQUIERDA";
	private static final String SLIGHT_LEFT_DE = "LEICHT LINKS";
	private static final String SLIGHT_RIGHT_IT = "LEGGERMENTE A DESTRA";
	private static final String SLIGHT_RIGHT_FR = "LEGGERMENTE A DROITE";
	private static final String SLIGHT_RIGHT_ES = "LIGERAMENTE A LA DERECHA";
	private static final String SLIGHT_RIGHT_DE = "LEICHT RECHTS";
	private static final String TOWARD_IT = " VERSO ";
	private static final String TOWARD_FR = " VERS ";
	private static final String ONTO_IT = " IMBOCCA ";
	private static final String ONTO_FR = " SUR ";
	private static final String ONTO_ES = " HACIA ";
	private static final String ONTO_DE = " AUF ";
	private static final String STAY_ON = "STAY ON";
	private static final String X = "X";
	private static final String WATER = "WATER";
	private static final String FOOD = "FOOD";
	private static final String SUMMIT = "SUMMIT";
	private static final String U_TURN = "U";
	private static final String CONTINUE = "C";
	private static final String STRAIGHT = "S";
	private static final String RIGHT_ = "R";
	private static final String LEFT_ = "L";
	private static final String SLIGHT_RIGHT = "SR";
	private static final String SLIGHT_LEFT = "SL";
	private static final String GENERIC = "GENERIC";
	private static final String PARSER_CONFIG = " ParserConfig";
	private static final String IO_EXCEPTION = " IOException";
	/** types of GPXRoutePoints in the merged array */
	private static final int trkPtKind = 100;
	private static final int routePtKind = 1;
	private static final int clusterTrkPtKind = 99;
	private static final int importantClusterTrkPtKind = 98;
	private static final String TRACK_POINT = "TrackPoint ";
    private static final String SHORT_TRACK_POINT = "TPt ";
    private static final String TPC = "TPC ";
	private static final int _360 = 360;
	private static final int maxTurnNameChars = 48;
	private static final int clusterThreshold = 3;
	//how close to be from trackpoint to insert a waypoint
	private static final double wpClose2TP = 15.;
	private static final double CLUSTER_PROXIMITY = wpClose2TP / 4;
	//how far to get from WP before setting it at top of list (in meters)
	private static final double movedAwayEnough = 11.26541;
	// how close to get to WP before reached it (in meters)
	private static final double closeEnough = 76.2024384;
	// tripDistMargin is used in arrived-at way point algorithm; looseness in
	// arrived-at definition (in meters)
	private static final double tripDistMargin = 1593.25;
	// eliminate trackpoints that are too close (meters), depending on density preference
	// use most trackpoints, but have to eliminate duplicates especially around turns
	private static final double trkptHighDensity = 45.72;
	// use more trackpoints
	private static final double trkptMediumDensity = 500.;
	// use some trackpoints
	private static final double trkptLowDensity = 5000.;
	// use no trackpoints
	private static final double trkptZeroDensity = 9999.;
    final MySAXHandler handler = new MySAXHandler();
	// combined RoutePoint - TrackPoint list of GPXRoutePoints read in by SAX handler
	ArrayList<GPXRoutePoint> mergedRoute;
	// list of RoutePoint - TrackPoints thinned by TrackPoint density to match the current routeHashMap
    final ArrayList<GPXRoutePoint> mergedRoute_HashMap;
	private double DOT;//direction of travel to set relBearIconIndex; use this instead of BikeStat for simulation
	private double prevDOT;
	private double deltaDOT;//change in DOT used for paused condition
	//index to the first WayPoint to be displayed at the top of the list
	int firstListElem = 0;
	// RouteMiles at firstListElem
	double routeMilesatFirstListElem;
	//index to first element in the list that has WayPoint.beenThere flag set to true
	int currWP = 0;
	// gpx route file
	File mChosenFile;
	private boolean proximity = false;
	//bonus miles accounts for the longer road distance compared to the
	//way point to way point direct distance
	private double bonusMiles = 0;
	private final Location there = new Location(LocationManager.GPS_PROVIDER);
	private final Location here = new Location(LocationManager.GPS_PROVIDER);
	private boolean closeToWP;
	private String error = "";
	boolean trackClosed = false;
	float accurateDOT;
	int defaultTrackDensity;

	NavRoute(Context context) {
		mergedRoute = new ArrayList<>();
		mergedRoute_HashMap = new ArrayList<>();
	}

	void loadNavRoute(Context applicationContext) {
		//todo use ThreadExecutorPool to put this off the UI thread
		// display FileChooser to select .gpx file, open scanner, read lines,
		// parse file, compose WayPoint, call getTurnDirIndex(),
		// call getBearFromHereIndex(), call CalcRouteMilesandBearings(), add to
		// ArrayList<> route
		setError("");
		BufferedReader r = null;
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			XMLReader xr = parser.getXMLReader();
			xr.setContentHandler(handler);
			xr.setErrorHandler(handler);
			r = new BufferedReader(new FileReader(mChosenFile));
			// uses the entire path, not just filename
			xr.parse(new InputSource(r));
		} catch (SAXException e) {
			e.printStackTrace();
			setError(applicationContext.getString(R.string.invalid_file_) + applicationContext.getString(R.string._saxexception));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			setError(applicationContext.getString(R.string.file_not_found) + " " + mChosenFile.toString());
		} catch (IOException e) {
			e.printStackTrace();
			setError(applicationContext.getString(R.string.invalid_file_) + IO_EXCEPTION);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			setError(applicationContext.getString(R.string.invalid_file_) + PARSER_CONFIG);
		} catch (NumberFormatException e) {
			setError(applicationContext.getString(R.string.invalid_file_)
					+ applicationContext.getString(R.string._this_file_has_lat_long_errors));
		} finally {
			if (r != null) {
				try {
					r.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} // finally
	}

	/**
	 * Given an ArrayList of GPXRoutePoints containing TrackPoints and merged RoutePoints.
	 * Step thru each GPXRoutePoint and fill in values for each WayPoint in the WayPoint ArrayList "route".
     * @param applicationContext used to get strings
     */
	private void initializeAllRouteWayPoints(Context applicationContext) {
		//called from prepareRoute
		if (getError().equals("")) {
			// step through the merged array and fill-in waypoint fields for route;
			GPXRoutePoint tempGPXRP;
			for (int index = 0; index < mergedRoute.size(); index++) {
				tempGPXRP = mergedRoute.get(index);
				tempGPXRP.setBeenThere(false);
				tempGPXRP.setWPNum(index);
				// set Way Point turn direction name
				tempGPXRP.setTurnDir(mergedRoute.get(index).name);
				// convert the turn name into an icon index
				tempGPXRP.turnIconIndex = getRoutePtTurnDirIndex(mergedRoute.get(index).name, applicationContext);
				String streetName = setWPStreetName(index, applicationContext);
				tempGPXRP.setStreetName(streetName);
				mergedRoute.set(index, tempGPXRP);
			} // for mergedArray
			// now that we're done with handler, clear the ArrayList
			handler.handlersGPXRoute.clear();
			handler.handlersTrackPtRoute.clear();
		}
	}

	/**
	 * Find a name to display in the turn-by-turn scrolling list. If a
	 * TrackPoint or TrackPoint cluster, just use a generic name followed by the
	 * number in the list. If a RoutePoint, find keywords in the .comment, .desc
	 * or .name fields. Extract substring after keyword, or just use the whole thing
	 */
	private String setWPStreetName(int i, Context applicationContext) {
		final String keyWords[] = { STAY_ON, applicationContext.getString(R.string._onto_),
				ONTO_DE, ONTO_FR, ONTO_ES, ONTO_IT,
                applicationContext.getString(R.string._toward_), TOWARD_FR, TOWARD_IT, " AT ", " ON " };
		String turnName;
		if (mergedRoute.get(i).kind == importantClusterTrkPtKind) {//set trkptCluster "street name" to "TCP"
			turnName = TPC + i;
		} else if (mergedRoute.get(i).kind == trkPtKind) {//set "street name" to "TrackPoint" or "TPt"
			turnName = TRACK_POINT + i;
			if (Utilities.isScreenWidthSmall(applicationContext)){
			    turnName = SHORT_TRACK_POINT + i;
            }
		} else {// RoutePoint should have a street name in one of the fields
			String commentString = (mergedRoute.get(i).comment.trim());
			// if comment string blank, look in desc string, then name, then
			// copy turn direction into street name field as default
			if (commentString.equals("")) {
				commentString = mergedRoute.get(i).desc.trim();
			}
			if (commentString.equals("")) {
				commentString = mergedRoute.get(i).name;
			}
			// find key words in comment string to extract street name
			turnName = commentString;
			for (int j = 0; j < keyWords.length; j++) {
				int beginIndex;
				String string = keyWords[j], subString;
				if (commentString.toUpperCase(Locale.US).contains(string)) {
					beginIndex = commentString.toUpperCase(Locale.US).lastIndexOf(string) + string.length() - 1;
					if (j == 0){// this keyword is "stay on" and we want to keep that instruction
						beginIndex = commentString.toUpperCase(Locale.US).lastIndexOf(string);
					}
					if (beginIndex == string.length() - 1) {
						// entire string is just the direction
						beginIndex = 0;// so use it all
					}
					subString = commentString.substring(beginIndex);
					// shorten the string so it fits within a few lines on the display
					if (subString.length() < 2) {
						turnName = commentString;
					} else if (subString.length() < maxTurnNameChars) {
						turnName = subString;
					} else {
						turnName = subString.substring(0, maxTurnNameChars-1) + " ...";
					}
					break;// found key word, break out of for loop
				}
			}// find Route Point street name
		} // set street name field for Track Points or Route Points
		return turnName;
	}

	/**
	 * After loading RoutePoint and TrackPoint data from file, prepare data
	 * before constructing Waypoint .route.
	 * 0) if there are no RoutePoints,
	 * detect clusters of TrackPoints as possible turn location preserve one of
	 * these as kind = importantClusterTrkPtKind. Don't ever delete kind = importantClusterTrkPtKind
	 * 1) merge RoutePoints with TrackPoints (if any); take care of "orphans"
	 *  that may not merge in the mergeByProximity() method.
	 * 1a) detect clusters of TrackPoints that don't have a nearby RoutePoint.
	 *  If the street name doesn't change, or RWGPS didn't capture that RoutePoint
	 *  we want to save this as an important turn.
	 * 2) thin TrackPoints to within 150' around RoutePoints
	 *  (RWGPS and others cluster TrackPoints around turns)
	 * 3) Delete these thinned TrackPoints
	 * 4) Thin & delete Trackpoints between
	 *  RoutePoints to within 150' of each other (clustered TrackPoints could
	 *  affect step 6)
	 * 5) Thin TrackPoints according to DefaultSharedPreference pref_trackpoint_density_key
	 * 6) Determine the Turn Name for all "un-thinned" TrackPoints using relative bearing
	 *  between them; Don't find Turn Name for TrackPoints we will later delete. We don't
	 *  want to delete TrackPoints before finding bearing; if TrackPoints are too distant, the
	 *  immediate bearing may be erroneous The TurnName is something like Left, Right, etc that
	 *  will be used to display a turn icon in the turn-by-turn list
	 * 7) Delete all previously "thinned" TrackPoints
	 * 8) Find the turn name for RoutePoints
     * @param applicationContext used to retrieve DefaultSharedPrefs and strings
     */
	void prepareRoute(Context applicationContext) {
		mergedRoute = new ArrayList<>();
		boolean noRoutePoints = (handler.handlersGPXRoute.size() < 1);
		setError("");
		// if there are TrackPts, start merged ArrayList with those
		if (handler.handlersTrackPtRoute.size() > 0){
			mergedRoute.addAll(handler.handlersTrackPtRoute);
		}
		mergeGPX_TrackRoute();
		detectTPClusters();
		// delete TrackPoints within 150' of RoutePoints or each-other. They
		// interfere with the isCloseToWP() and isProximate() methods; also
		// interfere with finding TrackPoint turn direction.
		thinTrkPtsNearRoutePt(trkptHighDensity);
		deleteThinnedPoints();
		thinTrkPts(trkptHighDensity);
		deleteThinnedPoints();
		findTrkPtTurnName(applicationContext);
		findRoutePtTurnName(applicationContext);
		calcMergedRouteMiles();
		initializeAllRouteWayPoints(applicationContext);
		// use preference to decide how many track points to include
		SharedPreferences defaultSettings = PreferenceManager.getDefaultSharedPreferences(applicationContext);
		int defaultTrackDensity = Integer.valueOf(defaultSettings.getString(applicationContext.getResources()
						.getString(R.string.pref_trackpoint_density_key), "0"));
		// If there are no RoutePoints and Preference is "no track points" (density = 0)
		// change this to trkptLowDensity
		if (noRoutePoints && (defaultTrackDensity == 0)) {
			defaultTrackDensity = 1;
		}
		//already thinned to trkptHighDensity (= 3)
		if (defaultTrackDensity != 3) {
			changeTrkPtDensity(defaultTrackDensity);
		}
	}

	/**
	 * prepareRoute() thins the TrackPoints to trkptHighDensity. Depending on
	 * user preference, want to thin track points to some other level by setting
	 * the .delete tag based on proximity. This is called either from
	 * prepareRoute() or onResume where trackPointDensity has been changed.
	 */
	void changeTrkPtDensity(int trackDensity) {
		double[] tpTooCloseDistance = { trkptZeroDensity, trkptLowDensity, trkptMediumDensity, trkptHighDensity };
		double trkPtCloseDistance = tpTooCloseDistance[trackDensity];
		if (trackDensity == 3 || trackDensity == 0) {
			boolean toDelete = true;
			// we've already thinned to high density just go thru merged and set
			// all .delete to false
			// for zeroDensity, just set all .delete to true
			if (trackDensity == 3) {
				// for high density, don't have to delete anything
				toDelete = false;
			}
			deleteAllOrNone(toDelete);
		} else {
			thinToTrkPtDensity(trkPtCloseDistance);
		}
	}

	/** for setting TrackPoint density to none (.delete = true) or high (.delete = false),
	 *  or to prepare mergedArray for setting low or moderate TrackPoint density.
	 *  Only set TrackPoint kind or clusterTrackPoints  */
	private void deleteAllOrNone(boolean toDelete) {
		int index = 0;
		for (GPXRoutePoint tempRP : mergedRoute) {
			if (tempRP.kind > importantClusterTrkPtKind) {
				tempRP.delete = toDelete;
				mergedRoute.set(index, tempRP);
			}
			index++;
		}
	}

	private void thinToTrkPtDensity(double trkPtCloseDistance) {
		// change number of track points from high density to medium or low density
		// first thin around RoutePoints, then for each TrackPoint that is still left thin
		// from here to the TrackPoint trkPtCloseDistance away.
		// first make sure all .delete = false
		deleteAllOrNone(false);
		GPXRoutePoint theTempPoint;
		// step thru merged array stopping at each RoutePoint
		// if distance from this RoutePoint to the previous TrackPoints
		// is less than trkPtCloseDistance, mark that TrackPoint for deletion
		// next check distance to later TrackPoints and mark for deletion if trkPtCloseDistance
		for (int index = 0; index < mergedRoute.size() - 1; index++) {
			// is it a RoutePoint? or an importantClusterTrackPoint?
			if (mergedRoute.get(index).kind < clusterTrkPtKind) {
				double distance;
				if (index != 0 ) {// can't check before the first route point
					// check all points before the RoutePoint until one is too far away
					for (int j = index - 1; j > -1; j--) {
						theTempPoint = mergedRoute.get(j);
						distance = mergedRoute.get(index).getRouteMiles() - theTempPoint.getRouteMiles();
						// TrackPoint too close?
						if ((theTempPoint.kind > importantClusterTrkPtKind) && (distance < trkPtCloseDistance)) {
							// just mark for deletion; we'll delete at the right time in the thinning sequence
							theTempPoint.delete = true;
							mergedRoute.set(j, theTempPoint);
						} else {
							// either come to a RoutePoint, importantTrackPoint, or a TrackPoint too far away
							break;
						}
					}//test TrackPoints before
				}
				// check all points after the RoutePoint until one is too far away
				for (int j = index + 1; j < mergedRoute.size(); j++) {
					theTempPoint = mergedRoute.get(j);
					distance = theTempPoint.getRouteMiles() - mergedRoute.get(index).getRouteMiles();
					// TrackPoint too close?
					if ((theTempPoint.kind > importantClusterTrkPtKind) && (distance < trkPtCloseDistance)) {
						theTempPoint.delete = true;
						mergedRoute.set(j, theTempPoint);
					} else {
						// either come to a RoutePoint, importantTrackPoint, or a TrackPoint too far away
						break;
					}
				}// test TrackPoints after
			}// test this RoutePoint
		}// step through the mergedRoute looking for RoutePoints.
		GPXRoutePoint theSecondPoint;
		// now step thru the merged array testing if TrackPoints are too close
		// to each other. We've already tested RoutePoints and know there aren't any
		// TrackPoints too close
		for (int index = 0; index < mergedRoute.size() - 2; index++) {
			// Is it an undeleted TrackPoint or a clusterTrackPoint
			if ((mergedRoute.get(index).kind > importantClusterTrkPtKind)
					&& !mergedRoute.get(index).delete) {
				for (int j = index + 1; j < mergedRoute.size(); j++) {
					theSecondPoint = mergedRoute.get(j);
					// Is it a TrackPoint or a clusterTrackPoint
					if (theSecondPoint.kind > importantClusterTrkPtKind) {
						// TrackPoint too close?
						if ((theSecondPoint.getRouteMiles()
								- mergedRoute.get(index).getRouteMiles()) < trkPtCloseDistance) {
							// just mark it for deletion
							theSecondPoint.delete = true;
							mergedRoute.set(j, theSecondPoint);
						} else {
							theSecondPoint.delete = false;
							mergedRoute.set(j, theSecondPoint);
							index = j - 1;
							// found something too far away, continue with next TrackPoint
							// after this one
							break;
						}
					} else {
						// found a RoutePoint, continue checking after this next RoutePoint
						index = j - 1;
						break;
					}
				}// check all points after the TrackPoint until one is too far away
			}// test against this TrackPoint
		}// step thru the merged Array
	}

	/**
	 * A cluster of TrackPoints may indicate a turn. Want to preserve one these
	 * key TrackPoints if there are no RoutePoints. For each TrackPoint, measure
	 * the number of other TrackPoints within a distance of wpClose2TP (15 m).
	 * If numInCluster > clusterSizeDef, mark that TrackPoint as kind = clusterTrkPtKind. Save
	 * the median TrackPoint in each cluster. Next identify "important" TrackPoint clusters
	 * for which there are no nearby RoutePoints. Set kind = importantClusterTrkPtKind
	 *  and treat these like RoutePoints.
	 */
	private void detectTPClusters() {
		// also detect clusters without RoutePoints nearby -> "important" clusterTP
		// we don't want to delete these
		GPXRoutePoint tempRP;
		for (int i = 0; i < mergedRoute.size(); i++) {
			tempRP = mergedRoute.get(i);
			if (mergedRoute.get(i).kind == trkPtKind) {
				// find clusterSize measures how many TPs are in proximity
				if (findClusterSize(i) > clusterThreshold) {
					tempRP.kind = clusterTrkPtKind;
					mergedRoute.set(i, tempRP);
				}
			}// TrackPoints
		}// cluster detection
		// now detect "important" clusters w/o a nearby RoutePoint
		for (int i = 0; i < mergedRoute.size(); i++) {
			tempRP = mergedRoute.get(i);
			if (tempRP.kind == clusterTrkPtKind) {
				if (detectNearbyWayPoint(i)) {
				// not an importantCluster, so mark this as a cluster trackPoint
					tempRP.kind = clusterTrkPtKind;
					mergedRoute.set(i, tempRP);
				} else {
					tempRP.kind = importantClusterTrkPtKind;
					mergedRoute.set(i, tempRP);
				}
			}// examine clusterTrkPtKind
		}//detecting important clusters
		// merge all importantClusterTPs into one
		for (int start = 0; start < mergedRoute.size(); start++) {
			if (mergedRoute.get(start).kind == importantClusterTrkPtKind) {
				//find the other end of this important Cluster
				int endIndex = start;
				for (int end = start; end < mergedRoute.size(); end++) {
					// mark important cluster TrkPts as trkpts unless it's the middle one
					if (mergedRoute.get(end).kind == importantClusterTrkPtKind) {
						tempRP = mergedRoute.get(end);
						tempRP.kind = trkPtKind;
						mergedRoute.set(end, tempRP);
					} else if (mergedRoute.get(end).kind == trkPtKind){//found the end of the cluster
						//mark the middle point of the cluster as an importantClusterTrkPt
						int midClusterIndex = start + ((end - start) / 2);
						tempRP = mergedRoute.get(midClusterIndex);
						tempRP.kind = importantClusterTrkPtKind;
						mergedRoute.set(midClusterIndex, tempRP);
						endIndex = end;
						break;
					}// notice that we just skip over any routePtKind in the middle of the cluster
				}// look for end of cluster
				start = endIndex;// next cluster is after end of the last one
			}//start of cluster
		}//merging cluster points into one
	}

	private boolean detectNearbyWayPoint(int i) {
		int index = i;
		while ((index < mergedRoute.size())
				&& (mergedRoute.get(index).kind < trkPtKind)) {
			// stops when we've found an ordinary trackpoint, marking the end of the cluster
			if (mergedRoute.get(index).kind == routePtKind) {
				return true;
			}
			index++;
		}
		index = i;
		// look backwards to see if a RoutePoint is in the middle-of or adjacent-to the cluster
		while ((index > 0)
				&& (mergedRoute.get(index).kind < trkPtKind)) {// look at RP, clusterTP kinds
			// stops when we've found an ordinary trackpoint, marking the end of the cluster
			if (mergedRoute.get(index).kind == routePtKind) {
				return true;
			}
			index--;
		}
		return false;
	}

	private int findClusterSize(int i) {
		if (i < 1 || i > mergedRoute.size() - 1) {
			return 0;
		}
		int clusterSize = 0;
		// examine TrackPoints after
		for (int clusterIndex = i + 1; clusterIndex < mergedRoute.size(); clusterIndex++) {
			if (distBetweenLatLonPoints(mergedRoute.get(i).lat, mergedRoute.get(i).lon,
					mergedRoute.get(clusterIndex).lat, mergedRoute.get(clusterIndex).lon) < CLUSTER_PROXIMITY) {
				clusterSize++;
			} else {//distance too large
				break;
			}//proximity detection
		}// examine TrackPoints after
		// examine TrackPoints before
		for (int clusterIndex = i - 1; clusterIndex > -1; clusterIndex--) {
			if (distBetweenLatLonPoints(mergedRoute.get(i).lat, mergedRoute.get(i).lon,
					mergedRoute.get(clusterIndex).lat, mergedRoute.get(clusterIndex).lon) < CLUSTER_PROXIMITY) {
				clusterSize++;
			} else {//distance too large
				break;
			}//proximity detection
		}// examine TrackPoints before
		return clusterSize;
	}

	/**
	 * Set the turn name, like "L", "R", "S" for TrackPoints using the relative
	 * bearing between TrackPoints ahead and behind each one. We've already
	 * thinned out the TrackPoints so there shouldn't be one within 150'
     * @param applicationContext used to get strings
     */
	private void findTrkPtTurnName(Context applicationContext) {
		// Calculate difference in relative bearing between TrackPoints
		final String[] turnNames = { LEFT_, SLIGHT_LEFT, STRAIGHT, SLIGHT_RIGHT, RIGHT_, X};
		double tempLat1, tempLon1;
		double tempLat2, tempLon2;
		double tempLat3, tempLon3;
		for (int i = 1; i < mergedRoute.size() - 2; i++) {
			GPXRoutePoint tempGPXRP;
			GPXRoutePoint tempBeforeGPXRP;
			GPXRoutePoint tempAfterGPXRP;
			tempGPXRP = mergedRoute.get(i);
			// don't find turnName for a RoutePoint
			if (((tempGPXRP.kind == trkPtKind) || tempGPXRP.kind == importantClusterTrkPtKind)) {
				String streetName = setWPStreetName(i, applicationContext);
				tempGPXRP.setStreetName(streetName);
				tempBeforeGPXRP = mergedRoute.get(i - 1);
				tempAfterGPXRP = mergedRoute.get(i + 1);
				tempLat1 = tempGPXRP.lat;
				tempLon1 = tempGPXRP.lon;
				tempLat2 = tempBeforeGPXRP.lat;
				tempLon2 = tempBeforeGPXRP.lon;
				tempLat3 = tempAfterGPXRP.lat;
				tempLon3 = tempAfterGPXRP.lon;

				here.setLatitude(tempLat1);
				here.setLongitude(tempLon1);
				there.setLatitude(tempLat2);
				there.setLongitude(tempLon2);
				float prevBearing = (here.bearingTo(there) + _360) % _360;
				there.setLatitude(tempLat3);
				there.setLongitude(tempLon3);
				float nextBearing = (here.bearingTo(there) + _360) % _360;
				int iconIndex = (int) Math.floor((((nextBearing - prevBearing + _360) % _360) / 45) + 0.5);
				// turn arrow behind us would show food or water icon
				if (iconIndex < 2) {
					iconIndex = 2;
				}
				if (iconIndex > 6) {
					iconIndex = 6;
				}
				tempGPXRP.name = turnNames[iconIndex - 2];
				tempGPXRP.turnIconIndex = getRoutePtTurnDirIndex(mergedRoute.get(i).name, applicationContext);
				//preserve left and right turn trackpoints by calling them importantClusterTrkPrKind
				// then when adjusting track pt density we won't delete the key turns
				if ((iconIndex == 2) || (iconIndex == 6)) {
					tempGPXRP.kind = importantClusterTrkPtKind;
				}
				mergedRoute.set(i, tempGPXRP);
			}
		}// for all TrackPoints
			// set turnName for first and last TrackPoint; could set this to the
			// "X" character
		GPXRoutePoint tempGPXRP0;
		tempGPXRP0 = mergedRoute.get(0);
		if ((tempGPXRP0.kind == trkPtKind) && (!tempGPXRP0.delete)) {
			tempGPXRP0.name = turnNames[5];
			mergedRoute.set(0, tempGPXRP0);
		}
		GPXRoutePoint tempGPXRP1;
		tempGPXRP1 = mergedRoute.get(mergedRoute.size() - 1);
		if ((tempGPXRP1.kind == trkPtKind) && (!tempGPXRP1.delete)) {
			tempGPXRP1.name = turnNames[5];
			mergedRoute.set(mergedRoute.size() - 1, tempGPXRP1);
		}
		GPXRoutePoint tempGPXRP2;
		tempGPXRP2 = mergedRoute.get(mergedRoute.size() - 2);
		if ((tempGPXRP2.kind == trkPtKind) && (!tempGPXRP2.delete)) {
			tempGPXRP2.name = turnNames[5];
			mergedRoute.set(mergedRoute.size() - 2, tempGPXRP2);
		}
	}

	/**
	 * Use information from parsing the GPXRoutePoint file to learn the turn
	 * direction such as left, slight-right, u-turn, etc This will be used to
	 * display a turn icon in the turn-by-turn list. The icon index is
	 * calculated in initializeRouteWayPoints(). The turn direction could be in
	 * various fields, but most commonly in the .name field. TrackPoint turn direction
	 * has been found by calculating bearings between locations
     * @param applicationContext
     */
	private void findRoutePtTurnName(Context applicationContext) {
		final String[] leftTurnName = { applicationContext.getString(R.string.left),
                applicationContext.getString(R.string.left_de), applicationContext.getString(R.string.left_es),
                applicationContext.getString(R.string.left_it), applicationContext.getString(R.string.left_fr) };
		final String[] rightTurnName = { applicationContext.getString(R.string.right),
                applicationContext.getString(R.string.right_de), applicationContext.getString(R.string.right_es),
                applicationContext.getString(R.string.right_it), applicationContext.getString(R.string.right_fr) };
		final String[] straightTurnName = { applicationContext.getString(R.string.straight),
                applicationContext.getString(R.string.straight_de), applicationContext.getString(R.string.straight_es),
                applicationContext.getString(R.string.straight_it), applicationContext.getString(R.string.straight_fr) };
		final String[] continueTurnName = { applicationContext.getString(R.string.continue_),
                applicationContext.getString(R.string.continue_de), applicationContext.getString(R.string.continue_es),
                applicationContext.getString(R.string.continue_it), applicationContext.getString(R.string.continue_fr) };
		final String[] slightRightTurnName = { "TSLR", SLIGHT_RIGHT_IT, SLIGHT_RIGHT_FR, SLIGHT_RIGHT_ES, SLIGHT_RIGHT_DE,
                applicationContext.getString(R.string.slight_right), applicationContext.getString(R.string.keep_right),
                applicationContext.getString(R.string.slight_right_de), applicationContext.getString(R.string.slight_right_es),
                applicationContext.getString(R.string.slight_right_it), applicationContext.getString(R.string.slight_right_fr),
                applicationContext.getString(R.string.right_slight),
                applicationContext.getString(R.string.right_slight_de), applicationContext.getString(R.string.right_slight_es),
                applicationContext.getString(R.string.right_slight_it), applicationContext.getString(R.string.right_slight_fr) };
		final String[] slightLeftTurnName = { "TSLL", SLIGHT_LEFT_IT, SLIGHT_LEFT_FR, SLIGHT_LEFT_ES, SLIGHT_LEFT_DE,
                applicationContext.getString(R.string.slight_left), applicationContext.getString(R.string.keep_left),
                applicationContext.getString(R.string.slight_left_de), applicationContext.getString(R.string.slight_left_es),
                applicationContext.getString(R.string.slight_left_it), applicationContext.getString(R.string.slight_left_fr),
                applicationContext.getString(R.string.left_slight),
                applicationContext.getString(R.string.left_slight_de), applicationContext.getString(R.string.left_slight_es),
                applicationContext.getString(R.string.left_slight_it), applicationContext.getString(R.string.left_slight_fr) };
		final String[] uTurnTurnName = { applicationContext.getString(R.string.uturn),
                applicationContext.getString(R.string.uturn_de), applicationContext.getString(R.string.uturn_es),
                applicationContext.getString(R.string.uturn_it), applicationContext.getString(R.string.uturn_fr),
                applicationContext.getString(R.string.u_turn),
                applicationContext.getString(R.string.u_turn_de), applicationContext.getString(R.string.u_turn_es),
                applicationContext.getString(R.string.u_turn_it), applicationContext.getString(R.string.u_turn_fr),
                applicationContext.getString(R.string.u_turn_),
                applicationContext.getString(R.string.u_turn_de_), applicationContext.getString(R.string.u_turn_es_),
                applicationContext.getString(R.string.u_turn_it_), applicationContext.getString(R.string.u_turn_fr_) };
		final String[] foodTurnName = { applicationContext.getString(R.string.food),
                applicationContext.getString(R.string.food_de), applicationContext.getString(R.string.food_es),
                applicationContext.getString(R.string.food_it), applicationContext.getString(R.string.food_fr) };
		final String[] waterTurnName = { applicationContext.getString(R.string.water),
                applicationContext.getString(R.string.water_de), applicationContext.getString(R.string.water_es),
                applicationContext.getString(R.string.water_it), applicationContext.getString(R.string.water_fr) };
		final String[] summitTurnName = { applicationContext.getString(R.string.summit)};
		final ArrayList<String[]> allPossibleTurnNames = new ArrayList<>();
		allPossibleTurnNames.add(slightRightTurnName);
		allPossibleTurnNames.add(slightLeftTurnName);
		allPossibleTurnNames.add(leftTurnName);
		allPossibleTurnNames.add(rightTurnName);
		allPossibleTurnNames.add(straightTurnName);
		allPossibleTurnNames.add(continueTurnName);
		allPossibleTurnNames.add(uTurnTurnName);
		allPossibleTurnNames.add(foodTurnName);
		allPossibleTurnNames.add(waterTurnName);
		allPossibleTurnNames.add(summitTurnName);

//these turn names are used with condition 'contains' and include a space at the end of the keyword
		final String[] leftTurnName_c = { applicationContext.getString(R.string.left_c),
                applicationContext.getString(R.string.left_de_c), applicationContext.getString(R.string.left_es_c),
                applicationContext.getString(R.string.left_it_c), applicationContext.getString(R.string.left_fr_c) };
		final String[] rightTurnName_c = { applicationContext.getString(R.string.right_c),
                applicationContext.getString(R.string.right_de_c), applicationContext.getString(R.string.right_es_c),
                applicationContext.getString(R.string.right_it_c), applicationContext.getString(R.string.right_fr_c) };
		final String[] straightTurnName_c = { applicationContext.getString(R.string.straight_c),
                applicationContext.getString(R.string.straight_de_c), applicationContext.getString(R.string.straight_es_c),
                applicationContext.getString(R.string.straight_it_c), applicationContext.getString(R.string.straight_fr_c) };
		final String[] continueTurnName_c = { applicationContext.getString(R.string.continue_c),
                applicationContext.getString(R.string.continue_de_c), applicationContext.getString(R.string.continue_es_c),
                applicationContext.getString(R.string.continue_it_c), applicationContext.getString(R.string.continue_fr_c) };
		final String[] uTurnTurnName_c = { applicationContext.getString(R.string.uturn__c),
                applicationContext.getString(R.string.uturn_de_c), applicationContext.getString(R.string.uturn_es_c),
                applicationContext.getString(R.string.uturn_it_c), applicationContext.getString(R.string.uturn_fr_c),
                applicationContext.getString(R.string.u_turn_c),
                applicationContext.getString(R.string.u_turn_de_c), applicationContext.getString(R.string.u_turn_es_c),
                applicationContext.getString(R.string.u_turn_it_c), applicationContext.getString(R.string.u_turn_fr_c),
                applicationContext.getString(R.string.u_turn__c),
                applicationContext.getString(R.string.u_turn_de__c), applicationContext.getString(R.string.u_turn_es__c),
                applicationContext.getString(R.string.u_turn_it__c), applicationContext.getString(R.string.u_turn_fr__c) };
		final String[] foodTurnName_c = { applicationContext.getString(R.string.food_c),
                applicationContext.getString(R.string.food_de_c), applicationContext.getString(R.string.food_es_c),
                applicationContext.getString(R.string.food_it_c), applicationContext.getString(R.string.food_fr_c) };
		final String[] waterTurnName_c = { applicationContext.getString(R.string.water_c),
                applicationContext.getString(R.string.water_de_c), applicationContext.getString(R.string.water_es_c),
                applicationContext.getString(R.string.water_it_c), applicationContext.getString(R.string.water_fr_c) };

		final ArrayList<String[]> allPossibleTurnNames_c = new ArrayList<>();
		allPossibleTurnNames_c.add(leftTurnName_c);
		allPossibleTurnNames_c.add(rightTurnName_c);
		allPossibleTurnNames_c.add(straightTurnName_c);
		allPossibleTurnNames_c.add(continueTurnName_c);
		allPossibleTurnNames_c.add(uTurnTurnName_c);
		allPossibleTurnNames_c.add(foodTurnName_c);
		allPossibleTurnNames_c.add(waterTurnName_c);

		final String[] rtPtTurnName = { SLIGHT_RIGHT, SLIGHT_LEFT, LEFT_, RIGHT_,
				STRAIGHT, CONTINUE, U_TURN, FOOD, WATER, SUMMIT };
		final String[] rtPtTurnName_c = { LEFT_, RIGHT_,
				STRAIGHT, CONTINUE, U_TURN, FOOD, WATER };
		for (int i = 0; i < mergedRoute.size(); i++) {
			//only look at RoutePoints
			GPXRoutePoint tempPoint = mergedRoute.get(i);
			String turnNameResult = "";
			if (tempPoint.kind == routePtKind) {
				// test the special case for start and finish, where .name = "Generic" (or .type, or .sym)
				if (GENERIC.equals(tempPoint.name.trim().toUpperCase(Locale.US))
						|| GENERIC.equals(tempPoint.type.trim().toUpperCase(Locale.US))
						|| GENERIC.equals(tempPoint.sym.trim().toUpperCase(Locale.US))) {
					turnNameResult = X;
				}
				// we assume the turn direction is in .name field. If this also has street name info, it would be erased
				// finding the turn info. If the comment field or description field is blank (or just spaces)
				// copy the .name field there to preserve other info in the .name field. If they are not empty hopefully there is street name info there
				if (("").equals(tempPoint.comment.trim())) {
					tempPoint.comment = tempPoint.name;
				}
				if (("").equals(tempPoint.desc.trim())) {
					tempPoint.desc = tempPoint.name;
				}
				boolean contains = false;
				if (("").equals(turnNameResult)) {
					turnNameResult = testRoutePtField(
							allPossibleTurnNames, rtPtTurnName,
							tempPoint.name, contains);
					if (("").equals(turnNameResult)) {
						turnNameResult = testRoutePtField(
								allPossibleTurnNames, rtPtTurnName,
								tempPoint.comment, contains);
						if (("").equals(turnNameResult)) {
							turnNameResult = testRoutePtField(
									allPossibleTurnNames, rtPtTurnName,
									tempPoint.desc, contains);
							if (("").equals(turnNameResult)) {
								turnNameResult = testRoutePtField(
										allPossibleTurnNames, rtPtTurnName,
										tempPoint.type, contains);
								if (("").equals(turnNameResult)) {
									turnNameResult = testRoutePtField(
											allPossibleTurnNames, rtPtTurnName,
											tempPoint.sym, contains);
								}// nothing in .type, tested .sym
							}// nothing in .desc, tested .type
						}// nothing in .comment
					}// nothing in .name
				} // not "GENERIC"
				// if no result test again for fields that contain keywords ending with a space character
				contains = true;
				if (("").equals(turnNameResult)) {
					turnNameResult = testRoutePtField(
							allPossibleTurnNames_c, rtPtTurnName_c,
							tempPoint.name, contains);
					if (("").equals(turnNameResult)) {
						turnNameResult = testRoutePtField(
								allPossibleTurnNames_c, rtPtTurnName_c,
								tempPoint.comment, contains);
						if (("").equals(turnNameResult)) {
							turnNameResult = testRoutePtField(
									allPossibleTurnNames_c, rtPtTurnName_c,
									tempPoint.desc, contains);
							if (("").equals(turnNameResult)) {
								turnNameResult = testRoutePtField(
										allPossibleTurnNames_c, rtPtTurnName_c,
										tempPoint.type, contains);
								if (("").equals(turnNameResult)) {
									turnNameResult = testRoutePtField(
											allPossibleTurnNames_c, rtPtTurnName_c,
											tempPoint.sym, contains);
								}// nothing in .type, tested .sym
							}// nothing in .desc, tested .type
						}// nothing in .comment
					}// nothing in .name
				} // not "GENERIC"
				// if turnNameResult still blank, use algorithm for track points to try to get a turn direction
				if (("").equals(turnNameResult)
						&& (i > 0)
						&& (i < (mergedRoute.size() - 1)) ) {
					turnNameResult = getTurnNameFromBearings(i, tempPoint);
				}
				// Handle some special cases
				// continue is the same icon as straight
				if (turnNameResult.equals(CONTINUE)) {
					turnNameResult = STRAIGHT;
				}
				// even tho' the .name field had "L", check if the comment field has Slight Left
				//(using testRoutePtField to detect Slight Left in comment field also checks
				// Slight Right first, but shouldn't match) sometimes "S" has slight in the .comment field
				contains = true;
				if (turnNameResult.equals(LEFT_)
						|| turnNameResult.equals(STRAIGHT)) {
					if (testRoutePtField(allPossibleTurnNames, rtPtTurnName, tempPoint.comment, contains).equals(SLIGHT_LEFT)
							|| testRoutePtField(allPossibleTurnNames, rtPtTurnName, tempPoint.desc, contains).equals(SLIGHT_LEFT)) {
						turnNameResult = SLIGHT_LEFT;
					}
				}
				// now look for Slight Right for "R" or "S" match
				if (turnNameResult.equals(RIGHT_)
						|| turnNameResult.equals(STRAIGHT)) {
					if (testRoutePtField(allPossibleTurnNames, rtPtTurnName, tempPoint.comment, contains).equals(SLIGHT_RIGHT)
							|| testRoutePtField(allPossibleTurnNames, rtPtTurnName, tempPoint.desc, contains).equals(SLIGHT_RIGHT)) {
						turnNameResult = SLIGHT_RIGHT;
					}
				}
				tempPoint.name = turnNameResult;
				mergedRoute.set(i, tempPoint);
			}//RoutePoint kind
		}// all elements in merged array
	}

	/**
	 * Failing to find a street name in any of the typical .gpx fields, use
	 * relative bearing to find a direction turn name
	 */
	private String getTurnNameFromBearings(int i, GPXRoutePoint tempPoint) {
		String turnNameResult;
		final String[] turnNames = { LEFT_, SLIGHT_LEFT, STRAIGHT, SLIGHT_RIGHT, RIGHT_, X};
		double tempLat1, tempLon1;
		double tempLat2, tempLon2;
		double tempLat3, tempLon3;
		GPXRoutePoint tempBeforeGPXRP;
		GPXRoutePoint tempAfterGPXRP;
		tempBeforeGPXRP = mergedRoute.get(i - 1);
		tempAfterGPXRP = mergedRoute.get(i + 1);
		tempLat1 = tempPoint.lat;
		tempLon1 = tempPoint.lon;
		tempLat2 = tempBeforeGPXRP.lat;
		tempLon2 = tempBeforeGPXRP.lon;
		tempLat3 = tempAfterGPXRP.lat;
		tempLon3 = tempAfterGPXRP.lon;

		here.setLatitude(tempLat1);
		here.setLongitude(tempLon1);
		there.setLatitude(tempLat2);
		there.setLongitude(tempLon2);
		float prevBearing = (here.bearingTo(there) + _360) % _360;
		there.setLatitude(tempLat3);
		there.setLongitude(tempLon3);
		float nextBearing = (here.bearingTo(there) + _360) % _360;
		int iconIndex = (int) Math.floor((((nextBearing - prevBearing + _360) % _360) / 45) + 0.5);
		// turn arrow behind us would show food or water icon
		if (iconIndex < 2) {
			iconIndex = 2;
		}
		if (iconIndex > 6) {
			iconIndex = 6;
		}
		turnNameResult = turnNames[iconIndex - 2];
		return turnNameResult;
	}

	/**
	 * step thru all possible synonyms of all possible turn names looking for a
	 * match to the string from the "possible field" of the RoutePoint.
	 * "possibleField" may come from the .name, .comment, .desc, .sym fields of the
	 * RoutePoint parsed from the xml file
	 */
	private String testRoutePtField(final ArrayList<String[]> allPossibleTurnNames,
			final String[] rtPtTurnName, String possibleField, boolean contains) {
		if (possibleField.equals("")) {
			return "";
		}
		String possibleFieldUC = possibleField.toUpperCase(Locale.US);
		//for each of the turn names like "SR", "SL"
		for (int rtPtTurnNameIndex = 0; rtPtTurnNameIndex < rtPtTurnName.length; rtPtTurnNameIndex++) {
			String turnName = rtPtTurnName[rtPtTurnNameIndex];
			String [] turnNameSynonymsList = allPossibleTurnNames.get(rtPtTurnNameIndex);
			//first test the turnName itself for an exact match; trim and uppercase
			if (turnName.equals(possibleField.trim().toUpperCase(Locale.US))) {
				// replace the synonym with the turnName, like "L", or "SR".
				// We'll step thru turnNames to find icon Index
				return turnName;
			} else {// did not match the single character case
				//now for each of the synonyms of the turn name, like "LEFT"
				for (String aTurnNameSynonymsList : turnNameSynonymsList) {
					if (contains) {
						if (possibleFieldUC.contains(aTurnNameSynonymsList)) {
							// replace the synonym with the turnName, like "L", or "SR".
							return turnName;
						}
					} else if (possibleFieldUC.equals(aTurnNameSynonymsList)) {
						return turnName;
					}
				}// for all synonyms of a particular rtPtTurnName
			}// not single character case
		}// try each category of turnName
		return "";
	}

	/**
	 * Insert each RoutePoint into merged ArrayList. First try
	 * mergeByProximity() to find a nearby TrackPoint. If that fails, add
	 * RoutePoint to an "orphans" list and continue merging RoutePoints. Go back
	 * to the orphans list and insert route points in between previously found
	 * matches.
	 */
	private void mergeGPX_TrackRoute() {
		if (mergedRoute.size() == 0) {
			// no TrackPoints in file -> insert all RoutePoints and exit
			mergedRoute.addAll(handler.handlersGPXRoute);
			return;
		}
		ArrayList<OrphanRoutePoint> orphansList = new ArrayList<>();
		int startIndex = 0;
		for (GPXRoutePoint tempGPXRoutePoint : handler.handlersGPXRoute) {
			int insertIdx = mergeByProximity(startIndex, tempGPXRoutePoint);
			if (insertIdx != -1) {
				// This will be the start distance for the search for the position
				// of the next Route Point in the merged Array
				startIndex = insertIdx + 1;
				mergedRoute.add(insertIdx + 1, tempGPXRoutePoint);
				// Go thru the orphans array and if any of the .endIndex is larger than the index of the current waypoint,
				// set the endIndex to the insertIdx. Once we've found a new position for a Route Point
				// any existing orphans must be before this in the merged Array.
				if (orphansList.size() > 0) {
					int i = 0;
					for (OrphanRoutePoint tempOrphan : orphansList) {
						if (tempOrphan.endIndex > insertIdx) {
							tempOrphan.endIndex = insertIdx;
							orphansList.set(i, tempOrphan);
						}
						i++;
					}// step thru all orphans updating endIndex
				}// have any orphans?
			} else {
				//found an orphan because mergeByProximity() returned -1
				// add tempGPXRoutePoint to orphan array, put startIndex and endIndex = merged Array.size()
				OrphanRoutePoint orphan = new OrphanRoutePoint(tempGPXRoutePoint, startIndex, mergedRoute.size() - 1);
				orphansList.add(orphan);
			}
		}// for all GPXRoutePoints in handlersGPXRoute
		// deal with orphaned RoutePoints
		if (orphansList.size() > 0) {
			mergeOrphanRoutePoints(orphansList);
		}
	}

	/**
	 * Insert a Route Point into an array of TrackPoints by looking for the
	 * entry that is within wpClose2TP distance. If no TrackPoint was close
	 * enough, return -1: an "orphan"
	 */
	private int mergeByProximity(int startIndex, GPXRoutePoint tempGPXRoutePoint) {
		if (startIndex > mergedRoute.size() - 1) {
			return mergedRoute.size();
		}
		for (int insertIdx = startIndex; insertIdx < mergedRoute.size(); insertIdx++) {
			if (distBetweenLatLonPoints(mergedRoute.get(insertIdx).lat,
					mergedRoute.get(insertIdx).lon, tempGPXRoutePoint.lat,
					tempGPXRoutePoint.lon) < wpClose2TP) {
				return insertIdx;
			}
		}
		// couldn't find proximate TrackPoint
		// add orphan to ArrayList of orphan RoutePoints and move on
		return -1;
	}

	/**
	 * Orphans are RoutePoints that couldn't be merged using the
	 * mergeByProximity method. We know that a RoutePoint before the orphan was
	 * merged at .startIndex and the RoutePoint after was merged at .endIndex.
	 * In this method, insert the orphan into the merged Array at the
	 * location nearest to one of the TrackPoints between start and end indices
	 */
	private void mergeOrphanRoutePoints(ArrayList<OrphanRoutePoint> orphans) {
		// For each element in orphans, calculate distance between orphan RoutePoint
		// and each merged Array RoutePoint between orphan's startIndex and endIndex.
		// Insert orphan into merged Array at index with minimum distance.
		// So we don't interfere with lower start & end indexes, start with orphans
		// at the end of the list and work down to the beginning
		for (int orphanIndex = orphans.size() -1; orphanIndex > -1; orphanIndex--) {
			GPXRoutePoint theOrphanRoutePoint = orphans.get(orphanIndex).thePoint;
			int insertIndex = mergedRoute.size();
			int orphanStartIndex = orphans.get(orphanIndex).startIndex;
			int orphanEndIndex = orphans.get(orphanIndex).endIndex;
			// This is the largest distance we can have - between the first and last expected
			// orphan locations. Initialize minDistance to the largest distance.
			double minDistance = distBetweenLatLonPoints(
					mergedRoute.get(orphanStartIndex).lat,
					mergedRoute.get(orphanStartIndex).lon,
					mergedRoute.get(orphanEndIndex).lat,
					mergedRoute.get(orphanEndIndex).lon);
			// Step thru the merged Array calculating distances between orphan RoutePoint and
			// each RoutePoint in the merged Array between start and end Index
			if (orphanStartIndex > orphanEndIndex) {
				orphanStartIndex = orphanEndIndex - 2;
			}
			for (int insertIdx = orphanStartIndex; insertIdx < orphanEndIndex; insertIdx++) {
				double orphanDistance = distBetweenLatLonPoints(mergedRoute.get(insertIdx).lat, mergedRoute.get(insertIdx).lon,
						theOrphanRoutePoint.lat, theOrphanRoutePoint.lon);
				if (orphanDistance < minDistance) {
					minDistance = orphanDistance;
					insertIndex = insertIdx;
				}//smaller distance from orphan to RoutePoint in mergedArray
			}//test between start and end orphan's index
			// now insert orphan into mergedArray at insertIndex place
			mergedRoute.add(insertIndex, theOrphanRoutePoint);
		}// repeat for all orphans in the list
	}

	private float distBetweenLatLonPoints(double tpLat, double tpLon, double rpLat, double rpLon) {
		float[] results = {0};
		Location.distanceBetween(tpLat, tpLon, rpLat, rpLon, results);
		return results[0];
	}

	private void calcMergedRouteMiles() {
		// this is just a preliminary RouteMiles calculation. It is updated in
		//distFromMyPlace2WP to display in the turn-by-turn list when we are between
		// waypoints. That method should use RouteMiles in its distance to WayPoint
		// for the RouteMiles preference.

		// We need RouteMiles (or the distance between RoutePoints) for thinning
		// and to determine the new firstListElem when changing TrackPoint density
		GPXRoutePoint tempRP;
		for (int i = 0; i < mergedRoute.size() - 1; i++) {
			tempRP = mergedRoute.get(i + 1);
			tempRP.distFromPrevWP = distBetweenLatLonPoints(tempRP.lat,
					tempRP.lon, mergedRoute.get(i).lat, mergedRoute.get(i).lon);
			tempRP.setRouteMiles(mergedRoute.get(i).getRouteMiles() + tempRP.distFromPrevWP);
			mergedRoute.set(i + 1, tempRP);
		}
	}

	private void thinTrkPtsNearRoutePt(double trkPtCloseDistance) {
		GPXRoutePoint theTempPoint;
		// step thru merged array stopping at each RoutePoint
		// if distance from this RoutePoint to the previous TrackPoints
		// is less than trkPtCloseDistance, mark that TrackPoint for deletion
		// next check distance to later TrackPoints and mark for deletion if trkPtCloseDistance
		for (int index = 0; index < mergedRoute.size() - 1; index++) {
			// is it a RoutePoint? or an importantClusterTrackPoint?
			if (mergedRoute.get(index).kind < clusterTrkPtKind) {
				float distance;
				if (index != 0 ) {// can't check before the first route point
					// check all points before the RoutePoint until one is too far away
					for (int j = index - 1; j > -1; j--) {
						theTempPoint = mergedRoute.get(j);
						distance = distBetweenLatLonPoints(theTempPoint.lat, theTempPoint.lon,
								mergedRoute.get(index).lat, mergedRoute.get(index).lon);
						// TrackPoint too close?
						if ((theTempPoint.kind > importantClusterTrkPtKind) && (distance < trkPtCloseDistance)) {
							// just mark for deletion; we'll delete at the right time in the thinning sequence
							theTempPoint.delete = true;
							mergedRoute.set(j, theTempPoint);
						} else {
							break;
						}
					}//test TrackPoints before
				}
				// check all points after the RoutePoint until one is too far away
				for (int j = index + 1; j < mergedRoute.size(); j++) {
					theTempPoint = mergedRoute.get(j);
					distance = distBetweenLatLonPoints(theTempPoint.lat, theTempPoint.lon,
							mergedRoute.get(index).lat, mergedRoute.get(index).lon);
					// TrackPoint too close?
					if ((theTempPoint.kind > importantClusterTrkPtKind) && (distance < trkPtCloseDistance)) {
						theTempPoint.delete = true;
						mergedRoute.set(j, theTempPoint);
					} else {
						break;
					}
				}// test TrackPoints after
			}// test this RoutePoint
		}// step through the merged array looking for RoutePoints.
	}

	private int thinTrkPts(double trkPtCloseDistance) {
		// already thinned out to high density
		GPXRoutePoint theSecondPoint;
		int rpCount = 10;
		// now step thru the merged array testing if TrackPoints are too close
		// to each other. We've already tested RoutePoints and know there aren't any
		// TrackPoints too close
		for (int index = 0; index < mergedRoute.size() - 2; index++) {
			// Is it a TrackPoint? or a clusterTrackPoint
			if (mergedRoute.get(index).kind > importantClusterTrkPtKind) {
				for (int j = index + 1; j < mergedRoute.size(); j++) {
					theSecondPoint = mergedRoute.get(j);
					// Is it a TrackPoint? Only delete TrackPoints, not
					// importantClusterTrkPts or RoutePts
					if (theSecondPoint.kind > importantClusterTrkPtKind) {
						// TrackPoint too close?
						if (distBetweenLatLonPoints(mergedRoute.get(index).lat,
								mergedRoute.get(index).lon, mergedRoute.get(j).lat,
								mergedRoute.get(j).lon) < trkPtCloseDistance) {
							// just mark it for deletion
							theSecondPoint.delete = true;
							mergedRoute.set(j, theSecondPoint);
						} else {
							theSecondPoint.delete = false;
							mergedRoute.set(j, theSecondPoint);
							index = j - 1;
							// found something too far away, continue with next TrackPoint
							break;
						}
					}
				}// check all points after the TrackPoint until one is too far away
			}// test against this TrackPoint
		}// step thru the merged Array
		return rpCount;
	}

	private void deleteThinnedPoints() {
		// now go thru the mergedArray deleting all the marked Track Points
		// have to do it backwards or index gets screwed-up
		// don't do this, it takes too much time; just ignore deleted points
		for (int index = mergedRoute.size() - 1; index > -1; index--) {
			if (mergedRoute.get(index).delete){
				mergedRoute.remove(index);
			}
		}
	}

	//___________________________________
	// these routines operate on the mergedRoute, thinned to TrackPoint density. The mergedRoute_HashMap
	// has the same size and indexing as the routeHashMap

	void refreshRouteWayPoints(Location here, double tripDistance) {
		/*
		 * Every time a new Position is received from Location Sensor
		 * decide if the current WayPoint has been reached. If so, set
		 * Proximate alarm. Also test if we've moved away enough to start looking for the next WP
		 * by setting .beenThere flag to true and canceling the proximity alarm.
		 */
		if (mergedRoute_HashMap.size() < 1)
			return;
		if (isProximate()) {
			testMovedAway(here);
		} else {
			testArrivedatWayPoint(here, tripDistance);
		}
	}// refreshRouteWayPoints()

	private void testMovedAway(Location here) {
		// After we've reached a WayPoint, don't increment currWP until we've moved away.
		there.setLatitude(mergedRoute_HashMap.get(currWP).lat);
		there.setLongitude(mergedRoute_HashMap.get(currWP).lon);
		// Make sure we're away from the closeEnough definition to be at a WayPoint
		double bearingToWP = (here.bearingTo(there) - getDOT() + _360) % _360;
		boolean bearingBehindUS = (bearingToWP > 125) & (bearingToWP < 235);
		//tooClose means we're still within "MOVED_AWAY_ENOUGH" feet of the WayPoint
		//want to keep the Proximity alarm active until we've passed by the WayPoint
		boolean tooClose = here.distanceTo(there) < movedAwayEnough;
		//one condition for having moved away is that we're in proximity, the relative
		//bearing is behind us and we're not too close in distance
		boolean condition1 = !tooClose & bearingBehindUS;
		boolean close = here.distanceTo(there) < closeEnough;
		//if somehow the first condition didn't catch, make sure that if we've just moved
		//away and we were proximate that we cancel the proximity alarm
		if (condition1 | !close) {
			//now cancel the proximity, set the WP to beenThere, increment currWP and firstListElem
			proximity = false;
			setCloseToWP(false);
			setBeenThere();
			currWP++;
			firstListElem = currWP;
		}
	}

	private void setBeenThere() {
		GPXRoutePoint tempRP;
		tempRP = mergedRoute_HashMap.get(currWP);
		tempRP.setBeenThere(true);
		mergedRoute_HashMap.set(currWP, tempRP);
	}

	private void testArrivedatWayPoint(Location here, double tripDistance) {
		int i = currWP;
		while (i < mergedRoute_HashMap.size()) {
			// test if we haven't beenThere and are closeEnough
			there.setLatitude(mergedRoute_HashMap.get(i).lat);
			there.setLongitude(mergedRoute_HashMap.get(i).lon);
			double distToWP = (here.distanceTo(there));
			setCloseToWP(distToWP < closeEnough);
			/*
			 * another necessary condition for arriving at a Way Point is that
			 * we've traveled far enough. On an out-and-back route we may be
			 * close to a Way Point at the end of the route. Test the distance
			 * segment - to segment to account for de-tours, short-cuts or extra
			 * loops that may add a lot of miles, or cut the route short. This
			 * is what "bonusMiles" represents. Add a bit of margin so we find
			 * proximity before the way point and detect way points close
			 * together. The margin should be big enough so we have success
			 * finding way points, but not so loose we detect wrong way points on
			 * an out-and-back route
			 */
			double routeDistance = mergedRoute_HashMap.get(i).getRouteMiles();
			if (getBonusMiles() > tripDistance) {
				setBonusMiles(0);
			}
			boolean farEnough = (tripDistance - getBonusMiles() + tripDistMargin) > routeDistance;
			/*
			 * if all conditions met, set that WP.beenThere = true and set every
			 * way point num < than that way point as .beenThere = true also.
			 *(Because we may have by-passed some way points) Find the
			 * difference between trip distance and route miles to reset bonus
			 * miles
			 */
			if (!farEnough){return;}
			if (isCloseToWP()) {
				proximity = true;
				for (int j = 0; j < i; j++) {
					GPXRoutePoint tempRtePt;
					tempRtePt = mergedRoute_HashMap.get(j);
					tempRtePt.setBeenThere(true);
					mergedRoute_HashMap.set(j, tempRtePt);
				}// for loop
				currWP = i;
				setBonusMiles(tripDistance + distToWP - routeDistance);
				return;// escape from while loop searching thru way points
			}// if close & !beenThere
			i++;
		}// while loop
	}

	private int getRoutePtTurnDirIndex(String turnDir, Context applicationContext) {
		// convert the turnDir name in the .name field into an index to the turn icons
		int turnDirIndex = 8;
		String turnDirUC = turnDir.toUpperCase(Locale.US);
		if (turnDirUC.equals(STRAIGHT)) {
			turnDirIndex = 4;
		} else if (turnDirUC.equals(RIGHT_)) {
			turnDirIndex = 6;
		} else if (turnDirUC.equals(LEFT_)) {
			turnDirIndex = 2;
		} else if (turnDirUC.equals(SLIGHT_RIGHT)) {
			turnDirIndex = 5;
		} else if (turnDirUC.equals(SLIGHT_LEFT)) {
			turnDirIndex = 3;
		} else if (turnDirUC.equals(U_TURN)) {
			turnDirIndex = 7;
		} else if (turnDirUC.contains(applicationContext.getString(R.string.food))) {
			turnDirIndex = 0;
		} else if (turnDirUC.contains(applicationContext.getString(R.string.water))) {
			turnDirIndex = 1;
		} else if (turnDirUC.contains(applicationContext.getString(R.string.summit))) {
			turnDirIndex = 29;
		} else if (turnDirUC.equals(X)) {
			turnDirIndex = 99;
		}
		return turnDirIndex;
	}// getTurnDirIndex

	boolean isProximate() {
		return proximity;
	}

	void setProximate(boolean proximity) {
		this.proximity = proximity;
	}

	double getDOT() {
		return DOT;
	}

	void setDOT(double dOT) {
		DOT = dOT;
	}

	double getBonusMiles() {
		return bonusMiles;
	}

	void setBonusMiles(double bonusMiles) {
		this.bonusMiles = bonusMiles;
	}

	private boolean isCloseToWP() {
		return closeToWP;
	}

	private void setCloseToWP(boolean closeToWP) {
		this.closeToWP = closeToWP;
	}

	double getPrevDOT() {
		return prevDOT;
	}

	void setPrevDOT(double prevDOT) {
		this.prevDOT = prevDOT;
	}

	double getDeltaDOT() {
		return deltaDOT;
	}

	void setDeltaDOT(double deltaDOT) {
		this.deltaDOT = deltaDOT;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	/** When track point density has changed, set the firstListElem to
	the Waypoint that used to be at the top of the turn-by-turn list, based on RouteMiles */
	void recalcFirstListElem() {
		// step thru the route comparing RouteMiles to parameter
		int newFirstListElem = 0;
		for (GPXRoutePoint tempRP : mergedRoute_HashMap) {
			if (tempRP.getRouteMiles() >= routeMilesatFirstListElem) {
				firstListElem = newFirstListElem;
				break;
			}
			newFirstListElem++;
		}
	}
	/** When track point density has changed, set the currWP to
	the Waypoint that used to be currWP, based on RouteMiles */

	void recalcCurrWP(double milesTraveled) {
		// step thru the route comparing RouteMiles to parameter
		int wayPtNum = 0;
		for (GPXRoutePoint tempRP : mergedRoute_HashMap) {
			if (tempRP.getRouteMiles() >= milesTraveled) {
				currWP = wayPtNum;
				break;
			}
			wayPtNum++;
		}
	}

}
