package net.peku33.freeturilo;

import com.graphhopper.PathWrapper;

public class FreeTuriloVeturiloStopPath {
	
	private VeturiloStop from;
	private VeturiloStop to;
	
	private PathWrapper pathWrapper;

	public FreeTuriloVeturiloStopPath(VeturiloStop from, VeturiloStop to, PathWrapper pathWrapper) {
		this.from = from;
		this.to = to;
		
		this.pathWrapper = pathWrapper;
	}

	public VeturiloStop getFrom() {
		return from;
	}
	public VeturiloStop getTo() {
		return to;
	}

	public PathWrapper getPathWrapper() {
		return pathWrapper;
	}

}
