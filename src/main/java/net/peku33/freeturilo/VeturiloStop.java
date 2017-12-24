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
	public boolean equals(Object object) {
		if(object == null)
			return false;
		if(object == this)
			return true;
		if(!(object instanceof VeturiloStop))
			return false;
		
		return uid == ((VeturiloStop) object).uid;
	}
	
	@Override
	public int hashCode() {
		return uid;
	}
}
