package com.cyclebikeapp.ble;

import com.garmin.fit.DateTime;
import com.garmin.fit.RecordMesg;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Read-in .tcx activity file, parse it to extract RecordMesgs to encode a .fit
 * format activity file.
 * 
 * */
class MyTCXSAXHandler extends DefaultHandler {
/**<Lap StartTime= "2013-11-22T18:11:39Z">
*  <Trackpoint>
*	<Time>2013-11-22T18:11:42Z</Time>
*	<Position>
*	<LatitudeDegrees>37.4107871</LatitudeDegrees>
*	<LongitudeDegrees>-122.0946846</LongitudeDegrees>
*	</Position>
*	<AltitudeMeters>-30.00</AltitudeMeters>
*	<DistanceMeters>11.37</DistanceMeters>
*	<HeartRateBpm xsi:type="HeartRateInBeatsPerMinute_t">
*		<Value>71</Value>
*	</HeartRateBpm>
*	<Cadence>49</Cadence>
*	<Extensions>
*		<TPX xmlns="http://www.garmin.com/xmlschemas/ActivityExtension/v2">
*		<Speed>4.50</Speed>
*		<Watts>0</Watts>
*		</TPX>
*	</Extensions>
* </Trackpoint>
**/
	private static final String DATE_TIME_Z_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
	private int tagLevel = -1;
	private static final String lvl0LapTagName = "Lap";
	private static final String lvl1TrackpointTagName = "Trackpoint";
	private static final String lvl2TimeTagNameUF = "UFTime";
	private static final String lvl2AltitudeTagName = "AltitudeMeters";
	private static final String lvl2DistanceTagName = "DistanceMeters";
	private static final String lvl2CadenceTagName = "Cadence";
	private static final String lvl2PositionTagName = "Position";
	private static final String lvl2HeartRateTagName = "HeartRateBpm";
	private static final String lvl2ExtensionsTagName = "Extensions";
	private static final Object lvl3LatitudeTagNameSC = "LatitudeDegreesSC";
	private static final Object lvl3LocationAccuracyTagName = "LocationAccuracy";
    private static final Object lvl3NumSatellitesTagName = "NumSatellites";
    private static final Object lvl3LongitudeTagNameSC = "LongitudeDegreesSC";
	private static final String lvl3HRValueTagName = "Value";
	private static final String lvl3TPXTagName = "TPX";
	private static final String lvl4SpeedTagName = "Speed";
	private static final String lvl4WattsTagName = "Watts";
	
	//summary tagNames
	private static final String lvl1SumTotalTimeSecondsTagName = "TotalTimeSeconds";
	private static final String lvl1SumTotalDistanceTagName = "DistanceMeters";
	private static final String lvl1SumMaxSpeedTagName = "MaximumSpeed";
	private static final String lvl1SumAvgHeartRateTagName = "AverageHeartRateBpm";
	private static final String lvl1SumMaxHeartRateTagName = "MaximumHeartRateBpm";
	private static final String lvl1SumAvgCadenceTagName = "Cadence";
	private static final String lvl1SumAvgWattsTagName = "AvgWatts";
	private static final String lvl1SumExtensionsTagName = "Extensions";
	private static final String lvl2SumAvgHRValueTagName = "Value";
	private static final String lvl2SumMaxHRValueTagName = "Value";
	private static final String lvl2SumExtLXTagName = "LX";
	private static final String lvl3SumAvgSpeedTagName = "AvgSpeed";
	private static final String lvl3SumMaxCadenceTagName = "MaxBikeCadence";

