package net.peku33.freeturilo;

/**
 * Klasa opisująca pojedynczą stację.
 * 
 * Unikalny identyfikator - UID
 * 
 * @author peku33
 *
 */
public class VeturiloStop {
	
	private int uid;
	
	private double latitude;
	private double longitude;
	
	private String name;

	public VeturiloStop(int uid, double latitude, double longitude, String name) {
		this.uid = uid;
		this.latitude = latitude;
		this.longitude = longitude;
		this.name = name;
	}

	public int getUid() {
		return uid;
	}

	public double getLatitude() {
		return latitude;
	}
	public double getLongitude() {
		return longitude;
	}

	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return String.format("#%d: %s (%f / %f)", uid, name, latitude, longitude);
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
