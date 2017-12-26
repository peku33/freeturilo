package net.peku33.freeturilo.core;

import java.io.Serializable;

/**
 * Klasa opisująca pojedynczą stację.
 * 
 * Unikalny identyfikator - UID
 * 
 * @author peku33
 *
 */
public class VeturiloStop implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private int uid;
	private GeoPoint geoPoint;
	private String name;

	public VeturiloStop(int uid, GeoPoint geoPoint, String name) {
		this.uid = uid;
		this.geoPoint = geoPoint;
		this.name = name;
	}

	public int getUid() {
		return uid;
	}
	public GeoPoint getGeoPoint() {
		return geoPoint;
	}
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return String.format("#%d: %s (%s)", uid, name, geoPoint);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		VeturiloStop other = (VeturiloStop) obj;
		
		return uid == other.uid;
	}
	
	@Override
	public int hashCode() {
		return uid;
	}
}
