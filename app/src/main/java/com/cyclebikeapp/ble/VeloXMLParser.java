package com.cyclebikeapp.ble;

import android.location.Location;
import android.location.LocationManager;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

class VeloXMLParser extends DefaultHandler {

	private boolean currentElement = false;
	private String currentValue = "";
	private Location loc = new Location(LocationManager.PASSIVE_PROVIDER);
	private VelodromeSpec velo;
	private ArrayList<VelodromeSpec> veloList;

	ArrayList<VelodromeSpec> getVeloList() {
		return veloList;
	}

	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		currentElement = true;
		switch (qName) {
			case "VelodromeList":
				veloList = new ArrayList<>();
				break;
			case "Velodrome":
				velo = new VelodromeSpec();
				break;
			case "CenterLocation":
				loc = new Location(LocationManager.PASSIVE_PROVIDER);
				//since this is a built-in XML file we don't have to check for valid doubles
				if (attributes.getValue("lat") != null)
					loc.setLatitude(Double.valueOf(attributes.getValue("lat")));
				if (attributes.getValue("lon") != null)
					loc.setLongitude(Double.valueOf(attributes.getValue("lon")));
				break;
		}
	}

	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		currentElement = false;
		if (qName.equalsIgnoreCase("MinorAxis")) {
			velo.setMinorAxis(Double.valueOf(currentValue.trim()));
		} else if (qName.equalsIgnoreCase("MajorAxis")) {
			velo.setMajorAxis(Double.valueOf(currentValue.trim()));
		} else if (qName.equalsIgnoreCase("CenterLocation")) {
			velo.setCenterLocation(loc);
		} else if (qName.equalsIgnoreCase("tilt")) {
			velo.setTilt(Double.valueOf(currentValue.trim()));
		} else if (qName.equalsIgnoreCase("cmt")) {
			velo.setComment(currentValue.trim());
		} else if (qName.equalsIgnoreCase("Name")) {
			velo.setVelodromeName(currentValue.trim());
		} else if (qName.equalsIgnoreCase("Velodrome")) {
			veloList.add(velo);
		}
		currentValue = "";
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (currentElement) {
			currentValue = currentValue + new String(ch, start, length);
		}
	}

}
