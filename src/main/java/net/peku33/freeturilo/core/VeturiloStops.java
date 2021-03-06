package net.peku33.freeturilo.core;

import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Klasa odpowiedzialna za pobieranie listy stacji Veturilo
 * Działa na zasadzie singletonu, pierwsze uruchomienie pobiera listę stacji, którą można odczytać metodą getVeturiloStopCollection()
 * @author peku33
 *
 */
public class VeturiloStops {
	
	private static Logger logger = Logger.getLogger(WarsawMapDownloader.class.toString());
	
	/**
	 * Dostęp przez singleton
	 */
	private static VeturiloStops instance;
	private static Object getInstanceTSLock = new Object();
	public static VeturiloStops getInstanceTS() throws Exception {
		synchronized (getInstanceTSLock) {
			if(instance == null)
				instance = new VeturiloStops();
		}
		return instance;
	}
	
	/**
	 * Pobiera i zwraca goły nieprzetworzony HTML strony mapy stacji
	 * @return
	 * @throws Exception
	 */
	private static String downloadMapPage() throws Exception {
		HttpGet request = new HttpGet("https://www.veturilo.waw.pl/mapa-stacji/");
		CloseableHttpClient client = HttpClients.createDefault();
		CloseableHttpResponse response = client.execute(request);
		
		String responseString = EntityUtils.toString(response.getEntity());
		
		response.close();
		client.close();
		
		return responseString;
	}
	
	/**
	 * Z gołego i nieprzetworzonego HTML'a mapy stacji wyciąga obiekt JSON listy stacji
	 * @param mapPage
	 * @return
	 * @throws Exception
	 */
	private static JsonNode mapPageToJsonNode(String mapPage) throws Exception {
		
		// Wytnij odpowiedni fragment z całej strony
		String mapPageJsonStart = "var NEXTBIKE_PLACES_DB = '";
		String mapPageJsonEnd = "';\n            </script>";
		
		int mapPageJsonStartPosition = mapPage.indexOf(mapPageJsonStart);
		if(mapPageJsonStartPosition == -1)
			throw new Exception("mapPageJsonStart not found");
		
		int mapPageJsonEndPosition = mapPage.indexOf(mapPageJsonEnd, mapPageJsonStartPosition + mapPageJsonStart.length());
		if(mapPageJsonStartPosition == -1)
			throw new Exception("mapPageJsonEnd not found");
		
		String mapPageJson = mapPage.substring(mapPageJsonStartPosition + mapPageJsonStart.length(), mapPageJsonEndPosition);
		
		// Usuń znaki '
		mapPageJson = StringEscapeUtils.unescapeEcmaScript(mapPageJson);
		
		// Sparsuj jako json
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode node = objectMapper.readTree(mapPageJson);
		
		return node;
	}
	
	/**
	 * Z obiektu JSON listy stacji tworzy kolekcję stacji
	 * 
	 * @param jsonNode
	 * @return
	 * @throws Exception
	 */
	private static LinkedList<VeturiloStop> jsonNodeToVeturiloStopLinkedList(JsonNode jsonNode) throws Exception {
		
		LinkedList<VeturiloStop> veturiloStops = new LinkedList<>();
		
		// Root jest listą podregionów
		JsonNode subregionsArray = jsonNode;
		
		if(!subregionsArray.isArray())
			throw new Exception("!subregionsArray.isArray()");
		
		for(int subregionId = 0; subregionId < subregionsArray.size(); ++subregionId) {
			JsonNode subregionObject = subregionsArray.get(subregionId);
			if(!subregionObject.isObject())
				throw new Exception("!subregionObject.isObject()");
			
			JsonNode subregionPlacesArray = subregionObject.get("places");
			if(subregionPlacesArray == null || !subregionPlacesArray.isArray())
				throw new Exception("subregionPlacesArray == null || !subregionPlacesArray.isArray()");
			
			for(int placeId = 0; placeId < subregionPlacesArray.size(); ++placeId) {
				
				JsonNode placeObject = subregionPlacesArray.get(placeId);
				if(!placeObject.isObject())
					throw new Exception("!placeObject.isObject()");
				
				JsonNode placeUidNode = placeObject.get("uid");
				if(placeUidNode == null || !placeUidNode.isTextual())
					throw new Exception("placeUidNode == null || !placeUidNode.isTextual()");
				int placeUid = Integer.parseInt(placeUidNode.asText());
				
				JsonNode placeLatitudeNode = placeObject.get("lat");
				if(placeLatitudeNode == null || !placeLatitudeNode.isTextual())
					throw new Exception("placeLatitudeNode == null || !placeLatitudeNode.isTextual()");
				double placeLatitude = Double.parseDouble(placeLatitudeNode.asText());
				
				JsonNode placeLongitudeNode = placeObject.get("lng");
				if(placeLongitudeNode == null || !placeLongitudeNode.isTextual())
					throw new Exception("placeLongitudeNode == null || !placeLongitudeNode.isTextual()");
				double placeLongitude = Double.parseDouble(placeLongitudeNode.asText());
				
				JsonNode placeNameNode = placeObject.get("name");
				if(placeNameNode == null || !placeNameNode.isTextual())
					throw new Exception("placeNameNode == null || !placeNameNode.isTextual()");
				String placeName = placeNameNode.asText();
				
				VeturiloStop veturiloStop = new VeturiloStop(
					placeUid,
					new GeoPoint(placeLatitude, placeLongitude),
					placeName
				);
				
				veturiloStops.add(veturiloStop);
			}
		}
		
		return veturiloStops;
	}
	
	/**
	 * Lokalna kolekcja listy stacji
	 */
	private LinkedList<VeturiloStop> veturiloStopLinkedList;
	
	/**
	 * Konstruktor, tworzy listę stacji
	 * @throws Exception
	 */
	private VeturiloStops() throws Exception {
		
		logger.info("downloadMapPage start");
		String mapPage = downloadMapPage();
		logger.info("downloadMapPage end");
		
		logger.info("mapPageToJsonNode start");
		JsonNode jsonNode = mapPageToJsonNode(mapPage);
		logger.info("mapPageToJsonNode end");
		
		logger.info("jsonNodeToVeturiloStopCollection start");
		veturiloStopLinkedList = jsonNodeToVeturiloStopLinkedList(jsonNode);
		logger.info("jsonNodeToVeturiloStopCollection end");
	}
	
	/**
	 * Gettery. Kolekcje są niezmienne, więc raczej ThreadSafe
	 */
	public Iterable<VeturiloStop> getVeturiloStopIterable() {
		return veturiloStopLinkedList;
	}
	public int getVeturiloStopSize() {
		return veturiloStopLinkedList.size();
	}
}
