package net.peku33.freeturilo;

import java.util.Locale;
import java.util.logging.Logger;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.shapes.GHPoint;

public class FreeTuriloGraphHooper {
	
	private static Logger logger = Logger.getLogger(WarsawMapDownloader.class.toString());
	
	private static final String GRAPHHOOPER_PATH = "work/" + WarsawMapDownloader.class.getName();
	private static final Locale locale = Locale.getDefault();
	
	private static FreeTuriloGraphHooper instance;
	public static FreeTuriloGraphHooper getInstance() throws Exception {
		if(instance == null)
			instance = new FreeTuriloGraphHooper();
		return instance;
	}
	
	private GraphHopper gh;
	private FreeTuriloGraphHooper() throws Exception {
		
		boolean mapUpdated = WarsawMapDownloader.checkUpdate();
		
		gh = new GraphHopperOSM();
		gh.forDesktop();
		gh.setDataReaderFile(WarsawMapDownloader.getLocal().getPath());
		gh.setGraphHopperLocation(GRAPHHOOPER_PATH);
		gh.setEncodingManager(new EncodingManager("bike"));
		
		if(mapUpdated) {
			logger.info("mapUpdated, cleaning GraphHopperOSM storage");
			gh.clean();
		}
		
		logger.info("importOrLoad start");
		gh.importOrLoad();
		logger.info("importOrLoad end");
	}
	
	public PathWrapper route(VeturiloStop from, VeturiloStop to) {
		
		// Punkty startowy i końcowy
		GHPoint fromPoint = new GHPoint(from.getLatitude(), from.getLongitude());
		GHPoint toPoint = new GHPoint(to.getLatitude(), to.getLongitude());
		
		// Zadanie routingu
		GHRequest ghRequest = new GHRequest(fromPoint, toPoint)
			.setLocale(locale)
			.setVehicle("bike")
		;
		
		// Wynik routingu
		GHResponse ghResponse = gh.route(ghRequest);
		
		// Jeśli ścieżka zawiera błędy - zignoruj
		if(ghResponse.hasErrors()) {
			
			logger.info(from + " -> " + to);
			for(Throwable throwable : ghResponse.getErrors())
				logger.info(throwable.getMessage());
			
			return null;
		}
		
		return ghResponse.getBest();
	}
}
