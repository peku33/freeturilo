package net.peku33.freeturilo.core;

import java.io.Serializable;

/**
 * Klasa reprezentująca ścieżkę jako zbiór punktów + szacowany czas jej pokonania
 * @author peku33
 *
 */
public class Path implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Iterable<GeoPoint> geoPoints;
	private long timeMillis;
	
	public Path(Iterable<GeoPoint> geoPoints, long timeMillis) {
		this.geoPoints = geoPoints;
		this.timeMillis = timeMillis;
	}
	
	public Iterable<GeoPoint> getGeoPoints() {
		return geoPoints;
	}
	public long getTimeMillis() {
		return timeMillis;
	}
}