	private final SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_Z_FORMAT, Locale.US);
	// temporary field used to assemble each entry in the ArrayList
	private RecordMesg theRecordMesg = new RecordMesg(); 
	//GPXRoute cues off "wpt", "CoursePoint" or "rtept" tags to find turn direction way points
    final ArrayList<RecordMesg> handlersRecordMesgs = new ArrayList<>();
	private final StringBuilder buff = new StringBuilder(20);
	float totalTimerTime = 0;
	float totalDistance = 0;
	float maxSpeed = 0;
	float avgSpeed = 0;
	float avgHeartRate = 0;
	float maxHeartRate = 0;
	float avgCadence = 0;
	float maxCadence = 0;
	float avgPower = 0;
	final float maxPower = 0;
	private boolean summarizing = false;
	private String lvl1SumTagName = "";

	MyTCXSAXHandler() {
		super();
	}

    ////////////////////////////////////////////////////////////////////
    // Event handlers.
    ////////////////////////////////////////////////////////////////////

	@Override
	public void startDocument() {
		summarizing = false;
		tagLevel = -1;
		// need to flush out the old handlersGPXRoute when loading another route
		handlersRecordMesgs.clear();
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		//Log.v(this.getClass().getName(), " - TCXSAXHandler starting to parse file");
	}

	@Override
	public void endDocument() {
		//Log.v(this.getClass().getName(), " - TCXSAXHandler finished parsing file");
		summarizing = false;
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
			if (tagName.equals(lvl0LapTagName))
				tagLevel = 0;
			break;
			// now look for TrackPoint tag
		case 0:
			if (tagName.equals(lvl1TrackpointTagName)) {
				tagLevel = 1;
				theRecordMesg = new RecordMesg();
			} else if (tagName.equals(lvl1SumTotalTimeSecondsTagName)
					|| tagName.equals(lvl1SumTotalDistanceTagName)
					|| tagName.equals(lvl1SumMaxSpeedTagName)
					|| tagName.equals(lvl1SumAvgHeartRateTagName)
					|| tagName.equals(lvl1SumMaxHeartRateTagName)
					|| tagName.equals(lvl1SumAvgCadenceTagName)
					|| tagName.equals(lvl1SumAvgWattsTagName)
					|| tagName.equals(lvl1SumExtensionsTagName)) {
				tagLevel = 1;
				lvl1SumTagName = tagName;
				summarizing  = true;
			}
			break;
			// now look for lvl 2 tags
		case 1:
			if (tagName.equals(lvl2TimeTagNameUF)
					|| tagName.equals(lvl2PositionTagName)
					|| tagName.equals(lvl2AltitudeTagName)
					|| tagName.equals(lvl2DistanceTagName)
					|| tagName.equals(lvl2HeartRateTagName)
					|| tagName.equals(lvl2CadenceTagName)
					|| tagName.equals(lvl2ExtensionsTagName)) {
				tagLevel = 2;
			}			
			else if (tagName.equals(lvl2SumAvgHRValueTagName)
					|| tagName.equals(lvl2SumMaxHRValueTagName)
					|| tagName.equals(lvl2SumExtLXTagName)) {
				tagLevel = 2;
				summarizing  = true;
//				Log.i(this.getClass().getName(),  "lvl 2 summary tags - " + tagName);
			}
			break;
			// now look for lvl 3 tags
		case 2:
			if (tagName.equals(lvl3LatitudeTagNameSC)
					|| tagName.equals(lvl3LongitudeTagNameSC)
                    || tagName.equals(lvl3NumSatellitesTagName)
                    || tagName.equals(lvl3LocationAccuracyTagName)
					|| tagName.equals(lvl3TPXTagName)
					|| tagName.equals(lvl3HRValueTagName)) {
				tagLevel = 3;
			}
			else if (tagName.equals(lvl3SumAvgSpeedTagName)
					|| tagName.equals(lvl3SumMaxCadenceTagName)) {
				tagLevel = 3;
				summarizing  = true;
//				Log.i(this.getClass().getName(),  "lvl 3 summary tags - " + tagName);
			}
			break;
			// now look for lvl 4 tags
		case 3:
			if (tagName.equals(lvl4SpeedTagName)
					|| tagName.equals(lvl4WattsTagName)) {
				tagLevel = 4;
			}
			break;
		default:
			break;
		}
		clearBuff();
	}

	@Override
	public void endElement(String uri, String name, String qName) throws NumberFormatException {
		String tagName;
		if ("".equals(uri)) {
			tagName = qName;
		} else {
			tagName = name;
		}
		//lvl1
//		<TotalTimeSeconds>3875.10</TotalTimeSeconds>
//		<DistanceMeters>23893.53</DistanceMeters>
//		<MaximumSpeed>12.50</MaximumSpeed>
//		<AverageHeartRateBpm xsi:type="HeartRateInBeatsPerMinute_t">
			//lvl2
//			<Value>100</Value>
//		</AverageHeartRateBpm>
//		<MaximumHeartRateBpm xsi:type="HeartRateInBeatsPerMinute_t">
			//lvl2
//			<Value>133</Value>
//		</MaximumHeartRateBpm>
//		<Cadence>54</Cadence>
//		<Extensions>
			//lvl2
//			<LX xmlns="http://www.garmin.com/xmlschemas/ActivityExtension/v2">
				//lvl3
//				<AvgSpeed>6.17</AvgSpeed>
//				<MaxBikeCadence>99</MaxBikeCadence>
//			</LX>
//		</Extensions>
//		</Lap>
		//extract summary values from tcx file footer
		if (summarizing) {
//			Log.d("CycleBike" + "TCXSAX", "summarizing endTag: " + tagName + " tagLevel: " + tagLevel);
			switch (tagLevel) {
			case 0:
				break;
			case 1:
				switch (tagName) {
					case lvl1SumTotalTimeSecondsTagName:
//					 Log.i(this.getClass().getName(), "totalTimerTime: " + buff.toString());
						totalTimerTime = Double.valueOf(buff.toString()).floatValue();
						tagLevel = 0;
						clearBuff();
						break;
					case lvl1SumTotalDistanceTagName:
//					 Log.i(this.getClass().getName(), "totalDistance: " + buff.toString());
						totalDistance = Double.valueOf(buff.toString()).floatValue();
						tagLevel = 0;
						clearBuff();
						break;
					case lvl1SumMaxSpeedTagName:
//					 Log.i(this.getClass().getName(), "maxSpeed: " + buff.toString());
						maxSpeed = Double.valueOf(buff.toString()).floatValue();
						tagLevel = 0;
						clearBuff();
						break;
					case lvl1SumAvgHeartRateTagName:
						tagLevel = 0;
						clearBuff();
						break;
					case lvl1SumMaxHeartRateTagName:
						tagLevel = 0;
						clearBuff();
						break;
					case lvl1SumAvgCadenceTagName:
//					 Log.i(this.getClass().getName(), "avgCadence: " + buff.toString());
						avgCadence = Integer.valueOf(buff.toString()).floatValue();
						tagLevel = 0;
						clearBuff();
						break;
					case lvl1SumAvgWattsTagName:
//					Log.i(this.getClass().getName(), "avgWatts: " + buff.toString());
						avgPower = Integer.valueOf(buff.toString()).floatValue();
						tagLevel = 0;
						clearBuff();
						break;
					case lvl1SumExtensionsTagName:
						summarizing = false;
						tagLevel = 0;
						clearBuff();
						break;
				}
				break;
			case 2:
				if (tagName.equals(lvl2SumAvgHRValueTagName) 
						&& lvl1SumTagName.equals(lvl1SumAvgHeartRateTagName) ) {
//					 Log.i(this.getClass().getName(), "avgHeartRate: " + buff.toString());
					avgHeartRate = Integer.valueOf(buff.toString()).floatValue();
					tagLevel = 1;
					clearBuff();
				} else if (tagName.equals(lvl2SumMaxHRValueTagName) 
						&& lvl1SumTagName.equals(lvl1SumMaxHeartRateTagName) ) {
//					Log.i(this.getClass().getName(), "maxHeartRate: " + buff.toString());
					maxHeartRate = Integer.valueOf(buff.toString()).floatValue();
					tagLevel = 1;
					clearBuff();
				} else if (tagName.equals(lvl2SumExtLXTagName)) {
					tagLevel = 1;
					clearBuff();
				}
				break;
			case 3:
				if (tagName.equals(lvl3SumAvgSpeedTagName)) {
//					 Log.i(this.getClass().getName(), "avgSpeed: " + buff.toString());
					avgSpeed = Double.valueOf(buff.toString()).floatValue();
					tagLevel = 2;
					clearBuff();
				} else if (tagName.equals(lvl3SumMaxCadenceTagName)) {
//					 Log.i(this.getClass().getName(), "maxCadence: " + buff.toString());
					maxCadence = Integer.valueOf(buff.toString()).floatValue();
					tagLevel = 2;
					clearBuff();
				}
				break;
			}

		}  else {
		//this is the end of an element. Check if we are closing a hierarchy level.
//		Log.d(this.getClass().getName(), "endTag: " + tagName + " tagLevel: " + tagLevel);
		switch (tagLevel) {
		case 0:
			if (tagName.equals(lvl0LapTagName)) {
				tagLevel = -1;
			}
			break;
		case 1:
			if (tagName.equals(lvl1TrackpointTagName)) {
				handlersRecordMesgs.add(theRecordMesg);
				tagLevel = 0;
			}
//			Log.i(this.getClass().getName(), "added RecordMesg - " + handlersRecordMesgs.size());
			clearBuff();
			break;
		case 2:
			switch (tagName) {
				case lvl2TimeTagNameUF:
//				DateTime uf = new DateTime(Integer.valueOf(buff.toString()) - DateTime.OFFSET/1000);
//			     Log.i(this.getClass().getName(), "UF " + buff.toString());
//			     Log.i(this.getClass().getName(), "ufDateTime: " + uf.getDate().toString());
					theRecordMesg.setTimestamp(new DateTime(Integer.valueOf(buff.toString())));
					tagLevel = 1;
					clearBuff();
					break;
				case lvl2AltitudeTagName:
					theRecordMesg.setAltitude(Double.valueOf(buff.toString()).floatValue());
//				 Log.i(this.getClass().getName(), "Altitude: " + buff.toString());
					tagLevel = 1;
					clearBuff();
					break;
				case lvl2DistanceTagName:
					theRecordMesg.setDistance(Double.valueOf(buff.toString()).floatValue());
//				 Log.i(this.getClass().getName(), "Distance: " + buff.toString());
					tagLevel = 1;
					clearBuff();
					break;
				case lvl2CadenceTagName:
					theRecordMesg.setCadence(Integer.valueOf(buff.toString()).shortValue());
//				 Log.i(this.getClass().getName(), "Cadence: " + buff.toString());
					tagLevel = 1;
					clearBuff();
					break;
				case lvl2PositionTagName:
				case lvl2HeartRateTagName:
				case lvl2ExtensionsTagName:
					tagLevel = 1;
					clearBuff();
					break;
			}
			break;
		case 3:
			if (tagName.equals(lvl3LatitudeTagNameSC)) {
				theRecordMesg.setPositionLat(Integer.valueOf(buff.toString()));
				tagLevel = 2;
				clearBuff();
			} else if (tagName.equals(lvl3LongitudeTagNameSC)) {
				theRecordMesg.setPositionLong(Integer.valueOf(buff.toString()));
				tagLevel = 2;
				clearBuff();
			} else if (tagName.equals(lvl3LocationAccuracyTagName)) {
                theRecordMesg.setGpsAccuracy(Integer.valueOf(buff.toString()).shortValue());
                tagLevel = 2;
                clearBuff();
            } else if (tagName.equals(lvl3NumSatellitesTagName)) {
                theRecordMesg.setCycles(Integer.valueOf(buff.toString()).shortValue());
                tagLevel = 2;
                clearBuff();
            } else if (tagName.equals(lvl3HRValueTagName)) {
				theRecordMesg.setHeartRate((Integer.valueOf(buff.toString()).shortValue()));
//				 Log.i(this.getClass().getName(), "HeartRate: " + buff.toString());
				tagLevel = 2;
				clearBuff();
			} else if (tagName.equals(lvl3TPXTagName)){
				tagLevel = 2;
				clearBuff();				
			}
			break;
		case 4:
			if (tagName.equals(lvl4SpeedTagName)) {
				theRecordMesg.setSpeed(Double.valueOf(buff.toString()).floatValue());
//				 Log.i(this.getClass().getName(), "Speed: " + buff.toString());
				tagLevel = 3;
				clearBuff();
			} else if (tagName.equals(lvl4WattsTagName)) {
				theRecordMesg.setPower(Integer.valueOf(buff.toString()));
//				 Log.i(this.getClass().getName(), "Power: " + buff.toString());
				tagLevel = 3;
				clearBuff();
			}
			break;
		default:
			break;
		}// switch on tagName
		}// not summarizing
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