package net.peku33.freeturilo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Klasa zarządzająca pobieraniem danych OSM dla Warszawy
 * @author peku33
 *
 */
public class WarsawMapDownloader {
	
	private static Logger logger = Logger.getLogger(WarsawMapDownloader.class.toString());
	
	// Ścieżka do pobrania pliku .osm.gz
	private static final String REMOTE_GZ_PATH = "https://download.bbbike.org/osm/bbbike/Warsaw/Warsaw.osm.gz";
	
	// Ścieżka lokalnego pliku .osm
	private static final String LOCAL_PATH = "download/Warsaw.osm";
	
	/**
	 * Zwraca instancję File pliku lokalnego
	 * Plik może nie istnieć przed wywołaniem checkUpdate()
	 */
	static File localInstance = null;
	public static File getLocal() {
		if(localInstance == null)
			localInstance = new File(LOCAL_PATH);
		return localInstance;
	}
	
	/**
	 * Zwraca datę modyfikacji pliku lokalnego
	 * @return
	 */
	private static Date getLocalLastModified() {
		if(!getLocal().exists())
			return null;
		
		return new Date(
			getLocal().lastModified()
		);
	}
	
	/**
	 * Zwraca, bez pobierania całości, datę modyfikacji pliku zdalnego
	 * 
	 * @return
	 * @throws Exception
	 */
	private static Date getRemoteLastModified() throws Exception {
		HttpHead request = new HttpHead(REMOTE_GZ_PATH);
		CloseableHttpClient client = HttpClients.createDefault();
		CloseableHttpResponse response = client.execute(request);
		
		Header lastModifiedHeader = response.getFirstHeader("Last-Modified");
		if(lastModifiedHeader == null)
			throw new Exception("Last-Modified header not found");
		
		Date lastModifiedDate = DateUtils.parseDate(lastModifiedHeader.getValue());
		
		response.close();
		client.close();
		
		return lastModifiedDate;
	}
	
	/**
	 * Wykonuje aktualizację - pobiera plik zdalny, rozpakowuje go i zapisuje
	 * @throws Exception
	 */
	private static void update() throws Exception {
		
		// Pobierz plik
		HttpGet request = new HttpGet(REMOTE_GZ_PATH);
		CloseableHttpClient client = HttpClients.createDefault();
		CloseableHttpResponse response = client.execute(request);
		
		// Data ostatniej modyfikacji
		Header lastModifiedHeader = response.getFirstHeader("Last-Modified");
		if(lastModifiedHeader == null)
			throw new Exception("Last-Modified header not found");
		
		Date lastModifiedDate = DateUtils.parseDate(lastModifiedHeader.getValue());
		
		// Dane
		byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
		
		// Zamknięcie połączenia
		client.close();
		response.close();
		
		// Utworzenie katalogu dla zdekodowanego pliku
		FileUtils.forceMkdir(getLocal().getParentFile());
		
		// Dekodowanie gzip
		GZIPInputStream gzipInputStream = new GZIPInputStream(
			new ByteArrayInputStream(responseBytes)
		);
		
		FileOutputStream fileOutputStream = new FileOutputStream(
			getLocal()
		);
		
		IOUtils.copy(gzipInputStream, fileOutputStream);
		
		fileOutputStream.close();
		gzipInputStream.close();
		
		// Zmiana daty ostatniej modyfikacji
		getLocal().setLastModified(
			lastModifiedDate.getTime()
		);
	}
	
	/**
	 * Sprawdza, czy plik lokalny jest w najnowszej wersji
	 * Jeśli nie istnieje - zostaje usunięty
	 * @throws Exception
	 */
	public static void checkUpdate() throws Exception {
		
		Date localLastModified = getLocalLastModified();
		logger.info("localLastModified: " + localLastModified);
		
		Date remoteLastModified = getRemoteLastModified();
		logger.info("remoteLastModified: " + remoteLastModified);
		
		if(localLastModified != null && remoteLastModified != null && !(localLastModified.before(remoteLastModified))) {
			logger.info("local in latest version, skipping download");
			return;
		}
		
		logger.info("local requires update");
		
		logger.info("starting download");
		update();
		logger.info("finished download");
	}
	
	private WarsawMapDownloader() {}
}
