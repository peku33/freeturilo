package net.peku33.freeturilo.core;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.Locale;

/**
 * Klasa reprezentująca współrzędne geograficzne
 * @author peku33
 *
 */
public class GeoPoint implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private double latitude;
	private double longitude;
	
	public GeoPoint(double latitude, double longitude) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}
	
	public static double calculateAbsoluteDistance(GeoPoint from, GeoPoint to) {
		return Math.sqrt(
			Math.pow(from.getLatitude() - to.getLatitude(), 2)
			+
			Math.pow(from.getLongitude() - to.getLongitude(), 2)
		);
	}
	
	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%f, %f", latitude, longitude);
	}
	
	/**
	 * Parsuje współrzędne w postaci
	 * xx.xxxxxxxxx, yy.yyyyyyyyyyy
	 * 
	 * @param from
	 * @return
	 */
	public static GeoPoint fromString(String from) {
		
		// Dzielimy napis po przecinku
		String parts[] = from.split(",");
		
		// Jeśli nie ma dwóch części - problem
		if(parts.length != 2)
			throw new InvalidParameterException();
		
		String latitudeString = parts[0].trim();
		String longitudeString = parts[1].trim();
		
		double latitude = Double.parseDouble(latitudeString);
		double longitude = Double.parseDouble(longitudeString);
		
		return new GeoPoint(latitude, longitude);
	}
}
