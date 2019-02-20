package com.cyclebikeapp.ble;

import android.content.Context;
import android.content.res.AssetManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

class LocationSpoofer {

	private static final double EARTH_RADIUS = 6371.*1000;// meters
	private final Context context;
	private ArrayList<VelodromeSpec> veloList = new ArrayList<>();
	static private final boolean debugLanguage = false;
	
	LocationSpoofer(Context mcontext) {
		if (MainActivity.debugAppState) Log.i(this.getClass().getName(), "LocationSpoofer - parseXML");
		context = mcontext;
		parseXML();
	}

	/**
	 * use bike speed-distance sensor to create an elliptical track of timed locations
	 * write these locations in bikeStat, so writeTrackRecord will access the new locations
	 * @return  the information
	 */
	String[] spoofLocations(boolean firstSpoof, BikeStat bs, int veloChoice){
		//if (DisplayActivity.debugAppState) Log.w(this.getClass().getName(), "spoofingLocations");
		Location spoofLocation;
		VelodromeSpec chosenVelo;
		chosenVelo = veloList.get(veloChoice);
		//don't display GPS speed
		spoofLocation = calcNextSpoofLocation1(bs, chosenVelo, firstSpoof);
		bs.setLastGoodWP(spoofLocation, firstSpoof);
		return new String[]{chosenVelo.getVelodromeName(),chosenVelo.getComment()};
	}

	/**
	 * given the previous Location and distance travelled, find the next
	 * Location and new bearing for an elliptical track using VelodromeSpec parameters
	 */
	
	private Location calcNextSpoofLocation1(BikeStat bs, VelodromeSpec velo, boolean firstSpoof) {
		//this is a variable to hold the result, new Location
		Location spoofLocation = new Location(LocationManager.PASSIVE_PROVIDER);
		Double deltaDistance = bs.getSpoofWheelTripDistance() - bs.getPrevSpoofWheelTripDistance();
		bs.setPrevSpoofWheelTripDistance(bs.getSpoofWheelTripDistance());
		Double previousBearing = Math.toRadians(bs.getLastGoodWP().getBearing());
		double r = velo.getMinorAxis();
		double R = velo.getMajorAxis();
		//calculate new bearing from center point along ellipse
		Double sPB = Math.sin(previousBearing);
		Double cPB = Math.cos(previousBearing);
		Double newBearing = 0.;
		if (!firstSpoof){
			newBearing = deltaDistance / Math.sqrt(r*r*cPB*cPB + R*R*sPB*sPB) + previousBearing;
		}
		//calculate distance from centerLocation, along newBearing to the newLocation
		// in radians, Location is in degrees
		Double tilt = Math.toRadians(velo.getTilt());
		Double projection = Math.sqrt(2.) * R * r
				/ Math.sqrt((r * r - R * R)
						* Math.cos(2 * (newBearing - tilt)) + r * r + R * R);
		Double lat1 = Math.toRadians(velo.getCenterLocation().getLatitude());
		Double lon1 = Math.toRadians(velo.getCenterLocation().getLongitude());
		// lat2 and lon2 are in radians
		Double lat2 = Math.asin(Math.sin(lat1) * Math.cos(projection / EARTH_RADIUS) + 
				Math.cos(lat1) * Math.sin(projection / EARTH_RADIUS)*Math.cos(newBearing));
		Double lon2 = lon1 + Math.atan2(Math.sin(newBearing) * Math.sin(projection / EARTH_RADIUS) * Math.cos(lat1), 
               (Math.cos(projection / EARTH_RADIUS) - Math.sin(lat1 ) * Math.sin(lat2)));
		spoofLocation.setLatitude(Math.toDegrees(lat2));
		spoofLocation.setLongitude(Math.toDegrees(lon2));
		spoofLocation.setAltitude(0.);
		spoofLocation.setBearing((float) Math.toDegrees(newBearing));
		spoofLocation.setTime(System.currentTimeMillis());
		return spoofLocation;
	}
	private void parseXML() {
		AssetManager assetManager = context.getAssets();
		String locale = Locale.getDefault().getDisplayName();
		String veloListName = "velodromeList.xml";
		//TODO change velodromeList filename depending on locale language
		if (locale.toUpperCase(Locale.US).contains("ESPANOL")){
			veloListName = "velodromeList_es.xml";
		} else if (locale.toUpperCase(Locale.US).contains("ITALIANO")){
			veloListName = "velodromeList_it.xml";
		} else if (locale.toUpperCase(Locale.US).contains("DEUTSCH")){
			veloListName = "velodromeList_de.xml";
		} else if (locale.toUpperCase(Locale.US).contains("FRANCAIS")){
			veloListName = "velodromeList_fr.xml";
		}
		if (debugLanguage ){
			Log.i(this.getClass().getName(), "locale: " + locale);
			Log.i(this.getClass().getName(), "veloListName: " + veloListName);
		}
		try {
			InputStream mInputStream = assetManager.open(veloListName);
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();

			VeloXMLParser myXMLHandler = new VeloXMLParser();
			xr.setContentHandler(myXMLHandler);
			InputSource inStream = new InputSource(mInputStream);
			xr.parse(inStream);
			veloList = myXMLHandler.getVeloList();
			mInputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}//parse XML file of velodromes from file in asset folder

}
