package net.peku33.freeturilo.core;

public class VeturiloStopPair {
	
	private VeturiloStop from;
	private VeturiloStop to;
	
	public VeturiloStopPair(VeturiloStop from, VeturiloStop to) {
		this.from = from;
		this.to = to;
	}
	
	public VeturiloStop getFrom() {
		return from;
	}
	public VeturiloStop getTo() {
		return to;
	}
	
	public double calculateAbsoluteDistance() {
		return calculateAbsoluteDistance(from, to);
	}
	public static double calculateAbsoluteDistance(VeturiloStop from, VeturiloStop to) {
		return Math.sqrt(
			Math.pow(from.getLatitude() - to.getLatitude(), 2)
			+
			Math.pow(from.getLongitude() - to.getLongitude(), 2)
		);
	}
	
	@Override
	public String toString() {
		return from + " -> " + to;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + from.hashCode();
		result = prime * result + to.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		VeturiloStopPair other = (VeturiloStopPair) obj;
		
		return from.equals(other.from) && to.equals(other.to);
	}
}
