package com.cyclebikeapp.ble;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.Locale;

class MySAXHandler extends DefaultHandler {
	private static final int trkPtKind = 100;
	private static final int routePtKind = 1;
	private static final String lcLON = "lon";
	private static final String lcLAT = "lat";
	private static final String LNG = "LNG";
	private static final String LAT = "LAT";
	private static final String DIRECTIONS = "DIRECTIONS";
	private static final String POINTTYPE = "POINTTYPE";
	private static final String SYM = "SYM";
	private static final String TYPE = "TYPE";
	private static final String NOTES = "NOTES";
	private static final String CMT = "CMT";
	private static final String DESC = "DESC";
	private static final String NAME2 = "NAME";
	private static final String ELE = "ELE";
	private static final String HEIGHT = "HEIGHT";
	private static final String ALTITUDEMETERS = "ALTITUDEMETERS";
	private static final String LATITUDEDEGREES = "LATITUDEDEGREES";
	private static final String LONGITUDEDEGREES = "LONGITUDEDEGREES";
	//tagLevel keeps track of the XML file hierarchy
	private int tagLevel = -1;
	private static final String lvl0TagName_1 = "gpx";
	private static final String lvl0TagName_2 = "Course";
	private static final String lvl0TagName_3 = "points";
	private static final String lvl0TagName_4 = "trk";
	private static final String lvl1TagName_1 = "wpt";
	private static final String lvl1TagName_2 = "rtept";
	private static final String lvl1TagName_3 = "CoursePoint";
	private static final String lvl1TagName_4 = "point";
	private static final String lvl1TagName_5 = "trkpt";
	private static final String lvl1TagName_6 = "Trackpoint";
	private static final String lvl2TagName_1 = "POSITION";
	// temporary field used to assemble each entry in the ArrayList	
	private GPXRoutePoint thePoint = new GPXRoutePoint(); 
	//GPXRoute cues off "wpt", "CoursePoint" or "rtept" tags to find turn direction way points
    final ArrayList<GPXRoutePoint> handlersGPXRoute = new ArrayList<>();
	//TrackPtRoute cues off "trkpt"tags to find Track Points
    final ArrayList<GPXRoutePoint> handlersTrackPtRoute = new ArrayList<>();
	private final StringBuilder buff = new StringBuilder(20);
	
	MySAXHandler() {
		super();
	}

    ////////////////////////////////////////////////////////////////////
    // Event handlers.
    ////////////////////////////////////////////////////////////////////

	@Override
	public void startDocument() {
		tagLevel = -1;
		// need to flush out the old handlersGPXRoute when loading another route
		handlersGPXRoute.clear();
		handlersTrackPtRoute.clear();
	}

	@Override
	public void endDocument() {
	}

	@Override
	public void startElement(String uri, String name, String qName, Attributes atts) throws NumberFormatException {
		String tagName;
		if ("".equals(uri)) {
			tagName = qName;
		} else {
			tagName = name;
		}
		switch (tagLevel) {
		case -1:
			if (tagName.equals(lvl0TagName_1) 
					|| tagName.equals(lvl0TagName_2)
					|| tagName.equals(lvl0TagName_3)
					|| tagName.equals(lvl0TagName_4))
				tagLevel = 0;
			break;
		case 0:
			if (tagName.equals(lvl1TagName_5) 
					|| tagName.equals(lvl1TagName_6)
					|| tagName.equals(lvl1TagName_1)
					|| tagName.equals(lvl1TagName_2)
					|| tagName.equals(lvl1TagName_3)
					|| tagName.equals(lvl1TagName_4)) {
				tagLevel = 1;
				thePoint = new GPXRoutePoint();
				if (atts.getValue(lcLAT) != null) {
					thePoint.lat = Double.valueOf(atts.getValue(lcLAT));
				}
				if (atts.getValue(lcLON) != null) {
					thePoint.lon = Double.valueOf(atts.getValue(lcLON));
				}
			}
			break;
		case 1:
			if (tagName.toUpperCase((Locale.US)).equals(lvl2TagName_1)) {
				tagLevel = 2;
			}
		default:
			break;
		}
		clearBuff();
		// some GPX implementations like track, points, waypoints may have the
		// element info in other tags
	}

