package net.peku33.freeturilo.core;

import java.util.LinkedList;
import java.util.Locale;
import java.util.logging.Logger;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.shapes.GHPoint;

/**
 * Klasa będąca wrapperem na GraphHooper, dostarczająca metody wyszukiwania ścieżki rowerowej i pieszej między punktami.
 * 
 * @author peku33
 *
 */
public class FreeTuriloGraphHooper {
	
	private static Logger logger = Logger.getLogger(WarsawMapDownloader.class.toString());
	
	// Katalog roboczy GrapHooper
	private static final String workDir = "work/" + FreeTuriloGraphHooper.class.getName();
	
	// Język komunikatów
	private static final Locale locale = Locale.getDefault();
	
	/**
	 * Wątkowo bezpieczny singleton
	 */
	private static FreeTuriloGraphHooper instance;
	private static Object getInstanceTSLock = new Object();
	public static FreeTuriloGraphHooper getInstanceTS() throws Exception {
		synchronized (getInstanceTSLock) {
			if(instance == null)
				instance = new FreeTuriloGraphHooper();
		}
		return instance;
	}
	

	// Uchwyt na instancję GraphHopper
	private GraphHopper graphHooper;
	
	/**
	 * Konstruktor. Inicjuje silnik map
	 * @throws Exception
	 */
	private FreeTuriloGraphHooper() throws Exception {
		
		// W przypadku pomyślnej aktualizacji mapy - przebudowujemy cache
		boolean mapUpdated = WarsawMapDownloader.checkUpdate();
		
		graphHooper = new GraphHopperOSM();
		graphHooper.forDesktop();
		graphHooper.setGraphHopperLocation(workDir);
		graphHooper.setDataReaderFile(WarsawMapDownloader.getLocal().getPath());
		graphHooper.setEncodingManager(new EncodingManager("bike,foot"));
		
		if(mapUpdated) {
			logger.info("mapUpdated, cleaning GraphHopperOSM storage");
			graphHooper.clean();
			FreeTuriloRouter.clearVeturiloStopsGraphCache();
		}
		
		logger.info("importOrLoad start");
		graphHooper.importOrLoad();
		logger.info("importOrLoad end");
	}
	
	/**
	 * Wyznacza trasę dla roweru
	 * @param fromPoint
	 * @param toPoint
	 * @return
	 */
	public Path routeBike(GeoPoint from, GeoPoint to) {
		return route(from, to, "bike");
	}
	
	/**
	 * Wyznacza trasę pieszą
	 * @param fromPoint
	 * @param toPoint
	 * @return
	 */
	public Path routeFoot(GeoPoint from, GeoPoint to) {
		return route(from, to, "foot");
	}
	
	private Path route(GeoPoint from, GeoPoint to, String vehicle) {
		// Zadanie routingu
		GHRequest ghRequest = new GHRequest(
				new GHPoint(from.getLatitude(), from.getLongitude()),
				new GHPoint(to.getLatitude(), to.getLongitude())
			)
			.setLocale(locale)
			.setVehicle(vehicle)
		;
		
		// Wynik routingu
		GHResponse ghResponse = graphHooper.route(ghRequest);
		
		// Jeśli ścieżka zawiera błędy - zignoruj
		if(ghResponse.hasErrors())			
			return null;
		
		// Zwróć najlepszą ścieżkę
		PathWrapper pathWrapper = ghResponse.getBest();
		
		LinkedList<GeoPoint> geoPoints = new LinkedList<>();
		for(GHPoint ghPoint : pathWrapper.getPoints()) {
			geoPoints.add(new GeoPoint(ghPoint.getLat(), ghPoint.getLon()));
		}
		
		long timeMillis = pathWrapper.getTime();
		
		return new Path(geoPoints, timeMillis);
	}
}