	@Override
	public void endElement(String uri, String name, String qName) throws NumberFormatException {
		String tagName;
		if ("".equals(uri)) {
			tagName = qName;
		} else {
			tagName = name;
		}
		String tagNameUC = tagName.toUpperCase(Locale.US);
		//this is the end of an element. Check if we are closing a hierarchy level.
		//level 0 = "gpx" ; another level exists for Routes - rte, but "gpx" gets by all the up-front hash
		//level 1 = "rtept" or "wpt" or "trkpt" or "Trackpoint"
		//level 2 = "Position" in BikeRouteToaster files or RWGPS tcx files

		// this is a strange GPX Route file type from GPSies, where TrackPoints are labeled
		// with "rtept", but there is no turn information. Treat it like a TrackPoint
		boolean oddCondition = tagName.equals(lvl1TagName_2)
				&& thePoint.comment.equals("")
				&& thePoint.name.equals("")
				&& thePoint.sym.equals("")
				&& thePoint.type.equals("");
		switch (tagLevel) {
		case 0:
			if (tagName.equals(lvl0TagName_1)
					|| tagName.equals(lvl0TagName_2)
					|| tagName.equals(lvl0TagName_3)
					|| tagName.equals(lvl0TagName_4)) {
				tagLevel = -1;
			}
			break;
		case 1:
			if (tagNameUC.equals(ELE)
					|| tagNameUC.equals(ALTITUDEMETERS)
					|| tagNameUC.equals(HEIGHT)) {
				thePoint.elevation = Double.valueOf(buff.toString());
			}
			if (tagName.equals(lvl1TagName_5)
					|| tagName.equals(lvl1TagName_6)
					|| oddCondition) {
				// deal with track points in a seperate ArrayList in case there
				// are both route points and track points in the file
				thePoint.kind = trkPtKind;
				thePoint.comment = "TrackPoint" + handlersTrackPtRoute.size();
				handlersTrackPtRoute.add(thePoint);
				tagLevel = 0;
			} else if (tagNameUC.equals(LAT)) {
				thePoint.lat = Double.valueOf(buff.toString());
			} else if (tagNameUC.equals(LNG)) {
				thePoint.lon = Double.valueOf(buff.toString());
			} else if (tagNameUC.equals(NAME2)) {
				thePoint.name = buff.toString();
			} else if (tagNameUC.equals(DESC)) {
				thePoint.desc = buff.toString();
			} else if (tagNameUC.equals(CMT)
					|| tagNameUC.equals(NOTES)
					|| tagNameUC.equals(DIRECTIONS)) {
				thePoint.comment = buff.toString();
			} else if (tagNameUC.equals(TYPE)
					|| tagNameUC.equals(POINTTYPE)) {
				thePoint.type = buff.toString();
			} else if (tagNameUC.equals(SYM)) {
				thePoint.sym = buff.toString();
			} else if (tagName.equals(lvl1TagName_1)
					|| tagName.equals(lvl1TagName_2)
					|| tagName.equals(lvl1TagName_3)
					|| tagName.equals(lvl1TagName_4)) {
				thePoint.kind = routePtKind;
				handlersGPXRoute.add(thePoint);
				tagLevel = 0;
			}
			clearBuff();
			break;
		case 2:
			//in BikeRouteToaster and RWGPS tcx file latitude and longitude are in a <Position> level
			switch (tagNameUC) {
				case LATITUDEDEGREES:
					thePoint.lat = Double.valueOf(buff.toString());
					break;
				case LONGITUDEDEGREES:
					thePoint.lon = Double.valueOf(buff.toString());
					break;
				case lvl2TagName_1:
					tagLevel = 1;
					break;
			}
		default:
			break;
		}
	}

	private void clearBuff() {
		if (buff.length() > 0) {
			buff.delete(0, buff.length());
		}
	}

	@Override
	public void characters(char ch[], int start, int length) {
		buff.append(ch, start, length);
	}

}